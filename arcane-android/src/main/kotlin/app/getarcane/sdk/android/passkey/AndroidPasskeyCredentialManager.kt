package app.getarcane.sdk.android.passkey

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.NotSupportedError
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import app.getarcane.sdk.models.auth.PasskeyChallenge
import app.getarcane.sdk.models.auth.PasskeyCredential
import app.getarcane.sdk.models.auth.PasskeyCredentialDecodingException
import app.getarcane.sdk.models.auth.MFAChallenge
import kotlinx.serialization.json.JsonObject

/**
 * Android Credential Manager ceremony adapter for Arcane's typed WebAuthn challenges.
 *
 * The application never receives or reconstructs individual WebAuthn fields: server options are
 * passed to the platform as one opaque JSON document and the provider response is returned to the
 * core SDK as an opaque credential. Neither value is included in exceptions or diagnostics here.
 */
@Deprecated(
    message = "Current Arcane servers require the same-origin browser bridge; use AndroidPasskeyBrowserBridge.",
)
public class AndroidPasskeyCredentialManager private constructor(
    context: Context,
    private val credentialManager: CredentialManager,
) {
    public constructor(context: Context) : this(context, CredentialManager.create(context))

    // Credential Manager may need an Activity context to present provider UI; retain exactly the
    // caller-provided context for this short-lived adapter.
    private val context: Context = context

    /** Performs a passkey creation ceremony for registration/enrollment. */
    public suspend fun create(challenge: PasskeyChallenge): PasskeyCredential {
        val response = try {
            credentialManager.createCredential(
                context = context,
                request = CreatePublicKeyCredentialRequest(credentialManagerRequestJson(challenge.options)),
            )
        } catch (_: CreateCredentialCancellationException) {
            throw PasskeyCeremonyCancelledException()
        } catch (exception: CreateCredentialException) {
            throw mapCreateCredentialException(exception)
        }
        val publicKey = response as? CreatePublicKeyCredentialResponse
            ?: throw PasskeyProviderResponseException()
        return decodeProviderResponse(publicKey.registrationResponseJson)
    }

    /** Performs a passkey assertion ceremony for login, MFA, or step-up. */
    public suspend fun get(challenge: PasskeyChallenge): PasskeyCredential {
        return get(credentialManagerRequestJson(challenge.options))
    }

    /** Performs the assertion embedded directly in a pending primary-login MFA challenge. */
    public suspend fun get(challenge: MFAChallenge): PasskeyCredential {
        return get(credentialManagerRequestJson(challenge.options))
    }

    private suspend fun get(requestJson: String): PasskeyCredential {
        val response = try {
            credentialManager.getCredential(
                context = context,
                request = GetCredentialRequest(
                    credentialOptions = listOf(GetPublicKeyCredentialOption(requestJson)),
                ),
            )
        } catch (_: GetCredentialCancellationException) {
            throw PasskeyCeremonyCancelledException()
        } catch (exception: GetCredentialException) {
            throw mapGetCredentialException(exception)
        }
        val publicKey = response.credential as? PublicKeyCredential
            ?: throw PasskeyProviderResponseException()
        return decodeProviderResponse(publicKey.authenticationResponseJson)
    }
}

/** Arcane exposes go-webauthn's inner `.Response`, already shaped for a platform ceremony. */
internal fun credentialManagerRequestJson(options: JsonObject): String = options.toString()

private fun decodeProviderResponse(responseJson: String): PasskeyCredential = try {
    PasskeyCredential.fromProviderResponse(responseJson)
} catch (_: PasskeyCredentialDecodingException) {
    throw PasskeyProviderResponseException()
}

internal fun mapCreateCredentialException(exception: CreateCredentialException): PasskeyProviderException =
    when (exception) {
        is CreateCredentialProviderConfigurationException -> PasskeyProviderConfigurationException()
        is CreateCredentialUnsupportedException,
        is CreateCredentialNoCreateOptionException,
        -> PasskeyProviderUnsupportedException()
        is CreateCredentialInterruptedException -> PasskeyProviderInterruptedException()
        is CreatePublicKeyCredentialDomException -> when (exception.domError) {
            is SecurityError -> PasskeyProviderConfigurationException()
            is NotSupportedError -> PasskeyProviderUnsupportedException()
            else -> PasskeyProviderFailureException()
        }
        else -> PasskeyProviderFailureException()
    }

internal fun mapGetCredentialException(exception: GetCredentialException): PasskeyProviderException =
    when (exception) {
        is GetCredentialProviderConfigurationException -> PasskeyProviderConfigurationException()
        is GetCredentialUnsupportedException -> PasskeyProviderUnsupportedException()
        is NoCredentialException -> PasskeyNotFoundException()
        is GetCredentialInterruptedException -> PasskeyProviderInterruptedException()
        is GetPublicKeyCredentialDomException -> when (exception.domError) {
            is SecurityError -> PasskeyProviderConfigurationException()
            is NotSupportedError -> PasskeyProviderUnsupportedException()
            else -> PasskeyProviderFailureException()
        }
        else -> PasskeyProviderFailureException()
    }

/** Deliberately carries no provider payload, challenge, or credential material. */
public class PasskeyProviderResponseException : IllegalStateException(
    "The credential provider returned an unsupported response.",
)

/** Stable provider failures deliberately carry no provider payload or credential material. */
public sealed class PasskeyProviderException(message: String) : IllegalStateException(message)

public class PasskeyProviderConfigurationException : PasskeyProviderException(
    "Passkeys are not configured for this Android app and server.",
)

public class PasskeyProviderUnsupportedException : PasskeyProviderException(
    "Passkeys are not supported on this device.",
)

public class PasskeyNotFoundException : PasskeyProviderException(
    "No matching passkey is available.",
)

public class PasskeyProviderInterruptedException : PasskeyProviderException(
    "The passkey provider was interrupted. Try again.",
)

public class PasskeyProviderFailureException : PasskeyProviderException(
    "The passkey provider could not complete the request.",
)

/** User cancellation is a normal terminal transition and carries no provider payload. */
public class PasskeyCeremonyCancelledException : IllegalStateException(
    "Passkey request cancelled.",
)
