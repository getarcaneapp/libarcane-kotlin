package app.getarcane.sdk.models.container

import app.getarcane.sdk.models.image.ImageUpdateInfo
import kotlinx.serialization.Serializable

/** A container summary as returned by the list endpoint. */
@Serializable
public data class ContainerSummary(
    public val id: String,
    public val names: List<String>,
    public val image: String,
    public val imageId: String,
    public val command: String,
    public val created: Long,
    public val ports: List<ContainerPort>,
    public val labels: Map<String, String> = emptyMap(),
    public val state: String,
    public val status: String,
    public val hostConfig: ContainerHostConfig,
    public val networkSettings: ContainerNetworkSettings,
    public val mounts: List<ContainerMount>,
    public val updateInfo: ImageUpdateInfo? = null,
    public val redeployDisabled: Boolean? = null,
)

/** A group of container summaries, e.g. by Compose project. */
@Serializable
public data class ContainerSummaryGroup(
    public val groupName: String,
    public val items: List<ContainerSummary>,
)

/** Counts of containers by status. */
@Serializable
public data class ContainerStatusCounts(
    public val runningContainers: Int,
    public val stoppedContainers: Int,
    public val totalContainers: Int,
)

/** Result of a container batch action (start/stop/etc). */
@Serializable
public data class ContainerActionResult(
    public val started: List<String>? = null,
    public val stopped: List<String>? = null,
    public val failed: List<String>? = null,
    public val success: Boolean,
    public val errors: List<String>? = null,
)

/** A newly created container. */
@Serializable
public data class ContainerCreated(
    public val id: String,
    public val name: String,
    public val image: String,
    public val status: String,
    public val created: String,
)
