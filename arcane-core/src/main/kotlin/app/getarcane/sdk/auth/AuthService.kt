package app.getarcane.sdk.auth

import app.getarcane.sdk.http.ArcaneTransport
import app.getarcane.sdk.http.MultipartFile
import app.getarcane.sdk.http.multipartUpload
import app.getarcane.sdk.http.request
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.auth.AuthenticationResult
import app.getarcane.sdk.models.auth.LoginRequest
import app.getarcane.sdk.models.auth.LoginResponse
import app.getarcane.sdk.models.auth.MFARequiredException
import app.getarcane.sdk.models.auth.OidcAuthUrlRequest
import app.getarcane.sdk.models.auth.OidcAuthUrlResponse
import app.getarcane.sdk.models.auth.OidcCallbackRequest
import app.getarcane.sdk.models.auth.OidcCallbackResponse
import app.getarcane.sdk.models.auth.OidcConfigResponse
import app.getarcane.sdk.models.auth.OidcDeviceAuthRequest
import app.getarcane.sdk.models.auth.OidcDeviceAuthResponse
import app.getarcane.sdk.models.auth.OidcDeviceTokenRequest
import app.getarcane.sdk.models.auth.OidcDeviceTokenResponse
import app.getarcane.sdk.models.auth.OidcStatusInfo
import app.getarcane.sdk.models.auth.PasswordChange
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.models.user.AvatarImageFormat
import app.getarcane.sdk.models.user.UpdateProfile

/**
 * High-level authentication operations. OIDC endpoints return un-enveloped bodies, so they use
 * [requestDecoded]. Successful auth saves tokens via [AuthManager] and records server capabilities
 * from the returned [User].
 */
public class AuthService internal constructor(
    private val transport: ArcaneTransport,
    private val authManager: AuthManager,
) {
    public suspend fun login(username: String, password: String): LoginResponse {
        return when (val result = authenticate(username, password)) {
            is AuthenticationResult.Authenticated -> result.response
            is AuthenticationResult.MfaRequired -> throw MFARequiredException(result.challenge)
        }
    }

    /** Authenticates without collapsing a pending MFA transaction into a decoding failure. */
    public suspend fun authenticate(username: String, password: String): AuthenticationResult {
        val result: AuthenticationResult = transport.request(
            "auth/login",
            method = "POST",
            body = LoginRequest(username, password),
            authorized = false,
        )
        authManager.save(result)
        return result
    }

    public suspend fun logout() {
        var remoteFailure: Throwable? = null
        try {
            transport.request<MessageResponse>("auth/logout", method = "POST")
        } catch (failure: Throwable) {
            remoteFailure = failure
        }
        authManager.clear()
        remoteFailure?.let { throw it }
    }

    public suspend fun me(): User {
        val user: User = transport.request("auth/me")
        authManager.recordCapabilities(user)
        return user
    }

    /** Updates only the current account through Arcane's self-service profile contract. */
    public suspend fun updateProfile(update: UpdateProfile): User {
        val user: User = transport.request("auth/me/profile", method = "PUT", body = update)
        authManager.recordCapabilities(user)
        return user
    }

    /** Uploads or replaces the signed-in user's custom avatar through the self-service route. */
    public suspend fun uploadAvatar(
        content: ByteArray,
        format: AvatarImageFormat,
        filename: String = "avatar.${format.fileExtension}",
    ): User {
        val user = transport.multipartUpload(
            path = "auth/me/avatar",
            deserializer = User.serializer(),
            files = listOf(
                MultipartFile(
                    fieldName = "file",
                    filename = filename,
                    content = content,
                    contentType = format.mediaType,
                ),
            ),
        )
        authManager.recordCapabilities(user)
        return user
    }

    /** Deletes the signed-in user's custom avatar and returns the refreshed account. */
    public suspend fun deleteAvatar(): User {
        val user: User = transport.request("auth/me/avatar", method = "DELETE")
        authManager.recordCapabilities(user)
        return user
    }

    public suspend fun refresh(): TokenPair = authManager.refreshTokens()

    public suspend fun changePassword(currentPassword: String?, newPassword: String) {
        transport.request<MessageResponse>(
            "auth/password",
            method = "POST",
            body = PasswordChange(currentPassword, newPassword),
        )
    }

    // --- OIDC (un-enveloped responses) ---

    public suspend fun oidcStatus(): OidcStatusInfo =
        transport.requestDecoded("oidc/status", authorized = false)

    public suspend fun oidcConfig(): OidcConfigResponse =
        transport.requestDecoded("oidc/config", authorized = false)

    public suspend fun oidcAuthUrl(mobileRedirectUri: String, redirectTo: String = "/"): OidcAuthUrlResponse =
        transport.requestDecoded(
            "oidc/url",
            method = "POST",
            body = OidcAuthUrlRequest(redirectUri = redirectTo, mobileRedirectUri = mobileRedirectUri),
            authorized = false,
        )

    public suspend fun oidcCallback(code: String, state: String, mobileRedirectUri: String): OidcCallbackResponse {
        return when (val result = authenticateOidcCallback(code, state, mobileRedirectUri)) {
            is AuthenticationResult.Authenticated -> OidcCallbackResponse(
                success = true,
                token = result.response.token,
                refreshToken = result.response.refreshToken,
                expiresAt = result.response.expiresAt,
                user = result.response.user,
            )
            is AuthenticationResult.MfaRequired -> throw MFARequiredException(result.challenge)
        }
    }

    /** Completes OIDC while preserving a possible MFA challenge and its no-token state. */
    public suspend fun authenticateOidcCallback(
        code: String,
        state: String,
        mobileRedirectUri: String,
    ): AuthenticationResult {
        val result: AuthenticationResult = transport.requestDecoded(
            "oidc/callback",
            method = "POST",
            body = OidcCallbackRequest(code, state, mobileRedirectUri),
            authorized = false,
        )
        authManager.save(result)
        return result
    }

    public suspend fun oidcDeviceCode(): OidcDeviceAuthResponse =
        transport.requestDecoded(
            "oidc/device/code",
            method = "POST",
            body = OidcDeviceAuthRequest(),
            authorized = false,
        )

    public suspend fun oidcDeviceToken(deviceCode: String): OidcDeviceTokenResponse {
        val response: OidcDeviceTokenResponse = transport.requestDecoded(
            "oidc/device/token",
            method = "POST",
            body = OidcDeviceTokenRequest(deviceCode),
            authorized = false,
        )
        authManager.save(TokenPair(response.token, response.refreshToken, response.expiresAt))
        authManager.recordCapabilities(response.user)
        return response
    }
}
