package app.getarcane.sdk.models.system

import kotlinx.serialization.Serializable

/** Resource statistics for a single GPU. */
@Serializable
public data class GPUStats(
    public val name: String,
    public val index: Int,
    public val memoryUsed: Double,
    public val memoryTotal: Double,
)

/**
 * System resource statistics for WebSocket streaming over `/environments/{id}/ws/system/stats`.
 * Byte counters are represented as [Long].
 */
@Serializable
public data class SystemStats(
    public val cpuUsage: Double,
    public val memoryUsage: Long,
    public val memoryTotal: Long,
    public val diskUsage: Long? = null,
    public val diskTotal: Long? = null,
    public val cpuCount: Int,
    public val architecture: String,
    public val platform: String,
    public val hostname: String? = null,
    public val gpuCount: Int,
    public val gpus: List<GPUStats>? = null,
)
