package app.getarcane.sdk.auth

import app.getarcane.sdk.ServerCapabilities
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.errors.fromResponse
import app.getarcane.sdk.http.ApiResponse
import app.getarcane.sdk.models.auth.LoginResponse
import app.getarcane.sdk.models.auth.RefreshRequest
import app.getarcane.sdk.models.auth.TokenRefreshResponse
import app.getarcane.sdk.models.user.User
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Thread-safe token lifecycle + capability cache: a [Mutex] guards mutable state and a shared lazy
 * [Deferred] de-duplicates concurrent refreshes, so N simultaneous 401s trigger exactly one
 * `auth/refresh`. X-API-Key takes precedence over the Bearer token. The refresh request uses a
 * dedicated [HttpClient] so it never recurses into the transport's auth-retry loop.
 */
public class AuthManager internal constructor(
    private val refreshClient: HttpClient,
    private val baseUrl: Url,
    private val tokenStore: TokenStore,
    private val apiKey: String?,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var cachedTokens: TokenPair? = null
    private var refreshJob: Deferred<TokenPair>? = null
    private var capabilities: ServerCapabilities = ServerCapabilities.UNKNOWN

    /** X-API-Key header (if configured), else a Bearer header from the cached access token, else empty. */
    public suspend fun authenticationHeaders(): Map<String, String> {
        if (!apiKey.isNullOrEmpty()) return mapOf("X-API-Key" to apiKey)
        val token = mutex.withLock { ensureLoadedLocked()?.accessToken }
        return if (token.isNullOrEmpty()) emptyMap() else mapOf("Authorization" to "Bearer $token")
    }

    /** Whether a non-empty refresh token is available (never true when using an API key). */
    public suspend fun hasRefreshCredential(): Boolean {
        if (apiKey != null) return false
        return mutex.withLock { ensureLoadedLocked()?.refreshToken?.isNotEmpty() == true }
    }

    public suspend fun save(loginResponse: LoginResponse) {
        save(TokenPair(loginResponse.token, loginResponse.refreshToken, loginResponse.expiresAt))
    }

    public suspend fun save(tokens: TokenPair) {
        mutex.withLock {
            cachedTokens = tokens
            tokenStore.saveTokens(tokens)
        }
    }

    public suspend fun clear() {
        mutex.withLock {
            cachedTokens = null
            refreshJob = null
            capabilities = ServerCapabilities.UNKNOWN
            tokenStore.clearTokens()
        }
    }

    public suspend fun currentCapabilities(): ServerCapabilities = mutex.withLock { capabilities }

    /** Records capabilities from a freshly decoded [User]. Ignores UNKNOWN so a stale signal never wins. */
    public suspend fun recordCapabilities(user: User) {
        mutex.withLock {
            val detected = ServerCapabilities.detect(user)
            if (detected != ServerCapabilities.Mode.UNKNOWN) {
                capabilities = ServerCapabilities(detected)
            }
        }
    }

    /**
     * Refreshes the access token, de-duplicating concurrent calls: the first caller creates the
     * in-flight [Deferred] and performs the post-success persistence; concurrent callers join the
     * same [Deferred]. On failure the auth state is cleared.
     */
    public suspend fun refreshTokens(): TokenPair {
        mutex.withLock { refreshJob }?.let { return it.await() }

        var isOwner = false
        val job = mutex.withLock {
            refreshJob ?: run {
                val refreshToken = ensureLoadedLocked()?.refreshToken
                if (refreshToken.isNullOrEmpty()) throw ArcaneError.Unauthorized
                isOwner = true
                scope.async(start = CoroutineStart.LAZY) { performRefresh(refreshToken) }
                    .also { refreshJob = it }
            }
        }
        if (!isOwner) return job.await()

        return try {
            val tokens = job.await()
            mutex.withLock {
                cachedTokens = tokens
                tokenStore.saveTokens(tokens)
                if (refreshJob === job) refreshJob = null
            }
            tokens
        } catch (t: Throwable) {
            mutex.withLock { if (refreshJob === job) refreshJob = null }
            if (t !is CancellationException) runCatching { clear() }
            throw t
        }
    }

    private suspend fun ensureLoadedLocked(): TokenPair? {
        if (cachedTokens == null) cachedTokens = tokenStore.loadTokens()
        return cachedTokens
    }

    private suspend fun performRefresh(refreshToken: String): TokenPair {
        val response = try {
            refreshClient.post(refreshUrl()) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ArcaneError) {
            throw e
        } catch (e: Throwable) {
            throw ArcaneError.Transport(e.message ?: e.toString())
        }

        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ArcaneError.fromResponse(response.status.value, bodyText, response.headers, json)
        }
        return try {
            val envelope = json.decodeFromString(ApiResponse.serializer(TokenRefreshResponse.serializer()), bodyText)
            TokenPair(envelope.data.token, envelope.data.refreshToken, envelope.data.expiresAt)
        } catch (e: Throwable) {
            throw ArcaneError.Decoding(e.message ?: e.toString())
        }
    }

    private fun refreshUrl(): String =
        URLBuilder(baseUrl).appendPathSegments("auth", "refresh").buildString()
}
