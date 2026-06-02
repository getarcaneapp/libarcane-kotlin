package app.getarcane.sdk.auth

import app.getarcane.sdk.http.ArcaneTransport
import app.getarcane.sdk.http.request
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.auth.LoginRequest
import app.getarcane.sdk.models.auth.LoginResponse
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
        val response: LoginResponse = transport.request(
            "auth/login",
            method = "POST",
            body = LoginRequest(username, password),
            authorized = false,
        )
        authManager.save(response)
        authManager.recordCapabilities(response.user)
        return response
    }

    public suspend fun logout() {
        transport.request<MessageResponse>("auth/logout", method = "POST")
        authManager.clear()
    }

    public suspend fun me(): User {
        val user: User = transport.request("auth/me")
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
        val response: OidcCallbackResponse = transport.requestDecoded(
            "oidc/callback",
            method = "POST",
            body = OidcCallbackRequest(code, state, mobileRedirectUri),
            authorized = false,
        )
        authManager.save(TokenPair(response.token, response.refreshToken, response.expiresAt))
        authManager.recordCapabilities(response.user)
        return response
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
