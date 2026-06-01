package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.environment.AgentPairRequest
import app.getarcane.sdk.models.environment.AgentPairResponse
import app.getarcane.sdk.models.environment.CreateEnvironment
import app.getarcane.sdk.models.environment.DeploymentSnippet
import app.getarcane.sdk.models.environment.Environment
import app.getarcane.sdk.models.environment.EnvironmentTestResult
import app.getarcane.sdk.models.environment.EnvironmentVersion
import app.getarcane.sdk.models.environment.TestConnectionRequest
import app.getarcane.sdk.models.environment.UpdateEnvironment
import app.getarcane.sdk.pagination.PaginatedResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * CreateEnvironmentResult is the response payload returned by [EnvironmentsService.create]. It
 * contains the created environment plus an optional one-time API key. Mirrors Swift
 * `CreateEnvironmentResult`.
 *
 * The server returns a flat environment object that additionally carries `apiKey`; decoding
 * delegates to [Environment] and surfaces its `apiKey` here, matching Swift's `WireFormat` flattening.
 */
@Serializable(with = CreateEnvironmentResultSerializer::class)
public data class CreateEnvironmentResult(
    public val environment: Environment,
    public val apiKey: String? = null,
)

/**
 * Reproduces Swift `CreateEnvironmentResult.init(from:)`/`encode(to:)`: decode the flat payload as an
 * [Environment] (which already carries `apiKey`), exposing the same value on both fields; on encode,
 * merge [CreateEnvironmentResult.apiKey] back onto the environment and encode it flat.
 */
public object CreateEnvironmentResultSerializer : KSerializer<CreateEnvironmentResult> {
    private val envSer = Environment.serializer()

    override val descriptor: SerialDescriptor = envSer.descriptor

    override fun deserialize(decoder: Decoder): CreateEnvironmentResult {
        val env = decoder.decodeSerializableValue(envSer)
        return CreateEnvironmentResult(environment = env, apiKey = env.apiKey)
    }

    override fun serialize(encoder: Encoder, value: CreateEnvironmentResult) {
        val merged = value.environment.copy(apiKey = value.apiKey ?: value.environment.apiKey)
        encoder.encodeSerializableValue(envSer, merged)
    }
}

/**
 * Environment management endpoints registered under `/environments` along with agent pairing, mTLS,
 * and version helpers. Port of Swift `EnvironmentsService`.
 */
public class EnvironmentsService internal constructor(private val rest: RestService) {
    // MARK: - CRUD

    /** Paginated list of environments. */
    public suspend fun list(
        query: SearchPaginationSort = SearchPaginationSort(),
        type: String? = null,
    ): PaginatedResponse<Environment> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            type?.let { add("type" to it) }
        }
        return rest.transport.paginated("environments", query.start ?: 0, query.limit ?: 20, items)
    }

    /** Get an environment by ID. */
    public suspend fun get(id: EnvironmentId): Environment = rest.get("environments/${id.rawValue}")

    /**
     * Create a new environment. The response may include a one-time API key when
     * [CreateEnvironment.useApiKey] is set to `true`.
     */
    public suspend fun create(request: CreateEnvironment): CreateEnvironmentResult =
        rest.post("environments", body = request)

    /** Update an environment. */
    public suspend fun update(id: EnvironmentId, request: UpdateEnvironment): Environment =
        rest.put("environments/${id.rawValue}", body = request)

    /** Delete an environment. The local environment cannot be deleted. */
    public suspend fun delete(id: EnvironmentId) {
        rest.deleteVoid("environments/${id.rawValue}")
    }

    // MARK: - Status / Connectivity

    /** Test connectivity to an environment, optionally overriding the URL. */
    public suspend fun testConnection(
        id: EnvironmentId,
        request: TestConnectionRequest? = null,
    ): EnvironmentTestResult = rest.post("environments/${id.rawValue}/test", body = request)

    /** Update the heartbeat timestamp for an environment. */
    public suspend fun updateHeartbeat(id: EnvironmentId) {
        rest.postVoid("environments/${id.rawValue}/heartbeat")
    }

    /** Sync container registries and git repositories to an environment. */
    public suspend fun sync(id: EnvironmentId) {
        rest.postVoid("environments/${id.rawValue}/sync")
    }

    /** Get the version of a remote environment. */
    public suspend fun version(id: EnvironmentId): EnvironmentVersion =
        rest.get("environments/${id.rawValue}/version")

    // MARK: - Agent pairing

    /** Pair the manager with the local agent, optionally rotating the token. */
    public suspend fun pairAgent(
        id: EnvironmentId = EnvironmentId.LOCAL_DOCKER,
        request: AgentPairRequest? = null,
    ): AgentPairResponse = rest.post("environments/${id.rawValue}/agent/pair", body = request)

    /**
     * Complete the agent-to-manager pairing handshake by submitting the API key issued during
     * environment creation. The agent sends this request with no Authorization header; the API key
     * header is set by the caller.
     */
    public suspend fun pairEnvironment() {
        rest.postVoid("environments/pair")
    }

    // MARK: - Deployment snippets / mTLS

    /** Get the deployment snippets (docker run / compose / optional mTLS) for an environment. */
    public suspend fun deploymentSnippets(id: EnvironmentId): DeploymentSnippet =
        rest.get("environments/${id.rawValue}/deployment")

    /** Download the mTLS certificate bundle (zip) for an environment. */
    public suspend fun downloadMTLSBundle(id: EnvironmentId): ByteArray =
        rest.transport.downloadRaw("environments/${id.rawValue}/deployment/mtls/bundle")

    /** Download a single mTLS file (PEM/key/etc.) for an environment. */
    public suspend fun downloadMTLSFile(id: EnvironmentId, fileName: String): ByteArray =
        rest.transport.downloadRaw("environments/${id.rawValue}/deployment/mtls/$fileName")
}
