package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** A swarm secret (named, write-only blob mountable into services). */
@Serializable
public data class SwarmSecretSummary(
    public val id: String,
    public val version: JsonValue,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
    public val spec: JsonValue,
)

/** Body for creating a swarm secret; [spec] is the raw Docker SecretSpec. */
@Serializable
public data class SwarmSecretCreateRequest(
    public val spec: JsonValue,
)

/** Body for updating a swarm secret. */
@Serializable
public data class SwarmSecretUpdateRequest(
    public val version: ULong? = null,
    public val spec: JsonValue,
)
