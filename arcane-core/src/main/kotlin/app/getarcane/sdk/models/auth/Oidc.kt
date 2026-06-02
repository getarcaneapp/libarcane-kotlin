package app.getarcane.sdk.models.auth

import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Status of OIDC configuration on the server. */
@Serializable
public data class OidcStatusInfo(
    public val envForced: Boolean,
    public val envConfigured: Boolean,
    public val mergeAccounts: Boolean,
    public val providerName: String? = null,
    public val providerLogoUrl: String? = null,
)

/** Request body for obtaining an OIDC authorization URL. */
@Serializable
public data class OidcAuthUrlRequest(
    public val redirectUri: String,
    public val mobileRedirectUri: String? = null,
)

/** Response carrying the OIDC authorization URL. */
@Serializable
public data class OidcAuthUrlResponse(
    public val authUrl: String,
)

/** OIDC provider configuration details. */
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

/** Request body for the OIDC authorization callback. */
@Serializable
public data class OidcCallbackRequest(
    public val code: String,
    public val state: String,
    public val mobileRedirectUri: String? = null,
)

/** Response returned after a successful OIDC callback, carrying tokens and the user. */
@Serializable
public data class OidcCallbackResponse(
    public val success: Boolean,
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
)

/** Request body for starting the OIDC device authorization flow. */
@Serializable
public data class OidcDeviceAuthRequest(
    public val redirectUri: String? = null,
)

/** Response carrying device and user codes for the OIDC device authorization flow. */
@Serializable
public data class OidcDeviceAuthResponse(
    public val deviceCode: String,
    public val userCode: String,
    public val verificationUri: String,
    public val verificationUriComplete: String? = null,
    public val expiresIn: Int,
    public val interval: Int? = null,
)

/** Request body for polling the OIDC device token endpoint. */
@Serializable
public data class OidcDeviceTokenRequest(
    public val deviceCode: String,
)

/** Response returned when the OIDC device flow completes, carrying tokens and the user. */
@Serializable
public data class OidcDeviceTokenResponse(
    public val success: Boolean,
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
)
