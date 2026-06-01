package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.swarm.SwarmConfigCreateRequest
import app.getarcane.sdk.models.swarm.SwarmConfigSummary
import app.getarcane.sdk.models.swarm.SwarmConfigUpdateRequest
import app.getarcane.sdk.models.swarm.SwarmInfo
import app.getarcane.sdk.models.swarm.SwarmInitRequest
import app.getarcane.sdk.models.swarm.SwarmInitResponse
import app.getarcane.sdk.models.swarm.SwarmJoinRequest
import app.getarcane.sdk.models.swarm.SwarmJoinTokens
import app.getarcane.sdk.models.swarm.SwarmLeaveRequest
import app.getarcane.sdk.models.swarm.SwarmNode
import app.getarcane.sdk.models.swarm.SwarmNodeAgentDeployment
import app.getarcane.sdk.models.swarm.SwarmNodeAgentDeploymentRequest
import app.getarcane.sdk.models.swarm.SwarmNodeIdentity
import app.getarcane.sdk.models.swarm.SwarmNodeUpdateRequest
import app.getarcane.sdk.models.swarm.SwarmRotateJoinTokensRequest
import app.getarcane.sdk.models.swarm.SwarmRuntimeStatus
import app.getarcane.sdk.models.swarm.SwarmSecretCreateRequest
import app.getarcane.sdk.models.swarm.SwarmSecretSummary
import app.getarcane.sdk.models.swarm.SwarmSecretUpdateRequest
import app.getarcane.sdk.models.swarm.SwarmServiceCreateRequest
import app.getarcane.sdk.models.swarm.SwarmServiceCreateResponse
import app.getarcane.sdk.models.swarm.SwarmServiceInspect
import app.getarcane.sdk.models.swarm.SwarmServiceScaleRequest
import app.getarcane.sdk.models.swarm.SwarmServiceSummary
import app.getarcane.sdk.models.swarm.SwarmServiceUpdateRequest
import app.getarcane.sdk.models.swarm.SwarmServiceUpdateResponse
import app.getarcane.sdk.models.swarm.SwarmStackDeployRequest
import app.getarcane.sdk.models.swarm.SwarmStackDeployResponse
import app.getarcane.sdk.models.swarm.SwarmStackInspect
import app.getarcane.sdk.models.swarm.SwarmStackRenderConfigRequest
import app.getarcane.sdk.models.swarm.SwarmStackRenderConfigResponse
import app.getarcane.sdk.models.swarm.SwarmStackSource
import app.getarcane.sdk.models.swarm.SwarmStackSourceUpdateRequest
import app.getarcane.sdk.models.swarm.SwarmStackSummary
import app.getarcane.sdk.models.swarm.SwarmTaskSummary
import app.getarcane.sdk.models.swarm.SwarmUnlockKeyResponse
import app.getarcane.sdk.models.swarm.SwarmUnlockRequest
import app.getarcane.sdk.models.swarm.SwarmUpdateRequest
import app.getarcane.sdk.pagination.PaginatedResponse
import app.getarcane.sdk.streaming.LogLine
import app.getarcane.sdk.streaming.logStream
import kotlinx.coroutines.flow.Flow

/**
 * Facade for the Docker Swarm endpoints under `/environments/{id}/swarm`. Port of Swift `SwarmService`.
 *
 * This class collides on name with the Docker concept of a "swarm service" (a deployed service
 * object), which is modeled as `SwarmServiceSummary` / `SwarmServiceInspect` to keep things
 * unambiguous.
 */
public class SwarmService internal constructor(private val rest: RestService) {
    // MARK: - Status / Info / Spec

    /** Returns whether swarm mode is enabled in this environment. */
    public suspend fun status(envId: EnvironmentId? = null): SwarmRuntimeStatus =
        rest.get(rest.environmentPath(envId, "swarm/status"))

    /** Returns the top-level swarm metadata. */
    public suspend fun info(envId: EnvironmentId? = null): SwarmInfo =
        rest.get(rest.environmentPath(envId, "swarm/info"))

    /** Updates the swarm-level spec. */
    public suspend fun updateSpec(request: SwarmUpdateRequest, envId: EnvironmentId? = null) {
        rest.putVoid(rest.environmentPath(envId, "swarm/spec"), body = request)
    }

    // MARK: - Lifecycle

    /** Initializes a brand-new swarm cluster on this environment's Docker engine. */
    public suspend fun initSwarm(request: SwarmInitRequest, envId: EnvironmentId? = null): SwarmInitResponse =
        rest.post(rest.environmentPath(envId, "swarm/init"), body = request)

