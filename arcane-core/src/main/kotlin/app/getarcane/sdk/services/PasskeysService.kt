package app.getarcane.sdk.services

import app.getarcane.sdk.auth.AuthManager
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.request
import app.getarcane.sdk.models.auth.AuthenticationResult
import app.getarcane.sdk.models.auth.MFAChallenge
import app.getarcane.sdk.models.auth.MFAFinishRequest
import app.getarcane.sdk.models.auth.MFAStartRequest
import app.getarcane.sdk.models.auth.MFAStatus
import app.getarcane.sdk.models.auth.MobilePasskeyCompletion
import app.getarcane.sdk.models.auth.MobilePasskeyBridgeManifest
import app.getarcane.sdk.models.auth.MobilePasskeyExchangeRequest
import app.getarcane.sdk.models.auth.MobilePasskeyFinishRequest
import app.getarcane.sdk.models.auth.PasskeyCapabilities
import app.getarcane.sdk.models.auth.PasskeyChallenge
import app.getarcane.sdk.models.auth.PasskeyCredential
import app.getarcane.sdk.models.auth.PasskeyFinishRequest
import app.getarcane.sdk.models.auth.PasskeyLoginAvailability
import app.getarcane.sdk.models.auth.PasskeySummary
import app.getarcane.sdk.models.auth.PasswordStepUpRequest
import app.getarcane.sdk.models.auth.RecoveryCodeRequest
import app.getarcane.sdk.models.auth.RecoveryCodesResponse
import app.getarcane.sdk.models.auth.RenamePasskeyRequest
import app.getarcane.sdk.models.auth.StepUpGrant
import kotlinx.coroutines.CancellationException

