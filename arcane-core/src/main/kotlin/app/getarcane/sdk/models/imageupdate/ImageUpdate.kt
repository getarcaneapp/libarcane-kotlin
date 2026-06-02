package app.getarcane.sdk.models.imageupdate

import app.getarcane.sdk.models.containerregistry.ContainerRegistryCredential
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Result of an image update check. */
@Serializable
public data class ImageUpdateResponse(
    public val hasUpdate: Boolean,
    public val updateType: String,
    public val currentVersion: String,
    public val latestVersion: String? = null,
    public val currentDigest: String? = null,
    public val latestDigest: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val checkTime: Instant,
    public val responseTimeMs: Int,
    public val error: String? = null,
    public val authMethod: String? = null,
    public val authUsername: String? = null,
    public val authRegistry: String? = null,
    public val usedCredential: Boolean? = null,
)

/** Aggregate summary of image update status across an environment. */
@Serializable
public data class ImageUpdateSummary(
    public val totalImages: Int,
    public val imagesWithUpdates: Int,
    public val digestUpdates: Int,
    public val errorsCount: Int,
)

/** Request body for the batch image update check endpoint. */
@Serializable
public data class BatchImageUpdateRequest(
    public val imageRefs: List<String>,
    public val credentials: List<ContainerRegistryCredential>? = null,
)

/** Request body for the "check all images" endpoint. */
@Serializable
public data class CheckAllImagesRequest(
    public val credentials: List<ContainerRegistryCredential>? = null,
)

/** Map keyed by image reference. Values may be null if the check failed. */
public typealias ImageUpdateBatchResponse = Map<String, ImageUpdateResponse?>
