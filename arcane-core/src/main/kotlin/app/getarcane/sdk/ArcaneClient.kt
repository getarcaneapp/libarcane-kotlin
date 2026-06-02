package app.getarcane.sdk

import app.getarcane.sdk.auth.AuthManager
import app.getarcane.sdk.auth.AuthService
import app.getarcane.sdk.http.ArcaneTransport
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.normalizeBaseUrl
import app.getarcane.sdk.services.APIKeysService
import app.getarcane.sdk.services.BuildsService
import app.getarcane.sdk.services.ContainerRegistriesService
import app.getarcane.sdk.services.ContainersService
import app.getarcane.sdk.services.DashboardService
import app.getarcane.sdk.services.EnvironmentsService
import app.getarcane.sdk.services.EventsService
import app.getarcane.sdk.services.GitOpsService
import app.getarcane.sdk.services.ImagesService
import app.getarcane.sdk.services.JobsService
import app.getarcane.sdk.services.NetworksService
import app.getarcane.sdk.services.NotificationsService
import app.getarcane.sdk.services.OidcRoleMappingsService
import app.getarcane.sdk.services.PortsService
import app.getarcane.sdk.services.ProjectsService
import app.getarcane.sdk.services.RolesService
import app.getarcane.sdk.services.SettingsService
import app.getarcane.sdk.services.SwarmService
import app.getarcane.sdk.services.SystemService
import app.getarcane.sdk.services.TemplatesService
import app.getarcane.sdk.services.UpdaterService
import app.getarcane.sdk.services.UsersService
import app.getarcane.sdk.services.VersionService
import app.getarcane.sdk.services.VolumesService
import app.getarcane.sdk.services.VulnerabilitiesService
import app.getarcane.sdk.services.WebhooksService
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json

/**
 * The SDK entry point. Owns a single
 * Ktor [HttpClient], an [AuthManager], the [ArcaneTransport], the [RestService], and per-resource
 * services. Because the Kotlin client owns IO resources it is [AutoCloseable] (use `client.use { }`
 * or call [close]); [scoped] returns a view that shares the same client/auth.
 */
public class ArcaneClient private constructor(
    public val configuration: ArcaneConfiguration,
    sharedHttpClient: HttpClient?,
    sharedScope: CoroutineScope?,
    sharedAuthManager: AuthManager?,
) : AutoCloseable {
    public constructor(configuration: ArcaneConfiguration) : this(configuration, null, null, null)

    private val ownsResources: Boolean = sharedHttpClient == null
    private val baseUrl = normalizeBaseUrl(configuration.baseUrl)
    private val scope: CoroutineScope = sharedScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PublishedApi
    internal val httpClient: HttpClient = sharedHttpClient ?: buildHttpClient(
        engine = configuration.engine,
        json = configuration.json,
        logLevel = configuration.logLevel,
        requestTimeoutMillis = configuration.requestTimeoutMillis,
        connectTimeoutMillis = configuration.connectTimeoutMillis,
        defaultHeaders = configuration.defaultHeaders,
    )

    public val authManager: AuthManager = sharedAuthManager ?: AuthManager(
        refreshClient = httpClient,
        baseUrl = baseUrl,
        tokenStore = configuration.tokenStore,
        apiKey = configuration.apiKey,
        json = configuration.json,
        scope = scope,
    )

    public val transport: ArcaneTransport =
        ArcaneTransport(httpClient, baseUrl, authManager, configuration.retryPolicy, configuration.json)

    public val rest: RestService = RestService(transport, configuration.defaultEnvironmentId)

    public val auth: AuthService = AuthService(transport, authManager)
    public val users: UsersService = UsersService(rest)
    public val apiKeys: APIKeysService = APIKeysService(rest)
    public val roles: RolesService = RolesService(rest)
    public val oidcRoleMappings: OidcRoleMappingsService = OidcRoleMappingsService(rest)
    public val environments: EnvironmentsService = EnvironmentsService(rest)
    public val containers: ContainersService = ContainersService(rest)
    public val images: ImagesService = ImagesService(rest)
    public val volumes: VolumesService = VolumesService(rest)
    public val networks: NetworksService = NetworksService(rest)
    public val projects: ProjectsService = ProjectsService(rest)
    public val swarm: SwarmService = SwarmService(rest)
    public val system: SystemService = SystemService(rest)
    public val dashboard: DashboardService = DashboardService(rest)
    public val events: EventsService = EventsService(rest)
    public val webhooks: WebhooksService = WebhooksService(rest)
    public val notifications: NotificationsService = NotificationsService(rest)
    public val templates: TemplatesService = TemplatesService(rest)
    public val registries: ContainerRegistriesService = ContainerRegistriesService(rest)
    public val gitops: GitOpsService = GitOpsService(rest)
    public val builds: BuildsService = BuildsService(rest)
    public val jobs: JobsService = JobsService(rest)
    public val settings: SettingsService = SettingsService(rest)
    public val updater: UpdaterService = UpdaterService(rest)
    public val vulnerabilities: VulnerabilitiesService = VulnerabilitiesService(rest)
    public val ports: PortsService = PortsService(rest)
    public val version: VersionService = VersionService(rest)

    /** A client view scoped to a different default environment, sharing this client's HTTP + auth. */
    public fun scoped(toEnvironment: EnvironmentId): ArcaneClient =
        ArcaneClient(
            configuration.copy(defaultEnvironmentId = toEnvironment),
            httpClient,
            scope,
            authManager,
        )

    /** The detected server capabilities (v1 legacy vs v2 RBAC), populated after the first auth. */
    public suspend fun serverCapabilities(): ServerCapabilities = authManager.currentCapabilities()

    override fun close() {
        if (ownsResources) {
            httpClient.close()
            scope.cancel()
        }
    }
}

internal fun buildHttpClient(
    engine: HttpClientEngine,
    json: Json,
    logLevel: LogLevel,
    requestTimeoutMillis: Long,
    connectTimeoutMillis: Long,
    defaultHeaders: Map<String, String> = emptyMap(),
): HttpClient = HttpClient(engine) {
    // We map non-2xx to ArcaneError ourselves; don't let Ktor throw on them.
    expectSuccess = false

    install(ContentNegotiation) { json(json) }

    // Headers applied to every request (e.g. the hosted-demo session-id cookie).
    if (defaultHeaders.isNotEmpty()) {
        install(DefaultRequest) {
            defaultHeaders.forEach { (key, value) -> headers.append(key, value) }
        }
    }

    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        this.connectTimeoutMillis = connectTimeoutMillis
        // No socketTimeout: it would abort long-lived log/stats/NDJSON streams.
    }

    install(WebSockets)

    if (logLevel != LogLevel.NONE) {
        install(Logging) { level = logLevel }
    }
}
