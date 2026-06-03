package app.getarcane.sdk.models.activity

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lifecycle state of a background [Activity]. Unknown wire values coerce to [UNKNOWN]
 * (the SDK's [app.getarcane.sdk.serialization.ArcaneJson] enables `coerceInputValues`).
 */
@Serializable
public enum class ActivityStatus(public val wire: String) {
    @SerialName("queued") QUEUED("queued"),
    @SerialName("running") RUNNING("running"),
    @SerialName("success") SUCCESS("success"),
    @SerialName("failed") FAILED("failed"),
    @SerialName("cancelled") CANCELLED("cancelled"),
    @SerialName("unknown") UNKNOWN("unknown"),
}

/** The kind of operation a background [Activity] represents. */
@Serializable
public enum class ActivityType(public val wire: String) {
    @SerialName("image_pull") IMAGE_PULL("image_pull"),
    @SerialName("image_build") IMAGE_BUILD("image_build"),
    @SerialName("image_update_check") IMAGE_UPDATE_CHECK("image_update_check"),
    @SerialName("project_pull") PROJECT_PULL("project_pull"),
    @SerialName("project_build") PROJECT_BUILD("project_build"),
    @SerialName("project_deploy") PROJECT_DEPLOY("project_deploy"),
    @SerialName("project_redeploy") PROJECT_REDEPLOY("project_redeploy"),
    @SerialName("project_down") PROJECT_DOWN("project_down"),
    @SerialName("project_restart") PROJECT_RESTART("project_restart"),
    @SerialName("project_destroy") PROJECT_DESTROY("project_destroy"),
    @SerialName("container_start") CONTAINER_START("container_start"),
    @SerialName("container_stop") CONTAINER_STOP("container_stop"),
    @SerialName("container_restart") CONTAINER_RESTART("container_restart"),
    @SerialName("container_redeploy") CONTAINER_REDEPLOY("container_redeploy"),
    @SerialName("container_delete") CONTAINER_DELETE("container_delete"),
    @SerialName("vulnerability_scan") VULNERABILITY_SCAN("vulnerability_scan"),
    @SerialName("auto_update") AUTO_UPDATE("auto_update"),
    @SerialName("system_prune") SYSTEM_PRUNE("system_prune"),
    @SerialName("resource_action") RESOURCE_ACTION("resource_action"),
    @SerialName("unknown") UNKNOWN("unknown"),
}

/** Severity of an [ActivityMessage]. */
@Serializable
public enum class ActivityMessageLevel(public val wire: String) {
    @SerialName("info") INFO("info"),
    @SerialName("warning") WARNING("warning"),
    @SerialName("error") ERROR("error"),
    @SerialName("success") SUCCESS("success"),
    @SerialName("unknown") UNKNOWN("unknown"),
}

/** Event kind emitted by the activity NDJSON stream. */
@Serializable
public enum class ActivityStreamEventType(public val wire: String) {
    @SerialName("snapshot") SNAPSHOT("snapshot"),
    @SerialName("activity") ACTIVITY("activity"),
    @SerialName("message") MESSAGE("message"),
    @SerialName("missed") MISSED("missed"),
    @SerialName("unknown") UNKNOWN("unknown"),
}

/** Who initiated an [Activity]. */
@Serializable
public data class ActivityStartedBy(
    public val userId: String? = null,
    public val username: String,
    public val displayName: String? = null,
)

/** A background activity (image pull, project deploy, container action, …) tracked by the server. */
@Serializable
public data class Activity(
    public val id: String,
    public val environmentId: String,
    public val sourceEnvironmentId: String? = null,
    public val sourceEnvironmentName: String? = null,
    public val type: ActivityType = ActivityType.UNKNOWN,
    public val status: ActivityStatus = ActivityStatus.UNKNOWN,
    public val resourceType: String? = null,
    public val resourceId: String? = null,
    public val resourceName: String? = null,
    public val progress: Int? = null,
    public val step: String = "",
    public val latestMessage: String = "",
    public val startedBy: ActivityStartedBy? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val startedAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val endedAt: Instant? = null,
    public val durationMs: Long? = null,
    public val error: String? = null,
    public val metadata: Map<String, JsonValue>? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
)

/** A single output line attached to an [Activity]. */
@Serializable
public data class ActivityMessage(
    public val id: String,
    public val activityId: String,
    public val level: ActivityMessageLevel = ActivityMessageLevel.UNKNOWN,
    public val message: String,
    public val payload: Map<String, JsonValue>? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
)

/** An [Activity] together with its recent output [messages]. */
@Serializable
public data class ActivityDetail(
    public val activity: Activity,
    public val messages: List<ActivityMessage> = emptyList(),
)

/** A frame from the activity NDJSON stream: a snapshot list, a single activity, or a message. */
@Serializable
public data class ActivityStreamEvent(
    public val type: ActivityStreamEventType = ActivityStreamEventType.UNKNOWN,
    public val activityId: String? = null,
    public val activity: Activity? = null,
    public val activities: List<Activity> = emptyList(),
    public val message: ActivityMessage? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val timestamp: Instant,
)

/** Result of clearing completed activity history for an environment. */
@Serializable
public data class ClearActivityHistoryResult(
    public val deleted: Long,
)
