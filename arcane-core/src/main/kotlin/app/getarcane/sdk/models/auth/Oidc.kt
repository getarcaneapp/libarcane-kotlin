package app.getarcane.sdk.models.auth

import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Mirrors Swift `OIDCStatusInfo` (Models/Auth/OIDC.swift). */
@Serializable
public data class OidcStatusInfo(
    public val envForced: Boolean,
    public val envConfigured: Boolean,
    public val mergeAccounts: Boolean,
    public val providerName: String? = null,
    public val providerLogoUrl: String? = null,
)

/** Mirrors Swift `OIDCAuthURLRequest`. */
@Serializable
public data class OidcAuthUrlRequest(
    public val redirectUri: String,
    public val mobileRedirectUri: String? = null,
)

/** Mirrors Swift `OIDCAuthURLResponse`. */
@Serializable
public data class OidcAuthUrlResponse(
    public val authUrl: String,
)

/** Mirrors Swift `OIDCConfigResponse`. */
@Serializable
public data class OidcConfigResponse(
    public val clientId: String,
    public val redirectUri: String,
    public val issuerUrl: String,
    public val authorizationEndpoint: String,
    public val tokenEndpoint: String,
    public val userinfoEndpoint: String,
    public val deviceAuthorizationEndpoint: String? = null,
    public val scopes: String,
)

/** Mirrors Swift `OIDCCallbackRequest`. */
@Serializable
public data class OidcCallbackRequest(
    public val code: String,
    public val state: String,
    public val mobileRedirectUri: String? = null,
)

/** Mirrors Swift `OIDCCallbackResponse`. */
@Serializable
public data class OidcCallbackResponse(
    public val success: Boolean,
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
)

/** Mirrors Swift `OIDCDeviceAuthRequest`. */
@Serializable
public data class OidcDeviceAuthRequest(
    public val redirectUri: String? = null,
)

/** Mirrors Swift `OIDCDeviceAuthResponse`. */
@Serializable
public data class OidcDeviceAuthResponse(
    public val deviceCode: String,
    public val userCode: String,
    public val verificationUri: String,
    public val verificationUriComplete: String? = null,
    public val expiresIn: Int,
    public val interval: Int? = null,
)

/** Mirrors Swift `OIDCDeviceTokenRequest`. */
@Serializable
public data class OidcDeviceTokenRequest(
    public val deviceCode: String,
)

/** Mirrors Swift `OIDCDeviceTokenResponse`. */
@Serializable
public data class OidcDeviceTokenResponse(
    public val success: Boolean,
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
)