    /** Joins this environment's Docker engine to an existing swarm. */
    public suspend fun join(request: SwarmJoinRequest, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "swarm/join"), body = request)
    }

    /** Removes this environment's Docker engine from the swarm. */
    public suspend fun leave(force: Boolean = false, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "swarm/leave"), body = SwarmLeaveRequest(force = force))
    }

    /** Unlocks the swarm using the provided unlock key. */
    public suspend fun unlock(key: String, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "swarm/unlock"), body = SwarmUnlockRequest(key = key))
    }

    /** Returns the swarm unlock key (requires admin). */
    public suspend fun unlockKey(envId: EnvironmentId? = null): SwarmUnlockKeyResponse =
        rest.get(rest.environmentPath(envId, "swarm/unlock-key"))

    /**
     * Returns the local swarm node identity (used for cross-environment matching). This endpoint
     * sits *outside* the environments tree.
     */
    public suspend fun localNodeIdentity(): SwarmNodeIdentity =
        rest.get("swarm/node-identity")

    // MARK: - Join tokens

    /** Returns the worker / manager join tokens for the swarm. */
    public suspend fun joinTokens(envId: EnvironmentId? = null): SwarmJoinTokens =
        rest.get(rest.environmentPath(envId, "swarm/join-tokens"))

    /** Rotates the swarm join tokens. */
    public suspend fun rotateJoinTokens(request: SwarmRotateJoinTokensRequest, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "swarm/join-tokens/rotate"), body = request)
    }

    // MARK: - Services

    /** Lists swarm services with offset-based pagination. */
    public suspend fun listServices(
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmServiceSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/services"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    /** Inspects a single swarm service. */
    public suspend fun service(serviceId: String, envId: EnvironmentId? = null): SwarmServiceInspect =
        rest.get(rest.environmentPath(envId, "swarm/services/$serviceId"))

    /** Creates a new swarm service. */
    public suspend fun createService(
        request: SwarmServiceCreateRequest,
        envId: EnvironmentId? = null,
    ): SwarmServiceCreateResponse =
        rest.post(rest.environmentPath(envId, "swarm/services"), body = request)

    /** Updates an existing swarm service. */
    public suspend fun updateService(
        serviceId: String,
        request: SwarmServiceUpdateRequest,
        envId: EnvironmentId? = null,
    ): SwarmServiceUpdateResponse =
        rest.put(rest.environmentPath(envId, "swarm/services/$serviceId"), body = request)

    /** Deletes a swarm service. */
    public suspend fun deleteService(serviceId: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "swarm/services/$serviceId"))
    }

    /** Lists the tasks belonging to a swarm service. */
    public suspend fun serviceTasks(
        serviceId: String,
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmTaskSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/services/$serviceId/tasks"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    /** Rolls a swarm service back to its previous spec. */
    public suspend fun rollbackService(serviceId: String, envId: EnvironmentId? = null): SwarmServiceUpdateResponse =
        rest.post(rest.environmentPath(envId, "swarm/services/$serviceId/rollback"))

    /** Scales a replicated swarm service. */
    public suspend fun scaleService(
        serviceId: String,
        replicas: ULong,
        envId: EnvironmentId? = null,
    ): SwarmServiceUpdateResponse =
        rest.post(
            rest.environmentPath(envId, "swarm/services/$serviceId/scale"),
            body = SwarmServiceScaleRequest(replicas = replicas),
        )

    // MARK: - Nodes

    /** Lists swarm nodes. */
    public suspend fun listNodes(
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmNode> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/nodes"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    /** Returns a single swarm node. */
    public suspend fun node(nodeId: String, envId: EnvironmentId? = null): SwarmNode =
        rest.get(rest.environmentPath(envId, "swarm/nodes/$nodeId"))

    /** Returns the node-agent deployment snippets for a swarm node. */
    public suspend fun nodeAgentDeployment(
        nodeId: String,
        rotate: Boolean = false,
        envId: EnvironmentId? = null,
    ): SwarmNodeAgentDeployment =
        rest.post(
            rest.environmentPath(envId, "swarm/nodes/$nodeId/agent/deployment"),
            body = SwarmNodeAgentDeploymentRequest(rotate = rotate),
        )

    /** Updates a swarm node (labels / role / availability). */
    public suspend fun updateNode(nodeId: String, request: SwarmNodeUpdateRequest, envId: EnvironmentId? = null) {
        rest.patch<MessageResponse>(rest.environmentPath(envId, "swarm/nodes/$nodeId"), body = request)
    }

    /** Deletes a swarm node, optionally forcing removal. */
    public suspend fun deleteNode(nodeId: String, force: Boolean = false, envId: EnvironmentId? = null) {
        rest.deleteVoid(
            rest.environmentPath(envId, "swarm/nodes/$nodeId"),
            query = listOf("force" to force.toString()),
        )
    }

    /** Promotes a worker to manager. */
    public suspend fun promoteNode(nodeId: String, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "swarm/nodes/$nodeId/promote"))
    }

    /** Demotes a manager to worker. */
    public suspend fun demoteNode(nodeId: String, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "swarm/nodes/$nodeId/demote"))
    }

    /** Lists tasks running on a swarm node. */
    public suspend fun nodeTasks(
        nodeId: String,
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmTaskSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/nodes/$nodeId/tasks"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    // MARK: - Tasks

    /** Lists every task in the swarm. */
    public suspend fun listTasks(
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmTaskSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/tasks"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    // MARK: - Stacks

    /** Lists stacks deployed to the swarm. */
    public suspend fun listStacks(
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmStackSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/stacks"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    /** Deploys (creates or updates) a stack from a compose document. */
    public suspend fun deployStack(request: SwarmStackDeployRequest, envId: EnvironmentId? = null): SwarmStackDeployResponse =
        rest.post(rest.environmentPath(envId, "swarm/stacks"), body = request)

    /** Inspects a single stack by name. */
    public suspend fun stack(name: String, envId: EnvironmentId? = null): SwarmStackInspect =
        rest.get(rest.environmentPath(envId, "swarm/stacks/$name"))

    /** Returns the persisted compose source for a stack. */
    public suspend fun stackSource(name: String, envId: EnvironmentId? = null): SwarmStackSource =
        rest.get(rest.environmentPath(envId, "swarm/stacks/$name/source"))

    /** Updates the persisted compose source for a stack. */
    public suspend fun updateStackSource(
        name: String,
        request: SwarmStackSourceUpdateRequest,
        envId: EnvironmentId? = null,
    ): SwarmStackSource =
        rest.put(rest.environmentPath(envId, "swarm/stacks/$name/source"), body = request)

    /** Removes a deployed stack. */
    public suspend fun deleteStack(name: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "swarm/stacks/$name"))
    }

    /** Lists the services that belong to a stack. */
    public suspend fun stackServices(
        name: String,
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmServiceSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/stacks/$name/services"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    /** Lists the tasks that belong to a stack. */
    public suspend fun stackTasks(
        name: String,
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<SwarmTaskSummary> =
        rest.transport.paginated(
            rest.environmentPath(envId, "swarm/stacks/$name/tasks"),
            start,
            limit,
            startLimitQuery(search, sort, order),
        )

    /** Renders / validates a compose document without deploying it. */
    public suspend fun renderStackConfig(
        request: SwarmStackRenderConfigRequest,
        envId: EnvironmentId? = null,
    ): SwarmStackRenderConfigResponse =
        rest.post(rest.environmentPath(envId, "swarm/stacks/config/render"), body = request)

    // MARK: - Configs

    /** Lists swarm configs. */
    public suspend fun listConfigs(envId: EnvironmentId? = null): List<SwarmConfigSummary> =
        rest.get(rest.environmentPath(envId, "swarm/configs"))

    /** Inspects a single swarm config. */
    public suspend fun config(configId: String, envId: EnvironmentId? = null): SwarmConfigSummary =
        rest.get(rest.environmentPath(envId, "swarm/configs/$configId"))

    /** Creates a new swarm config. */
    public suspend fun createConfig(request: SwarmConfigCreateRequest, envId: EnvironmentId? = null): SwarmConfigSummary =
        rest.post(rest.environmentPath(envId, "swarm/configs"), body = request)

    /** Updates a swarm config. */
    public suspend fun updateConfig(
        configId: String,
        request: SwarmConfigUpdateRequest,
        envId: EnvironmentId? = null,
    ): SwarmConfigSummary =
        rest.put(rest.environmentPath(envId, "swarm/configs/$configId"), body = request)

    /** Deletes a swarm config. */
    public suspend fun deleteConfig(configId: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "swarm/configs/$configId"))
    }

    // MARK: - Secrets

    /** Lists swarm secrets. */
    public suspend fun listSecrets(envId: EnvironmentId? = null): List<SwarmSecretSummary> =
        rest.get(rest.environmentPath(envId, "swarm/secrets"))

    /** Inspects a single swarm secret. */
    public suspend fun secret(secretId: String, envId: EnvironmentId? = null): SwarmSecretSummary =
        rest.get(rest.environmentPath(envId, "swarm/secrets/$secretId"))

    /** Creates a new swarm secret. */
    public suspend fun createSecret(request: SwarmSecretCreateRequest, envId: EnvironmentId? = null): SwarmSecretSummary =
        rest.post(rest.environmentPath(envId, "swarm/secrets"), body = request)

    /** Updates a swarm secret. */
    public suspend fun updateSecret(
        secretId: String,
        request: SwarmSecretUpdateRequest,
        envId: EnvironmentId? = null,
    ): SwarmSecretSummary =
        rest.put(rest.environmentPath(envId, "swarm/secrets/$secretId"), body = request)

    /** Deletes a swarm secret. */
    public suspend fun deleteSecret(secretId: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "swarm/secrets/$secretId"))
    }

    // MARK: - WebSocket streams

    /** Streams logs for a swarm service over a WebSocket connection. */
    public fun serviceLogs(
        serviceId: String,
        envId: EnvironmentId? = null,
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
        return rest.transport.logStream("environments/$env/ws/swarm/services/$serviceId/logs", query)
    }

    // MARK: - Helpers

    private fun startLimitQuery(
        search: String?,
        sort: String?,
        order: SortOrder?,
    ): List<Pair<String, String>> = buildList {
        search?.let { add("search" to it) }
        sort?.let { add("sort" to it) }
        order?.let { add("order" to it.wire) }
    }
}
