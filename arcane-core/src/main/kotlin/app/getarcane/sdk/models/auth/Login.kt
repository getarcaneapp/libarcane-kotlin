package app.getarcane.sdk.models.auth

import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Request body for username/password login. */
@Serializable
public data class LoginRequest(
    public val username: String,
    public val password: String,
)

/** Response returned on successful login, carrying tokens and the authenticated user. */
@Serializable
public data class LoginResponse(
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
)

/** Request body for exchanging a refresh token for a new access token. */
@Serializable
public data class RefreshRequest(
    public val refreshToken: String,
)

/** Response carrying refreshed tokens and their expiry. */
@Serializable
public data class TokenRefreshResponse(
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
)

/** Request body for changing a user's password. */
@Serializable
public data class PasswordChange(
    public val currentPassword: String? = null,
    public val newPassword: String,
)

/** Configuration for automatic login with a preset username. */
@Serializable
public data class AutoLoginConfig(
    public val enabled: Boolean,
    public val username: String,
)
