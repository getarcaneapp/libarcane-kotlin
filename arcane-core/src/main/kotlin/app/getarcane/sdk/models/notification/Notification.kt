package app.getarcane.sdk.models.notification

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Identifiers for notification providers. */
@Serializable
public enum class NotificationProvider(public val wire: String) {
    @SerialName("discord")
    DISCORD("discord"),

    @SerialName("email")
    EMAIL("email"),

    @SerialName("telegram")
    TELEGRAM("telegram"),

    @SerialName("signal")
    SIGNAL("signal"),

    @SerialName("slack")
    SLACK("slack"),

    @SerialName("ntfy")
    NTFY("ntfy"),

    @SerialName("pushover")
    PUSHOVER("pushover"),

    @SerialName("gotify")
    GOTIFY("gotify"),

    @SerialName("matrix")
    MATRIX("matrix"),

    @SerialName("generic")
    GENERIC("generic"),
}

/** Configured settings for a notification provider. */
@Serializable
public data class NotificationSettings(
    public val id: Long,
    public val provider: NotificationProvider,
    public val enabled: Boolean,
    public val config: Map<String, JsonValue> = emptyMap(),
)

/** Body for updating a notification provider's settings. */
@Serializable
public data class UpdateNotificationSettings(
    public val provider: NotificationProvider,
    public val enabled: Boolean,
    public val config: Map<String, JsonValue> = emptyMap(),
)

/** Configured Apprise notification settings. */
@Serializable
public data class AppriseSettings(
    public val id: Long,
    public val apiUrl: String,
    public val enabled: Boolean,
    public val imageUpdateTag: String,
    public val containerUpdateTag: String,
)

/** Body for updating Apprise notification settings. */
@Serializable
public data class UpdateAppriseSettings(
    public val apiUrl: String,
    public val enabled: Boolean,
    public val imageUpdateTag: String,
    public val containerUpdateTag: String,
)

/** The kind of test notification to send. */
@Serializable
public enum class NotificationTestType(public val wire: String) {
    @SerialName("simple")
    SIMPLE("simple"),

    @SerialName("image-update")
    IMAGE_UPDATE("image-update"),

    @SerialName("batch-image-update")
    BATCH_IMAGE_UPDATE("batch-image-update"),

    @SerialName("vulnerability-found")
    VULNERABILITY_FOUND("vulnerability-found"),

    @SerialName("prune-report")
    PRUNE_REPORT("prune-report"),

    @SerialName("auto-heal")
    AUTO_HEAL("auto-heal"),
}

/** The kind of notification dispatch payload. */
@Serializable
public enum class NotificationDispatchKind(public val wire: String) {
    @SerialName("image_update")
    IMAGE_UPDATE("image_update"),

    @SerialName("batch_image_update")
    BATCH_IMAGE_UPDATE("batch_image_update"),

    @SerialName("container_update")
    CONTAINER_UPDATE("container_update"),

    @SerialName("vulnerability_found")
    VULNERABILITY_FOUND("vulnerability_found"),

    @SerialName("prune_report")
    PRUNE_REPORT("prune_report"),

    @SerialName("auto_heal")
    AUTO_HEAL("auto_heal"),
}

/** Dispatch payload for a single image update. */
@Serializable
public data class NotificationDispatchImageUpdate(
    public val imageRef: String,
    public val updateInfo: JsonValue,
)

/** Dispatch payload for a batch of image updates. */
@Serializable
public data class NotificationDispatchBatchImageUpdate(
    public val updates: Map<String, JsonValue>,
)

/** Dispatch payload for a container update. */
@Serializable
public data class NotificationDispatchContainerUpdate(
    public val containerName: String,
    public val imageRef: String,
    public val oldDigest: String? = null,
    public val newDigest: String? = null,
)

/** Dispatch payload for a discovered vulnerability. */
@Serializable
public data class NotificationDispatchVulnerabilityFound(
    public val cveId: String,
    public val cveLink: String,
    public val severity: String,
    public val imageName: String,
    public val fixedVersion: String? = null,
    public val pkgName: String? = null,
    public val installedVersion: String? = null,
)

/** Dispatch payload for a prune report. */
@Serializable
public data class NotificationDispatchPruneReport(
    public val result: JsonValue,
)

/** Dispatch payload for an auto-heal event. */
@Serializable
public data class NotificationDispatchAutoHeal(
    public val containerName: String,
    public val containerId: String,
)

/** Request body for dispatching a notification. */
@Serializable
public data class NotificationDispatchRequest(
    public val kind: NotificationDispatchKind,
    public val imageUpdate: NotificationDispatchImageUpdate? = null,
    public val batchImageUpdate: NotificationDispatchBatchImageUpdate? = null,
    public val containerUpdate: NotificationDispatchContainerUpdate? = null,
    public val vulnerabilityFound: NotificationDispatchVulnerabilityFound? = null,
    public val pruneReport: NotificationDispatchPruneReport? = null,
    public val autoHeal: NotificationDispatchAutoHeal? = null,
)
