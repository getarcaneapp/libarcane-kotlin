package app.getarcane.sdk.models.auth

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Opaque WebAuthn credential response. Platform ceremony adapters create this value; its payload is
 * intentionally absent from [toString] so credentials cannot leak through ordinary diagnostics.
 */
@Serializable(with = PasskeyCredentialSerializer::class)
public class PasskeyCredential internal constructor(internal val value: JsonObject) {
    public companion object {
        /** Converts the opaque JSON returned by a trusted platform credential provider. */
        public fun fromProviderResponse(
            responseJson: String,
            json: Json = ArcaneJson.default,
        ): PasskeyCredential = try {
            PasskeyCredential(json.parseToJsonElement(responseJson).jsonObject)
        } catch (_: SerializationException) {
            throw PasskeyCredentialDecodingException()
        } catch (_: IllegalArgumentException) {
            throw PasskeyCredentialDecodingException()
        }
    }

    override fun equals(other: Any?): Boolean = other is PasskeyCredential && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "PasskeyCredential([REDACTED])"
}

/** Stable decoding failure that never includes the provider response or credential material. */
public class PasskeyCredentialDecodingException : IllegalArgumentException(
    "Passkey response could not be decoded.",
)

public object PasskeyCredentialSerializer : KSerializer<PasskeyCredential> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun serialize(encoder: Encoder, value: PasskeyCredential) {
        require(encoder is JsonEncoder) { "PasskeyCredential can only be encoded as JSON" }
        encoder.encodeJsonElement(value.value)
    }

    override fun deserialize(decoder: Decoder): PasskeyCredential {
        require(decoder is JsonDecoder) { "PasskeyCredential can only be decoded from JSON" }
        return PasskeyCredential(decoder.decodeJsonElement().jsonObject)
    }
}

/** Opaque options and identifiers returned by a WebAuthn begin endpoint. */
@Serializable
public data class PasskeyChallenge(
    public val ceremonyId: String,
    public val transactionId: String? = null,
    public val options: JsonObject,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
) {
    override fun toString(): String = "PasskeyChallenge([REDACTED])"
}

@Serializable
public data class PasskeyLoginAvailability(public val available: Boolean)

/** Discovery contract served by Arcane at `/arcane-mobile-passkey.json`. */
@Serializable
public data class MobilePasskeyBridgeManifest(
    public val version: Int,
    public val path: String,
    public val requestFragmentParameter: String,
    public val maximumRequestBytes: Int,
)

@Serializable
public data class PasskeySummary(
    public val id: String,
    public val name: String,
    public val rpId: String,
    public val aaguid: String? = null,
    public val transports: List<String>? = null,
    public val backupEligible: Boolean = false,
    public val backupState: Boolean = false,
    public val cloneWarning: Boolean = false,
    public val authenticatorAttachment: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastUsedAt: Instant? = null,
)

@Serializable
public data class PasskeyCapabilities(
    public val passkeyMfaEnabled: Boolean,
    public val passkeyCount: Int,
    public val hasLocalPassword: Boolean,
    public val hasOidcFallback: Boolean,
    public val canEnrollWithActiveSession: Boolean,
    public val canDeleteLastPasskey: Boolean,
    public val requiresStepUp: Boolean,
)

@Serializable
public data class MFAStatus(
    public val enabled: Boolean,
    public val passkeyCount: Int,
    public val recoveryCodesRemaining: Int,
)

/** Plaintext recovery codes are returned only once and omitted from default diagnostics. */
@Serializable
public data class RecoveryCodesResponse(public val codes: List<String>) {
    override fun toString(): String = "RecoveryCodesResponse([REDACTED])"
}

/** Request-scoped step-up bearer; its token is omitted from default diagnostics. */
@Serializable
public data class StepUpGrant(
    public val token: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
) {
    override fun toString(): String = "StepUpGrant([REDACTED])"
}

@Serializable
public data class MobilePasskeyCompletion(
    public val transactionId: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant,
) {
    override fun toString(): String = "MobilePasskeyCompletion([REDACTED])"
}

@Serializable
internal data class PasskeyFinishRequest(
    val ceremonyId: String,
    val credential: PasskeyCredential,
    val name: String? = null,
) {
    override fun toString(): String = "PasskeyFinishRequest([REDACTED])"
}

@Serializable
internal data class MobilePasskeyFinishRequest(
    val ceremonyId: String,
    val credential: PasskeyCredential,
    val codeChallenge: String,
) {
    override fun toString(): String = "MobilePasskeyFinishRequest([REDACTED])"
}

@Serializable
internal data class MobilePasskeyExchangeRequest(
    val transactionId: String,
    val codeVerifier: String,
) {
    override fun toString(): String = "MobilePasskeyExchangeRequest([REDACTED])"
}

@Serializable
internal data class MFAStartRequest(val transactionId: String) {
    override fun toString(): String = "MFAStartRequest([REDACTED])"
}

@Serializable
internal data class MFAFinishRequest(
    val transactionId: String,
    val credential: PasskeyCredential,
) {
    override fun toString(): String = "MFAFinishRequest([REDACTED])"
}

@Serializable
internal data class RecoveryCodeRequest(
    val transactionId: String,
    val code: String,
) {
    override fun toString(): String = "RecoveryCodeRequest([REDACTED])"
}

@Serializable
internal data class RenamePasskeyRequest(val name: String)

@Serializable
internal data class PasswordStepUpRequest(val password: String) {
    override fun toString(): String = "PasswordStepUpRequest([REDACTED])"
}
