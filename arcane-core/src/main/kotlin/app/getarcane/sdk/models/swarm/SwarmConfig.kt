package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** A swarm config (named blob mountable into services). */
@Serializable
public data class SwarmConfigSummary(
    public val id: String,
    public val version: JsonValue,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
    public val spec: JsonValue,
)

/** Body for creating a swarm config; [spec] is the raw Docker ConfigSpec. */
@Serializable
public data class SwarmConfigCreateRequest(
    public val spec: JsonValue,
)

/** Body for updating a swarm config. */
@Serializable
public data class SwarmConfigUpdateRequest(
    public val version: ULong? = null,
    public val spec: JsonValue,
)
