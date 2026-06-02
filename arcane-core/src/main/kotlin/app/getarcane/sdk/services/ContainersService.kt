package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.container.ContainerCreate
import app.getarcane.sdk.models.container.ContainerCreated
import app.getarcane.sdk.models.container.ContainerDetails
import app.getarcane.sdk.models.container.ContainerListResponse
import app.getarcane.sdk.models.container.ContainerStatsPayload
import app.getarcane.sdk.models.container.ContainerStatusCounts
import app.getarcane.sdk.streaming.LogLine
import app.getarcane.sdk.streaming.TerminalSession
import app.getarcane.sdk.streaming.logStream
import app.getarcane.sdk.streaming.statsStream
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
private data class AutoUpdateBody(val enabled: Boolean)

/** Container lifecycle, logs, stats, and exec. */
public class ContainersService internal constructor(private val rest: RestService) {
    public suspend fun list(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        groupBy: String? = null,
        includeInternal: Boolean? = null,
        updates: String? = null,
        standalone: String? = null,
    ): ContainerListResponse {
        val items = buildList {
            addAll(query.queryItems)
            groupBy?.let { add("groupBy" to it) }
            includeInternal?.let { add("includeInternal" to it.toString()) }
            updates?.let { add("updates" to it) }
            standalone?.let { add("standalone" to it) }
        }
        // The list endpoint returns the ContainerListResponse envelope directly
        // ({success, data:[...], counts, pagination}) — NOT wrapped in an outer ApiResponse —
        // so decode it as-is rather than unwrapping ApiResponse<T>.data.
        return rest.transport.requestDecoded(rest.environmentPath(envId, "containers"), query = items)
    }

    public suspend fun statusCounts(
        envId: EnvironmentId? = null,
        includeInternal: Boolean? = null,
    ): ContainerStatusCounts {
        val items = buildList { includeInternal?.let { add("includeInternal" to it.toString()) } }
        return rest.get(rest.environmentPath(envId, "containers/counts"), items)
    }

    public suspend fun inspect(envId: EnvironmentId? = null, id: String): ContainerDetails =
        rest.get(rest.environmentPath(envId, "containers/$id"))

    public suspend fun create(envId: EnvironmentId? = null, body: ContainerCreate): ContainerCreated =
        rest.post(rest.environmentPath(envId, "containers"), body = body)

    public suspend fun delete(
        envId: EnvironmentId? = null,
        id: String,
        force: Boolean = false,
        removeVolumes: Boolean = false,
    ) {
        rest.deleteVoid(
            rest.environmentPath(envId, "containers/$id"),
            query = listOf("force" to force.toString(), "volumes" to removeVolumes.toString()),
        )
    }

    public suspend fun start(envId: EnvironmentId? = null, id: String) {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/start"))
    }

    public suspend fun stop(envId: EnvironmentId? = null, id: String) {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/stop"))
    }

    public suspend fun restart(envId: EnvironmentId? = null, id: String) {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/restart"))
    }

    public suspend fun redeploy(envId: EnvironmentId? = null, id: String): ContainerDetails =
        rest.post(rest.environmentPath(envId, "containers/$id/redeploy"))

    public suspend fun pause(envId: EnvironmentId? = null, id: String) {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/pause"))
    }

    public suspend fun unpause(envId: EnvironmentId? = null, id: String) {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/unpause"))
    }

    public suspend fun kill(envId: EnvironmentId? = null, id: String, signal: String = "SIGKILL") {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/kill"), query = listOf("signal" to signal))
    }

    public suspend fun rename(envId: EnvironmentId? = null, id: String, newName: String) {
        rest.postVoid(rest.environmentPath(envId, "containers/$id/rename"), query = listOf("name" to newName))
    }

    public suspend fun setAutoUpdate(envId: EnvironmentId? = null, id: String, enabled: Boolean) {
        rest.putVoid(rest.environmentPath(envId, "containers/$id/auto-update"), body = AutoUpdateBody(enabled))
    }

    /** Stream container log lines over a WebSocket. */
    public fun logs(
        envId: EnvironmentId? = null,
        id: String,
        follow: Boolean = true,
        tail: String = "100",
        since: String? = null,
        timestamps: Boolean = false,
    ): Flow<LogLine> {
        val env = (envId ?: rest.defaultEnvironmentId).rawValue
        val query = buildList {
            add("follow" to follow.toString())
            add("tail" to tail)
            add("timestamps" to timestamps.toString())
            since?.let { add("since" to it) }
        }
        return rest.transport.logStream("environments/$env/ws/containers/$id/logs", query)
    }

    /** Stream container resource usage stats over a WebSocket. */
    public fun stats(envId: EnvironmentId? = null, id: String): Flow<ContainerStatsPayload> {
        val env = (envId ?: rest.defaultEnvironmentId).rawValue
        return rest.transport.statsStream(
            "environments/$env/ws/containers/$id/stats",
            ContainerStatsPayload.serializer(),
        )
    }

    /** Open an interactive terminal against a running container. */
    public suspend fun exec(
        envId: EnvironmentId? = null,
        id: String,
        shell: String = "/bin/sh",
    ): TerminalSession {
        val env = (envId ?: rest.defaultEnvironmentId).rawValue
        return TerminalSession.connect(rest.transport, "environments/$env/ws/containers/$id/terminal", shell)
    }
}
