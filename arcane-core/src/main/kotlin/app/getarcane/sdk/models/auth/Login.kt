package app.getarcane.sdk.models.auth

import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Mirrors Swift `LoginRequest` (Models/Auth/Login.swift). */
@Serializable
public data class LoginRequest(
    public val username: String,
    public val password: String,
)

/** Mirrors Swift `LoginResponse`. */
@Serializable
public data class LoginResponse(
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
)

/** Mirrors Swift `RefreshRequest`. */
@Serializable
public data class RefreshRequest(
    public val refreshToken: String,
)

/** Mirrors Swift `TokenRefreshResponse`. */
@Serializable
public data class TokenRefreshResponse(
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
)

/** Mirrors Swift `PasswordChange`. */
@Serializable
public data class PasswordChange(
    public val currentPassword: String? = null,
    public val newPassword: String,
)

/** Mirrors Swift `AutoLoginConfig`. */
@Serializable
public data class AutoLoginConfig(
    public val enabled: Boolean,
    public val username: String,
)
