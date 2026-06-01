package app.getarcane.sdk

import app.getarcane.sdk.auth.InMemoryTokenStore
import app.getarcane.sdk.auth.TokenStore
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.serialization.json.Json

/**
 * Configuration for an [ArcaneClient]. Mirrors Swift `ArcaneClient.Configuration`. Swift's injectable
 * `URLSession` becomes a Ktor [HttpClientEngine] (default CIO; pass a `MockEngine` in tests), and the
 * `JSONDecoder`/`JSONEncoder` pair becomes a single [Json] instance.
 */
public data class ArcaneConfiguration(
    public val baseUrl: String,
    public val tokenStore: TokenStore = InMemoryTokenStore(),
    public val apiKey: String? = null,
    public val defaultEnvironmentId: EnvironmentId = EnvironmentId.LOCAL_DOCKER,
    public val engine: HttpClientEngine = CIO.create(),
    public val retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    public val logLevel: LogLevel = LogLevel.NONE,
    public val requestTimeoutMillis: Long = 30_000,
    public val connectTimeoutMillis: Long = 15_000,
    public val json: Json = ArcaneJson.default,
)
