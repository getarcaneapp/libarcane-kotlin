package app.getarcane.sdk.android.passkey

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import app.getarcane.sdk.ArcaneClient
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.models.auth.AuthenticationResult
import app.getarcane.sdk.models.auth.MFAChallenge
import app.getarcane.sdk.models.auth.PasskeySummary
import app.getarcane.sdk.models.auth.StepUpGrant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Drives Arcane's same-origin mobile passkey bridge in an Android Custom Tab.
 *
 * The application must register an intent filter for
 * `arcane-mobile://passkey-callback`, retain this instance while a ceremony is active, and pass the
 * callback to [complete]. Only one ceremony may be active at a time. [cancel] invalidates an active
 * callback when the owning lifecycle stops; Custom Tabs do not provide an API for closing a tab.
 *
 * Challenges, credentials, callback state, and login verifiers remain opaque and are never included
 * in diagnostics. A process restart intentionally loses the in-memory session and rejects its callback.
 */
public class AndroidPasskeyBrowserBridge internal constructor(
    private val client: ArcaneClient,
    private val manifestSource: PasskeyBridgeManifestSource,
    private val randomBytes: (Int) -> ByteArray,
    private val launchDispatcher: CoroutineDispatcher,
) {
    public constructor(client: ArcaneClient) : this(
        client = client,
        manifestSource = SdkPasskeyBridgeManifestSource(client),
        randomBytes = { count -> java.security.SecureRandom().run { ByteArray(count).also(::nextBytes) } },
        launchDispatcher = Dispatchers.Main.immediate,
    )

    private val stateLock: Any = Any()

    @Volatile
    private var session: Session? = null

    /** True while a ceremony is being prepared or waiting for its callback. */
    public val hasActiveSession: Boolean
        get() = session != null

    /** Checks only the strict bridge manifest and supported server origin. */
    public suspend fun isBridgeAvailable(): Boolean = try {
        loadManifest()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    /** Begins passwordless sign-in. Completion uses the verifier-bound mobile exchange endpoint. */
    public suspend fun startLogin(context: Context) {
        startLogin(CustomTabPasskeyBridgeLaunchTarget(context))
    }

    internal suspend fun startLogin(launchTarget: PasskeyBridgeLaunchTarget) {
        start(launchTarget) {
            val challenge = client.passkeys.beginLogin()
            val verifier = makePasskeyBridgeState(randomBytes)
            val active = LoginSession(
                state = it,
                codeVerifier = verifier,
            )
            active to PasskeyBridgeRequest(
                state = it,
                operation = "authenticate",
                options = challenge.options,
                mobileLogin = PasskeyBridgeMobileLogin(
                    ceremonyId = challenge.ceremonyId,
                    codeChallenge = makePasskeyBridgeCodeChallenge(verifier),
                ),
            )
        }
    }

    /** Begins the passkey assertion for a pending primary-login MFA transaction. */
    public suspend fun startMfa(context: Context, transactionId: String) {
        startMfa(CustomTabPasskeyBridgeLaunchTarget(context), transactionId)
    }

    internal suspend fun startMfa(launchTarget: PasskeyBridgeLaunchTarget, transactionId: String) {
        start(launchTarget) {
            val challenge = client.passkeys.beginMfa(transactionId)
            MfaSession(it, challenge.transactionId) to PasskeyBridgeRequest(
                state = it,
                operation = "authenticate",
                options = challenge.options,
            )
        }
    }

    /** Begins the assertion from an already-loaded typed MFA challenge. */
    public suspend fun startMfa(context: Context, challenge: MFAChallenge) {
        startMfa(CustomTabPasskeyBridgeLaunchTarget(context), challenge)
    }

    internal suspend fun startMfa(launchTarget: PasskeyBridgeLaunchTarget, challenge: MFAChallenge) {
        start(launchTarget) {
            MfaSession(it, challenge.transactionId) to PasskeyBridgeRequest(
                state = it,
                operation = "authenticate",
                options = challenge.options,
            )
        }
    }

    /** Begins passkey enrollment. The typed SDK performs the protected finish request. */
    public suspend fun startRegistration(
        context: Context,
        name: String? = null,
        stepUpToken: String? = null,
    ) {
        startRegistration(CustomTabPasskeyBridgeLaunchTarget(context), name, stepUpToken)
    }

    internal suspend fun startRegistration(
        launchTarget: PasskeyBridgeLaunchTarget,
        name: String? = null,
        stepUpToken: String? = null,
    ) {
        start(launchTarget) {
            val challenge = client.passkeys.beginRegistration(stepUpToken)
            RegistrationSession(it, challenge.ceremonyId, name, stepUpToken) to PasskeyBridgeRequest(
                state = it,
                operation = "register",
                options = challenge.options,
            )
        }
    }

    /** Begins passkey step-up for protected account-security operations. */
    public suspend fun startStepUp(context: Context) {
        startStepUp(CustomTabPasskeyBridgeLaunchTarget(context))
    }

    internal suspend fun startStepUp(launchTarget: PasskeyBridgeLaunchTarget) {
        start(launchTarget) {
            val challenge = client.passkeys.beginStepUp()
            val transactionId = challenge.transactionId ?: throw PasskeyBridgeInvalidChallengeException()
            StepUpSession(it, transactionId) to PasskeyBridgeRequest(
                state = it,
                operation = "authenticate",
                options = challenge.options,
            )
        }
    }

    /**
     * Strictly validates and consumes the active callback, then invokes its typed SDK finish call.
     * Invalid callbacks do not consume the session; valid success/error callbacks are single-use.
     */
    public suspend fun complete(callbackUri: Uri): PasskeyBrowserBridgeResult {
        return complete(callbackUri.toString())
    }

    internal suspend fun complete(callbackUrl: String): PasskeyBrowserBridgeResult {
        val active = synchronized(stateLock) {
            session as? ActiveSession ?: throw PasskeyBridgeNoActiveSessionException()
        }
        val callback = parsePasskeyBridgeCallback(
            callbackUrl = callbackUrl,
            expectedState = active.state,
            expectsTransaction = active is LoginSession,
            json = client.configuration.json,
        )
        val completing = CompletingSession()
        synchronized(stateLock) {
            if (session !== active) throw PasskeyBridgeNoActiveSessionException()
            session = completing
        }
        try {
            if (callback is ParsedPasskeyBridgeCallback.Error) {
                if (callback.code == PasskeyBrowserBridgeErrorCode.CANCELLED) {
                    throw PasskeyBrowserBridgeCancelledException()
                }
                throw PasskeyBrowserBridgeFailureException(callback.code)
            }
            return when (active) {
                is LoginSession -> {
                    val transaction = callback as? ParsedPasskeyBridgeCallback.Transaction
                        ?: throw PasskeyBridgeInvalidCallbackException()
                    PasskeyBrowserBridgeResult.Login(
                        client.passkeys.exchangeMobileLogin(transaction.id, active.codeVerifier),
                    )
                }
                is MfaSession -> {
                    val credential = callback.requireCredential()
                    PasskeyBrowserBridgeResult.Mfa(
                        client.passkeys.finishMfa(active.transactionId, credential),
                    )
                }
                is RegistrationSession -> {
                    val credential = callback.requireCredential()
                    PasskeyBrowserBridgeResult.Registration(
                        client.passkeys.finishRegistration(
                            ceremonyId = active.ceremonyId,
                            credential = credential,
                            name = active.name,
                            stepUpToken = active.stepUpToken,
                        ),
                    )
                }
                is StepUpSession -> {
                    val credential = callback.requireCredential()
                    PasskeyBrowserBridgeResult.StepUp(
                        client.passkeys.finishStepUp(active.transactionId, credential),
                    )
                }
            }
        } finally {
            synchronized(stateLock) {
                if (session === completing) session = null
            }
        }
    }

    /** Invalidates the current preparation or callback without retaining sensitive session values. */
    public fun cancel() {
        synchronized(stateLock) { session = null }
    }

    private suspend fun start(
        launchTarget: PasskeyBridgeLaunchTarget,
        prepare: suspend (state: String) -> Pair<ActiveSession, PasskeyBridgeRequest>,
    ) {
        val reservation = PreparingSession()
        synchronized(stateLock) {
            if (session != null) throw PasskeyBridgeSessionActiveException()
            session = reservation
        }
        var ownedSession: Session = reservation
        try {
            val requestState = makePasskeyBridgeState(randomBytes)
            val (active, request) = prepare(requestState)
            val manifest = loadManifest()
            val url = buildPasskeyBridgeAuthorizationUrl(
                origin = passkeyServerOrigin(client.configuration.baseUrl),
                manifest = manifest,
                request = request,
                json = client.configuration.json,
            )
            synchronized(stateLock) {
                if (session !== reservation) throw PasskeyBrowserBridgeCancelledException()
                session = active
                ownedSession = active
            }
            try {
                withContext(launchDispatcher) {
                    synchronized(stateLock) {
                        if (session !== active) throw PasskeyBrowserBridgeCancelledException()
                    }
                    launchTarget.launch(url)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: PasskeyBrowserBridgeException) {
                throw failure
            } catch (_: Exception) {
                throw PasskeyBridgeLaunchException()
            }
        } catch (failure: Throwable) {
            synchronized(stateLock) {
                if (session === ownedSession) session = null
            }
            throw failure
        }
    }

    private suspend fun loadManifest(): PasskeyBridgeManifest {
        passkeyServerOrigin(client.configuration.baseUrl)
        val manifest = try {
            manifestSource.load()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: PasskeyBrowserBridgeException) {
            throw failure
        } catch (_: ArcaneError.Decoding) {
            throw PasskeyBridgeInvalidManifestException()
        } catch (_: Exception) {
            throw PasskeyBridgeUnavailableException()
        }
        return parseAndValidatePasskeyBridgeManifest(manifest)
    }

    private sealed interface Session
    private class PreparingSession : Session
    private class CompletingSession : Session
    private sealed class ActiveSession(open val state: String) : Session
    private data class LoginSession(
        override val state: String,
        val codeVerifier: String,
    ) : ActiveSession(state) {
        override fun toString(): String = "LoginSession([REDACTED])"
    }

    private data class MfaSession(
        override val state: String,
        val transactionId: String,
    ) : ActiveSession(state) {
        override fun toString(): String = "MfaSession([REDACTED])"
    }

    private data class RegistrationSession(
        override val state: String,
        val ceremonyId: String,
        val name: String?,
        val stepUpToken: String?,
    ) : ActiveSession(state) {
        override fun toString(): String = "RegistrationSession([REDACTED])"
    }

    private data class StepUpSession(
        override val state: String,
        val transactionId: String,
    ) : ActiveSession(state) {
        override fun toString(): String = "StepUpSession([REDACTED])"
    }
}

private fun ParsedPasskeyBridgeCallback.requireCredential() =
    (this as? ParsedPasskeyBridgeCallback.Credential)?.value
        ?: throw PasskeyBridgeInvalidCallbackException()

/** Typed terminal result from [AndroidPasskeyBrowserBridge.complete]. */
public sealed interface PasskeyBrowserBridgeResult {
    public data class Login(public val result: AuthenticationResult) : PasskeyBrowserBridgeResult {
        override fun toString(): String = "PasskeyBrowserBridgeResult.Login([REDACTED])"
    }

    public data class Mfa(public val result: AuthenticationResult) : PasskeyBrowserBridgeResult {
        override fun toString(): String = "PasskeyBrowserBridgeResult.Mfa([REDACTED])"
    }

    public data class Registration(public val passkey: PasskeySummary) : PasskeyBrowserBridgeResult
    public data class StepUp(public val grant: StepUpGrant) : PasskeyBrowserBridgeResult {
        override fun toString(): String = "PasskeyBrowserBridgeResult.StepUp([REDACTED])"
    }
}

/** Non-sensitive error codes emitted by Arcane's bridge. */
public enum class PasskeyBrowserBridgeErrorCode(public val wireValue: String) {
    INVALID_REQUEST("invalid_request"),
    OVERSIZED("oversized"),
    UNSUPPORTED("unsupported"),
    CANCELLED("cancelled"),
    FAILED("failed"),
    ;

    internal companion object {
        fun fromWireValue(value: String): PasskeyBrowserBridgeErrorCode? = entries.firstOrNull { it.wireValue == value }
    }
}

/** Stable bridge failures never include challenges, credentials, callback URLs, tokens, or verifiers. */
public sealed class PasskeyBrowserBridgeException(message: String) : IllegalStateException(message)

public class PasskeyBridgeUnsupportedOriginException : PasskeyBrowserBridgeException(
    "Passkeys require HTTPS or a loopback HTTP Arcane server.",
)

public class PasskeyBridgeUnavailableException : PasskeyBrowserBridgeException(
    "This Arcane server does not provide the mobile passkey bridge.",
)

public class PasskeyBridgeInvalidManifestException : PasskeyBrowserBridgeException(
    "The Arcane mobile passkey bridge manifest is invalid.",
)

public class PasskeyBridgeRequestTooLargeException : PasskeyBrowserBridgeException(
    "The passkey request is too large.",
)

public class PasskeyBridgeInvalidCallbackException : PasskeyBrowserBridgeException(
    "The passkey bridge returned an invalid callback.",
)

public class PasskeyBridgeStateMismatchException : PasskeyBrowserBridgeException(
    "The passkey bridge callback did not match this request.",
)

public class PasskeyBridgeDuplicateCallbackItemException : PasskeyBrowserBridgeException(
    "The passkey bridge callback contains duplicate values.",
)

public class PasskeyBridgeInvalidCredentialException : PasskeyBrowserBridgeException(
    "The passkey bridge returned an invalid credential.",
)

public class PasskeyBridgeRandomStateException : PasskeyBrowserBridgeException(
    "A secure passkey request state could not be created.",
)

public class PasskeyBridgeInvalidChallengeException : PasskeyBrowserBridgeException(
    "The Arcane server returned an invalid passkey challenge.",
)

public class PasskeyBridgeNoActiveSessionException : PasskeyBrowserBridgeException(
    "No passkey browser session is active.",
)

public class PasskeyBridgeSessionActiveException : PasskeyBrowserBridgeException(
    "A passkey browser session is already active.",
)

public class PasskeyBridgeLaunchException : PasskeyBrowserBridgeException(
    "The passkey browser session could not be started.",
)

public class PasskeyBrowserBridgeCancelledException : PasskeyBrowserBridgeException(
    "Passkey request cancelled.",
)

public class PasskeyBrowserBridgeFailureException(
    public val code: PasskeyBrowserBridgeErrorCode,
) : PasskeyBrowserBridgeException("The passkey bridge failed with ${code.wireValue}.")

internal fun interface PasskeyBridgeManifestSource {
    suspend fun load(): PasskeyBridgeManifest
}

internal class SdkPasskeyBridgeManifestSource(
    private val client: ArcaneClient,
) : PasskeyBridgeManifestSource {
    override suspend fun load(): PasskeyBridgeManifest = client.passkeys.mobileBridgeManifest()
}

internal fun interface PasskeyBridgeLaunchTarget {
    fun launch(url: URI)
}

private class CustomTabPasskeyBridgeLaunchTarget(
    private val context: Context,
) : PasskeyBridgeLaunchTarget {
    override fun launch(url: URI) {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url.toASCIIString()))
    }
}
