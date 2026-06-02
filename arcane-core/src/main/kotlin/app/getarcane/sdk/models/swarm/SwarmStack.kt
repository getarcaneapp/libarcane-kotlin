package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** A swarm stack as listed by the stacks endpoint. */
@Serializable
public data class SwarmStackSummary(
    public val id: String,
    public val name: String,
    public val namespace: String,
    public val services: Int,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/** A swarm stack inspect payload (no ID — stacks are keyed by name). */
@Serializable
public data class SwarmStackInspect(
    public val name: String,
    public val namespace: String,
    public val services: Int,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/**
 * A file synced alongside a swarm stack compose deployment; [content] holds the Base64-encoded
 * file bytes.
 */
@Serializable
public data class SwarmSyncFile(
    public val relativePath: String,
    public val content: String,
)

/** Body for `POST /environments/{id}/swarm/stacks` to deploy a stack. */
@Serializable
public data class SwarmStackDeployRequest(
    public val name: String,
    public val composeContent: String,
    public val envContent: String? = null,
    public val files: List<SwarmSyncFile>? = null,
    public val withRegistryAuth: Boolean? = null,
    public val prune: Boolean? = null,
    public val resolveImage: String? = null,
    public val workingDir: String? = null,
)

/** Result of `POST /environments/{id}/swarm/stacks`. */
@Serializable
public data class SwarmStackDeployResponse(
    public val name: String,
)

/** Persisted source for a deployed stack. */
@Serializable
public data class SwarmStackSource(
    public val name: String,
    public val composeContent: String,
    public val envContent: String? = null,
    public val files: List<SwarmSyncFile>? = null,
)

/** Update payload for the persisted stack source. */
@Serializable
public data class SwarmStackSourceUpdateRequest(
    public val composeContent: String,
    public val envContent: String? = null,
    public val files: List<SwarmSyncFile>? = null,
)

/** Render/validate request for a compose file before deploying. */
@Serializable
public data class SwarmStackRenderConfigRequest(
    public val name: String,
    public val composeContent: String,
    public val envContent: String? = null,
)

/** Result of rendering a compose config — normalized YAML plus referenced resources. */
@Serializable
public data class SwarmStackRenderConfigResponse(
    public val name: String,
    public val renderedCompose: String,
    public val services: List<String> = emptyList(),
    public val networks: List<String> = emptyList(),
    public val volumes: List<String> = emptyList(),
    public val configs: List<String> = emptyList(),
    public val secrets: List<String> = emptyList(),
    public val warnings: List<String>? = null,
)