/** Typed self-service passkey, MFA, recovery, and step-up operations. */
public class PasskeysService internal constructor(
    private val rest: RestService,
    private val authManager: AuthManager,
) {
    /** Public, cache-disabled capability check suitable for the signed-out screen. */
    public suspend fun loginAvailability(): PasskeyLoginAvailability =
        rest.transport.request("auth/passkey/login/availability", authorized = false)

    /** Loads the small, root-level browser bridge discovery document through this client's engine. */
    public suspend fun mobileBridgeManifest(): MobilePasskeyBridgeManifest {
        val bytes = rest.transport.rawOriginResourceBytes(
            path = "/arcane-mobile-passkey.json",
            maximumResponseBytes = 4_096,
        )
        return try {
            rest.transport.json.decodeFromString<MobilePasskeyBridgeManifest>(
                bytes.decodeToString(throwOnInvalidSequence = true),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw ArcaneError.Decoding("Passkey bridge manifest could not be decoded.")
        }
    }

    public suspend fun beginLogin(): PasskeyChallenge =
        rest.transport.request("auth/passkey/login/begin", method = "POST", authorized = false)

    public suspend fun finishLogin(
        ceremonyId: String,
        credential: PasskeyCredential,
    ): AuthenticationResult = saveAuthenticationResult(
        rest.transport.request(
            "auth/passkey/login/finish",
            method = "POST",
            body = PasskeyFinishRequest(ceremonyId, credential),
            authorized = false,
        ),
    )

    /** Browser-bridge completion bound to the caller's PKCE-style challenge. */
    public suspend fun finishMobileLogin(
        ceremonyId: String,
        credential: PasskeyCredential,
        codeChallenge: String,
    ): MobilePasskeyCompletion = rest.transport.request(
        "auth/passkey/mobile/finish",
        method = "POST",
        body = MobilePasskeyFinishRequest(ceremonyId, credential, codeChallenge),
        authorized = false,
    )

    /** One-time mobile transaction exchange; tokens are stored only if authentication completes. */
    public suspend fun exchangeMobileLogin(
        transactionId: String,
        codeVerifier: String,
    ): AuthenticationResult = saveAuthenticationResult(
        rest.transport.request(
            "auth/passkey/mobile/exchange",
            method = "POST",
            body = MobilePasskeyExchangeRequest(transactionId, codeVerifier),
            authorized = false,
        ),
    )

    public suspend fun beginMfa(transactionId: String): MFAChallenge = rest.transport.request(
        "auth/mfa/passkey/begin",
        method = "POST",
        body = MFAStartRequest(transactionId),
        authorized = false,
    )

    public suspend fun finishMfa(
        transactionId: String,
        credential: PasskeyCredential,
    ): AuthenticationResult = saveAuthenticationResult(
        rest.transport.request(
            "auth/mfa/passkey/finish",
            method = "POST",
            body = MFAFinishRequest(transactionId, credential),
            authorized = false,
        ),
    )

    public suspend fun finishRecovery(
        transactionId: String,
        code: String,
    ): AuthenticationResult = saveAuthenticationResult(
        rest.transport.request(
            "auth/mfa/recovery",
            method = "POST",
            body = RecoveryCodeRequest(transactionId, code),
            authorized = false,
        ),
    )

    public suspend fun list(): List<PasskeySummary> = rest.get("auth/me/passkeys")

    public suspend fun capabilities(): PasskeyCapabilities = rest.get("auth/me/passkeys/capabilities")

    /** The first enrollment permits an absent step-up token; later enrollment requires one. */
    public suspend fun beginRegistration(stepUpToken: String? = null): PasskeyChallenge = rest.post(
        "auth/me/passkeys/register/begin",
        requestHeaders = stepUpHeaders(stepUpToken),
    )

    public suspend fun finishRegistration(
        ceremonyId: String,
        credential: PasskeyCredential,
        name: String? = null,
        stepUpToken: String? = null,
    ): PasskeySummary = rest.post(
        "auth/me/passkeys/register/finish",
        body = PasskeyFinishRequest(ceremonyId, credential, name?.trim()),
        requestHeaders = stepUpHeaders(stepUpToken),
    )

    public suspend fun rename(
        id: String,
        name: String,
        stepUpToken: String,
    ): PasskeySummary = rest.put(
        "auth/me/passkeys/${validatedId(id)}",
        body = RenamePasskeyRequest(name),
        requestHeaders = stepUpHeaders(stepUpToken),
    )

    public suspend fun delete(id: String, stepUpToken: String) {
        rest.deleteVoid(
            "auth/me/passkeys/${validatedId(id)}",
            requestHeaders = stepUpHeaders(stepUpToken),
        )
    }

    public suspend fun beginStepUp(): PasskeyChallenge =
        rest.post("auth/me/passkeys/reauth/begin")

    public suspend fun finishStepUp(
        transactionId: String,
        credential: PasskeyCredential,
    ): StepUpGrant = rest.post(
        "auth/me/passkeys/reauth/finish",
        body = MFAFinishRequest(transactionId, credential),
    )

    public suspend fun passwordStepUp(password: String): StepUpGrant = rest.post(
        "auth/me/passkeys/reauth/password",
        body = PasswordStepUpRequest(password),
    )

    public suspend fun mfaStatus(): MFAStatus = rest.get("auth/me/mfa")

    public suspend fun enableMfa(stepUpToken: String): RecoveryCodesResponse = rest.post(
        "auth/me/mfa/enable",
        requestHeaders = stepUpHeaders(stepUpToken),
    )

    public suspend fun disableMfa(stepUpToken: String) {
        rest.postVoid(
            "auth/me/mfa/disable",
            requestHeaders = stepUpHeaders(stepUpToken),
        )
    }

    public suspend fun regenerateRecoveryCodes(stepUpToken: String): RecoveryCodesResponse = rest.post(
        "auth/me/mfa/recovery-codes/regenerate",
        requestHeaders = stepUpHeaders(stepUpToken),
    )

    private suspend fun saveAuthenticationResult(result: AuthenticationResult): AuthenticationResult {
        authManager.save(result)
        return result
    }

    private fun stepUpHeaders(token: String?): Map<String, String> =
        if (token.isNullOrEmpty()) emptyMap() else mapOf(STEP_UP_HEADER to token)

    private fun validatedId(id: String): String {
        if (
            id.isBlank() || id != id.trim() || id == "." || id == ".." ||
            id.any { it.isISOControl() || it == '/' || it == '\\' || it == '?' || it == '#' || it == '%' }
        ) {
            throw ArcaneError.Validation(mapOf("id" to listOf("Passkey identifier is invalid.")))
        }
        return id
    }

    private companion object {
        const val STEP_UP_HEADER: String = "X-Step-Up-Token"
    }
}
