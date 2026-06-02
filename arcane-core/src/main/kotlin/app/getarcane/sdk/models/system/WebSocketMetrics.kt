package app.getarcane.sdk.models.system

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Stable identifiers for the different WebSocket connection kinds. */
@Serializable
public enum class WebSocketKind(public val wire: String) {
    @SerialName("project_logs")
    PROJECT_LOGS("project_logs"),

    @SerialName("container_logs")
    CONTAINER_LOGS("container_logs"),

    @SerialName("container_stats")
    CONTAINER_STATS("container_stats"),

    @SerialName("container_exec")
    CONTAINER_EXEC("container_exec"),

    @SerialName("system_stats")
    SYSTEM_STATS("system_stats"),

    @SerialName("service_logs")
    SERVICE_LOGS("service_logs"),
}

/** Description of a single active WebSocket connection on the server. */
@Serializable
public data class WebSocketConnectionInfo(
    public val id: String,
    public val kind: String,
    public val envId: String? = null,
    public val resourceId: String? = null,
    public val clientIp: String? = null,
    public val userId: String? = null,
    public val userAgent: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val startedAt: Instant,
)

/** Point-in-time snapshot of active WebSocket connection counts by kind. */
@Serializable
public data class WebSocketMetricsSnapshot(
    public val projectLogsActive: Long,
    public val containerLogsActive: Long,
    public val containerStats: Long,
    public val containerExec: Long,
    public val systemStats: Long,
    public val serviceLogsActive: Long,
)
