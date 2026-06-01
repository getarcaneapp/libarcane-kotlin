package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.system.ConvertDockerRunRequest
import app.getarcane.sdk.models.system.ConvertDockerRunResponse
import app.getarcane.sdk.models.system.DockerInfo
import app.getarcane.sdk.models.system.HealthResponse
import app.getarcane.sdk.models.system.PruneAllRequest
import app.getarcane.sdk.models.system.PruneAllResult
import app.getarcane.sdk.models.system.SystemContainerActionResult
import app.getarcane.sdk.models.system.SystemStats
import app.getarcane.sdk.models.system.UpgradeCheckResult
import app.getarcane.sdk.streaming.statsStream
import kotlinx.coroutines.flow.Flow

/** System-level endpoints under `/environments/{id}/system`. Port of Swift `SystemService`. */
public class SystemService internal constructor(private val rest: RestService) {
    // MARK: - Health / Info

    /**
     * Top-level API health probe (`GET /health`) — does not hit Docker. Returns the parsed
     * [HealthResponse] (`status == "UP"`) when the server responds with 2xx. Unauthenticated.
     */
    public suspend fun apiHealth(): HealthResponse =
        rest.transport.requestDecoded("health", method = "GET", authorized = false)

    /**
     * Pings the Docker daemon for a specific environment. Throws an `ArcaneError` if the daemon is
     * unreachable.
     */
    public suspend fun health(envId: EnvironmentId? = null) {
        rest.transport.rawRequestText(
            rest.environmentPath(envId, "system/health"),
            method = "HEAD",
            authorized = true,
        )
    }

    /**
     * Returns the Docker daemon version and system info. The response is the merged `dockerinfo.Info`
     * blob, returned without an `APIResponse` envelope.
     */
    public suspend fun dockerInfo(envId: EnvironmentId? = null): DockerInfo =
        rest.transport.requestDecoded(rest.environmentPath(envId, "system/docker/info"))

    // MARK: - Prune

    /** Removes unused Docker resources according to [options]. */
    public suspend fun prune(options: PruneAllRequest, envId: EnvironmentId? = null): PruneAllResult =
        rest.post(rest.environmentPath(envId, "system/prune"), body = options)

    // MARK: - Bulk container actions

    /** Starts every container (creating them where necessary). */
    public suspend fun startAllContainers(envId: EnvironmentId? = null): SystemContainerActionResult =
        rest.post(rest.environmentPath(envId, "system/containers/start-all"))

    /** Starts every stopped container. */
    public suspend fun startAllStoppedContainers(envId: EnvironmentId? = null): SystemContainerActionResult =
        rest.post(rest.environmentPath(envId, "system/containers/start-stopped"))

    /** Stops every running container. */
    public suspend fun stopAllContainers(envId: EnvironmentId? = null): SystemContainerActionResult =
        rest.post(rest.environmentPath(envId, "system/containers/stop-all"))

    // MARK: - Convert / Upgrade

    /**
     * Converts a `docker run ...` command-line into an equivalent compose document. The response is
     * returned without the standard envelope.
     */
    public suspend fun convertDockerRun(
        request: ConvertDockerRunRequest,
        envId: EnvironmentId? = null,
    ): ConvertDockerRunResponse =
        rest.transport.requestDecoded(
            rest.environmentPath(envId, "system/convert"),
            method = "POST",
            body = request,
            authorized = true,
        )

    /**
     * Checks whether Arcane can self-upgrade in this environment. The response is returned without
     * the standard envelope.
     */
    public suspend fun checkUpgrade(envId: EnvironmentId? = null): UpgradeCheckResult =
        rest.transport.requestDecoded(
            rest.environmentPath(envId, "system/upgrade/check"),
            method = "GET",
            authorized = true,
        )

    /**
     * Triggers a system upgrade. Returns once the upgrade has been scheduled (HTTP 202 is normal
     * here — the server is mid-replacement).
     */
    public suspend fun triggerUpgrade(envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "system/upgrade"))
    }

    // MARK: - WebSocket streams

    /** Streams live system stats over a WebSocket connection. */
    public fun statsStream(envId: EnvironmentId? = null): Flow<SystemStats> =
        rest.transport.statsStream(rest.environmentPath(envId, "ws/system/stats"), SystemStats.serializer())

    /** Download the manager's edge mTLS CA certificate (used by agents). */
    public suspend fun downloadEdgeMTLSCA(): ByteArray =
        rest.transport.downloadRaw("edge-mtls/ca", authorized = false)
}
