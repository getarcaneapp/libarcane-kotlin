package app.getarcane.sdk.models.image

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.models.base.PaginationResponse
import app.getarcane.sdk.models.vulnerability.VulnerabilityScanSummary
import kotlinx.serialization.Serializable

/** Describes the project, container, or other consumer using an image. */
@Serializable
public data class ImageUsedBy(
    public val type: String,
    public val name: String,
    public val id: String? = null,
)

/** Image summary as returned by the list endpoint. */
@Serializable
public data class ImageSummary(
    public val id: String,
    public val repoTags: List<String>,
    public val repoDigests: List<String>,
    public val created: Long,
    public val size: Long,
    public val virtualSize: Long,
    public val labels: Map<String, JsonValue>,
    public val inUse: Boolean,
    public val usedBy: List<ImageUsedBy>? = null,
    public val repo: String,
    public val tag: String,
    public val updateInfo: ImageUpdateInfo? = null,
    public val vulnerabilityScan: VulnerabilityScanSummary? = null,
)

/** Result of an image prune operation. */
@Serializable
public data class ImagePruneReport(
    public val imagesDeleted: List<String>,
    public val spaceReclaimed: Long,
)

/** Aggregate image usage counts for an environment. */
@Serializable
public data class ImageUsageCounts(
    public val imagesInuse: Int,
    public val imagesUnused: Int,
    public val totalImages: Int,
    public val totalImageSize: Long,
)

/** Result of a `docker load` operation. */
@Serializable
public data class ImageLoadResult(
    public val stream: String,
)

/** Standardized NDJSON envelope for pull/build/deploy streams. */
@Serializable
public data class ImageProgressDetail(
    public val current: Long? = null,
    public val total: Long? = null,
)

/** A single progress event emitted by streaming image endpoints. */
@Serializable
public data class ImageProgressEvent(
    public val type: String? = null,
    public val phase: String? = null,
    public val service: String? = null,
    public val status: String? = null,
    public val id: String? = null,
    public val progressDetail: ImageProgressDetail? = null,
    public val error: String? = null,
)

/** Paginated list response for images. */
@Serializable
public data class ImageListResponse(
    public val success: Boolean,
    public val data: List<ImageSummary>,
    public val pagination: PaginationResponse,
)
