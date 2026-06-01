package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Runtime status reflecting whether swarm mode is enabled in this environment. Mirrors Swift `SwarmRuntimeStatus`. */
@Serializable
public data class SwarmRuntimeStatus(
    public val enabled: Boolean,
)

/**
 * Top-level information about the swarm cluster. The full Docker `Spec` blob is preserved as opaque
 * JSON. Mirrors Swift `SwarmInfo`.
 */
@Serializable
public data class SwarmInfo(
    public val id: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
    public val spec: JsonValue,
    public val rootRotationInProgress: Boolean,
)
