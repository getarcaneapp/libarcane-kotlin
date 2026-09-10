package app.getarcane.sdk.models.auth

import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Request body for username/password login. */
@Serializable
public data class LoginRequest(
    public val username: String,
    public val password: String,
) {
    override fun toString(): String = "LoginRequest(username=$username, password=[REDACTED])"
}

/** Response returned on successful login, carrying tokens and the authenticated user. */
@Serializable
public data class LoginResponse(
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
    public val user: User,
) {
    /** Authentication material is deliberately excluded from default diagnostics. */
    override fun toString(): String = "LoginResponse([REDACTED], userId=${user.id})"
}

/** A server-side second-factor transaction. [options] stays opaque to application code. */
@Serializable
public data class MFAChallenge(
    public val transactionId: String,
    public val method: String = "passkey",
    public val options: JsonObject = JsonObject(emptyMap()),
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
) {
    override fun toString(): String = "MFAChallenge([REDACTED])"
}

/**
 * Result shared by password, OIDC, passkey, and MFA login endpoints. A pending MFA result never
 * contains usable tokens and is intentionally distinct from [LoginResponse].
 */
@Serializable(with = AuthenticationResultSerializer::class)
public sealed interface AuthenticationResult {
    public data class Authenticated(public val response: LoginResponse) : AuthenticationResult {
        override fun toString(): String = "AuthenticationResult.Authenticated([REDACTED])"
    }

    public data class MfaRequired(public val challenge: MFAChallenge) : AuthenticationResult {
        override fun toString(): String = "AuthenticationResult.MfaRequired([REDACTED])"
    }
}

/** Raised by source-compatible login conveniences when the server requires MFA. */
public class MFARequiredException(public val challenge: MFAChallenge) :
    Exception("Multi-factor authentication is required.") {
    override fun toString(): String = "MFARequiredException([REDACTED])"
}

/** Supports current status-tagged auth responses plus the legacy successful-login shape. */
public object AuthenticationResultSerializer : KSerializer<AuthenticationResult> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): AuthenticationResult {
        require(decoder is JsonDecoder) { "AuthenticationResult can only be decoded from JSON" }
        val element = decoder.decodeJsonElement()
        val objectValue = try {
            element.jsonObject
        } catch (failure: IllegalArgumentException) {
            throw SerializationException("Authentication result must be a JSON object", failure)
        }
        return when (objectValue["status"]?.jsonPrimitive?.contentOrNull) {
            "mfa_required" -> {
                val challengeElement = objectValue["mfa"] ?: objectValue["challenge"] ?: element
                AuthenticationResult.MfaRequired(
                    decoder.json.decodeFromJsonElement(MFAChallenge.serializer(), challengeElement),
                )
            }
            null, "authenticated" -> AuthenticationResult.Authenticated(
                decoder.json.decodeFromJsonElement(LoginResponse.serializer(), element),
            )
            else -> throw SerializationException("Authentication result status is unsupported")
        }
    }

    override fun serialize(encoder: Encoder, value: AuthenticationResult) {
        require(encoder is JsonEncoder) { "AuthenticationResult can only be encoded as JSON" }
        val objectValue = when (value) {
            is AuthenticationResult.Authenticated -> {
                val login = encoder.json.encodeToJsonElement(LoginResponse.serializer(), value.response).jsonObject
                JsonObject(login + ("success" to JsonPrimitive(true)) + ("status" to JsonPrimitive("authenticated")))
            }
            is AuthenticationResult.MfaRequired -> buildJsonObject {
                put("success", JsonPrimitive(true))
                put("status", JsonPrimitive("mfa_required"))
                put("mfa", encoder.json.encodeToJsonElement(MFAChallenge.serializer(), value.challenge))
            }
        }
        encoder.encodeJsonElement(objectValue)
    }
}

/** Request body for exchanging a refresh token for a new access token. */
@Serializable
public data class RefreshRequest(
    public val refreshToken: String,
) {
    override fun toString(): String = "RefreshRequest([REDACTED])"
}

/** Response carrying refreshed tokens and their expiry. */
@Serializable
public data class TokenRefreshResponse(
    public val token: String,
    public val refreshToken: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
) {
    override fun toString(): String = "TokenRefreshResponse([REDACTED])"
}

/** Request body for changing a user's password. */
@Serializable
public data class PasswordChange(
    public val currentPassword: String? = null,
    public val newPassword: String,
) {
    override fun toString(): String = "PasswordChange([REDACTED])"
}

/** Configuration for automatic login with a preset username. */
@Serializable
public data class AutoLoginConfig(
    public val enabled: Boolean,
    public val username: String,
)
