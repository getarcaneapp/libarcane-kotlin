package app.getarcane.sdk.models.updater

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Options passed when triggering the updater (`POST .../updater/run`). */
@Serializable
public data class UpdaterOptions(
    public val type: String? = null,
    public val resourceIds: List<String>? = null,
    public val forceUpdate: Boolean? = null,
    public val dryRun: Boolean? = null,
)

/** Result for a single resource updated by the updater. */
@Serializable
public data class UpdaterResourceResult(
    public val resourceId: String,
    public val resourceName: String? = null,
    public val resourceType: String,
    public val status: String,
    public val updateAvailable: Boolean? = null,
    public val updateApplied: Boolean? = null,
    public val oldImages: Map<String, String>? = null,
    public val newImages: Map<String, String>? = null,
    public val error: String? = null,
    public val details: Map<String, JsonValue>? = null,
)

/** Overall result of an updater run. */
@Serializable
public data class UpdaterResult(
    public val success: Boolean? = null,
    public val checked: Int = 0,
    public val updated: Int = 0,
    public val skipped: Int = 0,
    public val failed: Int = 0,
    public val startTime: String? = null,
    public val endTime: String? = null,
    public val duration: String = "",
    public val items: List<UpdaterResourceResult> = emptyList(),
)

/** Live status of the updater. */
@Serializable
public data class UpdaterStatus(
    public val updatingContainers: Int = 0,
    public val updatingProjects: Int = 0,
    public val containerIds: List<String> = emptyList(),
    public val projectIds: List<String> = emptyList(),
)

/** One row in the updater run history. */
@Serializable
public data class AutoUpdateRecord(
    public val id: String,
    public val resourceId: String,
    public val resourceType: String,
    public val resourceName: String,
    public val status: AutoUpdateRecordStatus,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val startTime: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val endTime: Instant? = null,
    public val updateAvailable: Boolean,
    public val updateApplied: Boolean,
    public val oldImageVersions: Map<String, JsonValue>? = null,
    public val newImageVersions: Map<String, JsonValue>? = null,
    public val error: String? = null,
    public val details: Map<String, JsonValue>? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
)

/** Status of an auto-update record. */
@Serializable
public enum class AutoUpdateRecordStatus(public val wire: String) {
    @SerialName("pending")
    PENDING("pending"),

    @SerialName("checking")
    CHECKING("checking"),

    @SerialName("updating")
    UPDATING("updating"),

    @SerialName("completed")
    COMPLETED("completed"),

    @SerialName("failed")
    FAILED("failed"),

    @SerialName("skipped")
    SKIPPED("skipped"),
}
