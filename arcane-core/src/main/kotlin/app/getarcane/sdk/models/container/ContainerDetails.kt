package app.getarcane.sdk.models.container

import app.getarcane.sdk.models.base.PaginationResponse
import kotlinx.serialization.Serializable

/** Detailed container information. Mirrors Swift `ContainerDetails` (Models/container/ContainerDetails.swift). */
@Serializable
public data class ContainerDetails(
    public val id: String,
    public val name: String,
    public val image: String,
    public val imageId: String,
    public val created: String,
    public val state: ContainerState,
    public val config: ContainerConfig,
    public val hostConfig: ContainerHostConfig,
    public val networkSettings: ContainerNetworkSettings,
    public val ports: List<ContainerPort>,
    public val mounts: List<ContainerMount>,
    public val labels: Map<String, String>? = null,
    public val composeInfo: ContainerComposeInfo? = null,
    public val redeployDisabled: Boolean? = null,
)

/** Paginated container list response. Includes optional groups + status counts. Mirrors Swift `ContainerListResponse`. */
@Serializable
public data class ContainerListResponse(
    public val success: Boolean,
    public val data: List<ContainerSummary>,
    public val groups: List<ContainerSummaryGroup>? = null,
    public val counts: ContainerStatusCounts,
    public val pagination: PaginationResponse,
)
