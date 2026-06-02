package app.getarcane.sdk.models.dashboard

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.models.base.PaginationResponse
import app.getarcane.sdk.models.container.ContainerStatusCounts
import app.getarcane.sdk.models.container.ContainerSummary
import app.getarcane.sdk.models.image.ImageUsageCounts
import app.getarcane.sdk.models.version.VersionInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Kinds of dashboard action items. */
@Serializable
public enum class ActionItemKind(public val wire: String) {
    @SerialName("stopped_containers")
    STOPPED_CONTAINERS("stopped_containers"),

    @SerialName("image_updates")
    IMAGE_UPDATES("image_updates"),

    @SerialName("actionable_vulnerabilities")
    ACTIONABLE_VULNERABILITIES("actionable_vulnerabilities"),

    @SerialName("expiring_keys")
    EXPIRING_KEYS("expiring_keys"),
}

/** Severity of a dashboard action item. */
@Serializable
public enum class ActionItemSeverity(public val wire: String) {
    @SerialName("warning")
    WARNING("warning"),

    @SerialName("critical")
    CRITICAL("critical"),
}

/** A single attention item rendered on the dashboard. */
@Serializable
public data class ActionItem(
    public val kind: ActionItemKind,
    public val count: Int,
    public val severity: ActionItemSeverity,
)

/** Collection of dashboard action items. */
@Serializable
public data class ActionItems(
    public val items: List<ActionItem> = emptyList(),
)

/**
 * Settings payload carried in dashboard snapshots. Currently empty server-side — modeled as an
 * opaque object so future fields land transparently.
 */
@Serializable
public class DashboardSnapshotSettings

/** State of an environment snapshot in a dashboard overview. */
@Serializable
public enum class EnvironmentSnapshotState(public val wire: String) {
    @SerialName("ready")
    READY("ready"),

    @SerialName("skipped")
    SKIPPED("skipped"),

    @SerialName("error")
    ERROR("error"),
}

/**
 * Dashboard environment-overview row, one per visible environment. The full [environment] blob is
 * preserved as JSON.
 */
@Serializable
public data class DashboardEnvironmentOverview(
    public val environment: JsonValue,
    public val containers: ContainerStatusCounts,
    public val imageUsageCounts: ImageUsageCounts,
    public val actionItems: ActionItems,
    public val settings: DashboardSnapshotSettings,
    public val versionInfo: VersionInfo? = null,
    public val snapshotState: EnvironmentSnapshotState,
    public val snapshotError: String? = null,
)

/** Aggregate summary across all dashboard environments. */
@Serializable
public data class DashboardEnvironmentsSummary(
    public val totalEnvironments: Int = 0,
    public val onlineEnvironments: Int = 0,
    public val standbyEnvironments: Int = 0,
    public val offlineEnvironments: Int = 0,
    public val pendingEnvironments: Int = 0,
    public val errorEnvironments: Int = 0,
    public val disabledEnvironments: Int = 0,
    // Explicit all-zero counts are supplied here because `ContainerStatusCounts`/`ImageUsageCounts`
    // have no parameter defaults of their own.
    public val containers: ContainerStatusCounts = ContainerStatusCounts(0, 0, 0),
    public val imageUsageCounts: ImageUsageCounts = ImageUsageCounts(0, 0, 0, 0),
    public val environmentsWithActionItems: Int = 0,
)

/** Overview of all dashboard environments plus their aggregate summary. */
@Serializable
public data class DashboardEnvironmentsOverview(
    public val summary: DashboardEnvironmentsSummary,
    public val environments: List<DashboardEnvironmentOverview> = emptyList(),
)

/** Container table payload on the dashboard. */
@Serializable
public data class DashboardSnapshotContainers(
    public val data: List<ContainerSummary> = emptyList(),
    public val counts: ContainerStatusCounts = ContainerStatusCounts(0, 0, 0),
    public val pagination: PaginationResponse,
)

/**
 * Image table payload on the dashboard. Images are kept as opaque JSON until the image-summary type
 * is fully modeled.
 */
@Serializable
public data class DashboardSnapshotImages(
    public val data: List<JsonValue> = emptyList(),
    public val pagination: PaginationResponse,
)

/** Top-level dashboard first-paint snapshot. */
@Serializable
public data class DashboardSnapshot(
    public val containers: DashboardSnapshotContainers,
    public val images: DashboardSnapshotImages,
    public val imageUsageCounts: ImageUsageCounts,
    public val actionItems: ActionItems,
    public val settings: DashboardSnapshotSettings,
)
