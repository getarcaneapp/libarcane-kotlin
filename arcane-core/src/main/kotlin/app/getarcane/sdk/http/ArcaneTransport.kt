package app.getarcane.sdk.http

import app.getarcane.sdk.RetryPolicy
import app.getarcane.sdk.auth.AuthManager
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.errors.fromResponse
import app.getarcane.sdk.pagination.PaginatedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Low-level HTTP transport over a Ktor [HttpClient]. Port of Swift `ArcaneURLSessionTransport`
 * (Client/ArcaneURLSessionTransport.swift). The 401-refresh + idempotent-retry loop is implemented
 * manually (NOT via Ktor's Auth/HttpRequestRetry plugins) to reproduce Swift's exact ordering:
 * refresh once per request → retry → clear auth on a second 401.
 */
public class ArcaneTransport internal constructor(
    @PublishedApi internal val httpClient: HttpClient,
    internal val baseUrl: Url,
    internal val authManager: AuthManager,
    private val retryPolicy: RetryPolicy,
    @PublishedApi internal val json: Json,
) {
    /**
     * Executes a request and returns the raw response body bytes, applying auth headers, the
     * 401-refresh-once retry, the idempotent-method retry policy, and non-2xx → [ArcaneError]
     * mapping. Mirrors Swift `rawRequest`.
     */
    public suspend fun rawRequestBytes(
        path: String,
        method: String = "GET",
        query: List<Pair<String, String>> = emptyList(),
        body: Any? = null,
        authorized: Boolean = true,
    ): ByteArray {
        var didRefresh = false
        var attempt = 1
        val httpMethod = HttpMethod.parse(method)

        while (true) {
            val headers = if (authorized) authManager.authenticationHeaders() else emptyMap()
            val response = try {
                httpClient.request(buildUrl(path, query)) {
                    this.method = httpMethod
                    accept(ContentType.Application.Json)
                    headers.forEach { (key, value) -> header(key, value) }
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ArcaneError) {
                throw e
            } catch (e: Throwable) {
                if (isIdempotent(method) && attempt < retryPolicy.maxAttempts) {
                    sleepBeforeRetry(attempt)
                    attempt += 1
                    continue
                }
                throw ArcaneError.Transport(e.message ?: e.toString())
            }

            val status = response.status.value
            val bytes: ByteArray = response.body()

            if (status == 401 && authorized && !didRefresh && authManager.hasRefreshCredential()) {
                authManager.refreshTokens()
                didRefresh = true
                continue
            }

            if (shouldRetry(method, status) && attempt < retryPolicy.maxAttempts) {
                sleepBeforeRetry(attempt)
                attempt += 1
                continue
            }

            if (status !in 200..299) {
                if (status == 401) runCatching { authManager.clear() }
                throw ArcaneError.fromResponse(status, bytes.decodeToString(), response.headers, json)
            }
            return bytes
        }
    }

    /** [rawRequestBytes] decoded as UTF-8 text. */
    public suspend fun rawRequestText(
        path: String,
        method: String = "GET",
        query: List<Pair<String, String>> = emptyList(),
        body: Any? = null,
        authorized: Boolean = true,
    ): String = rawRequestBytes(path, method, query, body, authorized).decodeToString()

    /** Raw bytes of a GET response (binary downloads: mTLS bundles, backups, file contents). */
    public suspend fun downloadRaw(
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        authorized: Boolean = true,
    ): ByteArray = rawRequestBytes(path, "GET", query, null, authorized)

    /** Builds the absolute request [Url] for [path] (resolved against the `/api`-normalized base). */
    public fun buildUrl(path: String, query: List<Pair<String, String>> = emptyList()): Url =
        URLBuilder(baseUrl).apply {
            val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
            if (segments.isNotEmpty()) appendPathSegments(segments)
            query.forEach { (key, value) -> parameters.append(key, value) }
        }.build()

    /** Builds the WebSocket [Url] for [path], rewriting the scheme (https→wss, http→ws). */
    internal fun webSocketUrl(path: String, query: List<Pair<String, String>> = emptyList()): Url =
        URLBuilder(buildUrl(path, query)).apply {
            protocol = when (protocol) {
                URLProtocol.HTTPS -> URLProtocol.WSS
                URLProtocol.HTTP -> URLProtocol.WS
                else -> protocol
            }
        }.build()

    private fun shouldRetry(method: String, status: Int): Boolean =
        isIdempotent(method) && status in RETRYABLE_STATUSES

    private suspend fun sleepBeforeRetry(attempt: Int) {
        val multiplier = 1L shl maxOf(0, attempt - 1)
        val delayMillis = minOf(retryPolicy.baseBackoffMillis * multiplier, retryPolicy.maxBackoffMillis)
        delay(maxOf(delayMillis, 0))
    }

    private companion object {
        val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS", "PUT", "DELETE")
        val RETRYABLE_STATUSES = setOf(429, 502, 503, 504)
        fun isIdempotent(method: String): Boolean = method.uppercase() in IDEMPOTENT_METHODS
    }
}

/**
 * Decodes the standard `{ success, data }` envelope and returns `data`. Mirrors Swift
 * `transport.request<T>`.
 */
public suspend inline fun <reified T> ArcaneTransport.request(
    path: String,
    method: String = "GET",
    query: List<Pair<String, String>> = emptyList(),
    body: Any? = null,
    authorized: Boolean = true,
): T {
    val text = rawRequestText(path, method, query, body, authorized)
    return try {
        json.decodeFromString<ApiResponse<T>>(text).data
    } catch (e: SerializationException) {
        throw ArcaneError.Decoding(e.message ?: e.toString())
    } catch (e: IllegalArgumentException) {
        throw ArcaneError.Decoding(e.message ?: e.toString())
    }
}

/** Decodes the response body directly as [T] (no envelope). Used by OIDC and raw endpoints. */
public suspend inline fun <reified T> ArcaneTransport.requestDecoded(
    path: String,
    method: String = "GET",
    query: List<Pair<String, String>> = emptyList(),
    body: Any? = null,
    authorized: Boolean = true,
): T {
    val text = rawRequestText(path, method, query, body, authorized)
    return try {
        json.decodeFromString<T>(text)
    } catch (e: SerializationException) {
        throw ArcaneError.Decoding(e.message ?: e.toString())
    } catch (e: IllegalArgumentException) {
        throw ArcaneError.Decoding(e.message ?: e.toString())
    }
}

/** Decodes a `{ success, data, pagination }` page. Mirrors Swift `transport.paginated<T>`. */
public suspend inline fun <reified T> ArcaneTransport.paginated(
    path: String,
    start: Int,
    limit: Int,
    query: List<Pair<String, String>> = emptyList(),
): PaginatedResponse<T> {
    val withPaging = query + listOf("start" to maxOf(0, start).toString(), "limit" to limit.toString())
    val text = rawRequestText(path, "GET", withPaging, null, authorized = true)
    return try {
        json.decodeFromString<PaginatedResponse<T>>(text)
    } catch (e: SerializationException) {
        throw ArcaneError.Decoding(e.message ?: e.toString())
    } catch (e: IllegalArgumentException) {
        throw ArcaneError.Decoding(e.message ?: e.toString())
    }
}

/** Normalizes a base URL so its path ends in `/api` (without a trailing slash). */
internal fun normalizeBaseUrl(raw: String): Url {
    val builder = URLBuilder(raw)
    builder.parameters.clear()
    val segments = builder.pathSegments.filter { it.isNotEmpty() }
    builder.pathSegments = if (segments.lastOrNull() == "api") segments else segments + "api"
    return builder.build()
}
