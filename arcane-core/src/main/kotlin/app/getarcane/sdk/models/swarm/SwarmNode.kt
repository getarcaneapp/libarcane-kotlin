package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** State of the Arcane node-agent reporting back from a swarm node. Mirrors Swift `SwarmNodeAgentState`. */
@Serializable
public enum class SwarmNodeAgentState(public val wire: String) {
    @SerialName("none")
    NONE("none"),

    @SerialName("pending")
    PENDING("pending"),

    @SerialName("offline")
    OFFLINE("offline"),

    @SerialName("connected")
    CONNECTED("connected"),

    @SerialName("mismatched")
    MISMATCHED("mismatched"),
}

/** Coverage info for a swarm node from Arcane's node-agent perspective. Mirrors Swift `SwarmNodeAgentStatus`. */
@Serializable
public data class SwarmNodeAgentStatus(
    public val state: SwarmNodeAgentState,
    public val environmentId: String? = null,
    public val connected: Boolean? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastHeartbeat: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastPollAt: Instant? = null,
    public val reportedNodeId: String? = null,
    public val reportedHostname: String? = null,
)

/** A swarm node summary as returned by the list/get endpoints. Mirrors Swift `SwarmNode`. */
@Serializable
public data class SwarmNode(
    public val id: String,
    public val hostname: String,
    public val role: String,
    public val availability: String,
    public val status: String,
    public val address: String? = null,
    public val managerStatus: String? = null,
    public val reachability: String? = null,
    public val labels: Map<String, String>? = null,
    public val systemLabels: Map<String, String>? = null,
    public val engineVersion: String? = null,
    public val platform: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
    public val agent: SwarmNodeAgentStatus,
)

/** Request payload for updating a swarm node's spec. Mirrors Swift `SwarmNodeUpdateRequest`. */
@Serializable
public data class SwarmNodeUpdateRequest(
    public val version: ULong? = null,
    public val name: String? = null,
    public val labels: Map<String, String>? = null,
    public val role: String? = null,
    public val availability: String? = null,
)

/** Request for the swarm node agent deployment-snippet endpoint. Mirrors Swift `SwarmNodeAgentDeploymentRequest`. */
@Serializable
public data class SwarmNodeAgentDeploymentRequest(
    public val rotate: Boolean = false,
)

/**
 * File payload returned alongside deployment snippets (re-stated here so the swarm module is
 * self-contained even before the environments module is ported). Mirrors Swift `SwarmDeploymentSnippetFile`.
 */
@Serializable
public data class SwarmDeploymentSnippetFile(
    public val name: String,
    public val content: String? = null,
    public val downloadUrl: String? = null,
    public val sensitive: Boolean? = null,
    public val containerPath: String,
    public val permissions: String,
)

/** Mirrors Swift `SwarmDeploymentSnippetMTLS`. */
@Serializable
public data class SwarmDeploymentSnippetMTLS(
    public val dockerRun: String,
    public val dockerCompose: String,
    public val files: List<SwarmDeploymentSnippetFile> = emptyList(),
    public val hostDirHint: String,
)

/** Bundle of docker-run / docker-compose deployment snippets for a swarm node. Mirrors Swift `SwarmNodeAgentDeployment`. */
@Serializable
public data class SwarmNodeAgentDeployment(
    public val dockerRun: String,
    public val dockerCompose: String,
    public val mtls: SwarmDeploymentSnippetMTLS? = null,
    public val environmentId: String,
    public val agent: SwarmNodeAgentStatus,
)

/** Identifies the local swarm node (used by the cross-environment node identity endpoint). Mirrors Swift `SwarmNodeIdentity`. */
@Serializable
public data class SwarmNodeIdentity(
    public val swarmNodeId: String,
    public val hostname: String,
    public val role: String,
    public val engineVersion: String,
    public val swarmActive: Boolean,
)
