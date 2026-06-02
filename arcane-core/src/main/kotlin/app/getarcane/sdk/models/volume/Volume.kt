package app.getarcane.sdk.models.volume

import app.getarcane.sdk.models.base.PaginationResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Docker SDK volume usage data. */
@Serializable
public data class VolumeUsageData(
    @SerialName("Size")
    public val size: Long = 0,
    @SerialName("RefCount")
    public val refCount: Long = 0,
)

/**
 * A Docker volume. Several fields default to empty/false/0 because the Docker daemon often emits
 * `null` for empty labels/options/containers; the project [ArcaneJson] config
 * (`coerceInputValues = true`) coerces those nulls to these defaults.
 */
@Serializable
public data class Volume(
    public val id: String,
    public val name: String = "",
    public val driver: String = "",
    public val mountpoint: String = "",
    public val scope: String = "",
    public val options: Map<String, String> = emptyMap(),
    public val labels: Map<String, String> = emptyMap(),
    public val createdAt: String = "",
    public val inUse: Boolean = false,
    public val usageData: VolumeUsageData? = null,
    public val size: Long = 0,
    public val containers: List<String> = emptyList(),
)

/** Counts of volumes by usage status. */
@Serializable
public data class VolumeUsageCounts(
    public val inuse: Int = 0,
    public val unused: Int = 0,
    public val total: Int = 0,
)

/** Result of a volume prune operation. */
@Serializable
public data class VolumePruneReport(
    public val volumesDeleted: List<String> = emptyList(),
    public val spaceReclaimed: ULong = 0u,
)

/** Used to create a new volume. */
@Serializable
public data class CreateVolume(
    public val name: String,
    public val driver: String? = null,
    public val driverOpts: Map<String, String>? = null,
    public val labels: Map<String, String>? = null,
)

/** Per-volume usage details returned by `GET /volumes/{name}/usage`. */
@Serializable
public data class VolumeUsage(
    public val inUse: Boolean,
    public val containers: List<String> = emptyList(),
)

/** Size information for a single volume. */
@Serializable
public data class VolumeSizeInfo(
    public val name: String,
    public val size: Long,
    public val refCount: Long,
)

/** The page envelope returned by `GET /environments/{id}/volumes`. */
@Serializable
public data class VolumeListPage(
    public val success: Boolean,
    public val data: List<Volume>,
    public val counts: VolumeUsageCounts,
    public val pagination: PaginationResponse,
)
