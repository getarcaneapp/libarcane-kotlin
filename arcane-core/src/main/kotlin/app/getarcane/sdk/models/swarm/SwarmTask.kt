package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** A swarm task summary (one row in a service's task list). */
@Serializable
public data class SwarmTaskSummary(
    public val id: String,
    public val name: String,
    public val serviceId: String,
    public val serviceName: String,
    public val nodeId: String,
    public val nodeName: String,
    public val desiredState: String,
    public val currentState: String,
    public val error: String? = null,
    public val containerId: String? = null,
    public val image: String? = null,
    public val slot: Int? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)
