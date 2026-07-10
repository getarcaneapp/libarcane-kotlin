package app.getarcane.sdk.models.image

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.Serializable

/** Subject an in-toto attestation statement applies to. */
@Serializable
public data class ImageAttestationSubject(
    public val name: String,
    public val digest: Map<String, String>,
)

/** One in-toto attestation attached to an image. */
@Serializable
public data class ImageAttestation(
    public val digest: String,
    public val mediaType: String,
    public val artifactType: String? = null,
    public val predicateType: String,
    public val statementType: String? = null,
    public val subject: List<ImageAttestationSubject>? = null,
    public val platform: String? = null,
    public val size: Long,
    public val statement: JsonValue? = null,
) {
    public val id: String get() = digest + predicateType + platform.orEmpty()
}

/** Response from an image attestation listing endpoint. */
@Serializable
public data class ImageAttestationList(
    public val imageRef: String,
    public val subjectDigest: String,
    public val platform: String? = null,
    public val attestations: List<ImageAttestation>,
)
