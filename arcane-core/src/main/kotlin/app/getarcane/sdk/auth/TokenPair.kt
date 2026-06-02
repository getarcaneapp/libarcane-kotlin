package app.getarcane.sdk.auth

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Access + refresh token pair with the access token's expiry. */
@Serializable
public data class TokenPair(
    public val accessToken: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
)
