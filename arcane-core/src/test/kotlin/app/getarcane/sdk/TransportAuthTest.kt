package app.getarcane.sdk

import app.getarcane.sdk.auth.InMemoryTokenStore
import app.getarcane.sdk.auth.TokenPair
import app.getarcane.sdk.auth.TokenStore
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.models.user.isGlobalAdmin
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** MockEngine-backed tests for the transport + auth-refresh semantics (Phase C/D). */
class TransportAuthTest {
    private val future = Instant.parse("2030-01-01T00:00:00Z")
    private val okMessage = """{"success":true,"data":{"message":"ok"}}"""
    private fun refreshBody(token: String) =
        """{"success":true,"data":{"token":"$token","refreshToken":"r2","expiresAt":"2030-01-01T00:00:00Z"}}"""

    private fun MockRequestHandleScope.jsonOk(body: String): HttpResponseData =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.jsonStatus(status: HttpStatusCode, body: String = "{}"): HttpResponseData =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun client(
        apiKey: String? = null,
        tokenStore: TokenStore = InMemoryTokenStore(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): Pair<ArcaneClient, MutableList<HttpRequestData>> {
        val recorded = mutableListOf<HttpRequestData>()
        val engine = MockEngine { req ->
            recorded.add(req)
            handler(req)
        }
        val client = ArcaneClient(
            ArcaneConfiguration(
                baseUrl = "https://test.local",
                engine = engine,
                apiKey = apiKey,
                tokenStore = tokenStore,
                retryPolicy = RetryPolicy(maxAttempts = 3, baseBackoffMillis = 1, maxBackoffMillis = 2),
            ),
        )
        return client to recorded
    }

    @Test
    fun injectsApiPathAndBearer() = runTest {
        val store = InMemoryTokenStore(TokenPair("acc", "ref", future))
        val (c, recorded) = client(tokenStore = store) { jsonOk(okMessage) }
        c.use { it.rest.get<MessageResponse>("environments/0/containers/counts") }
        assertEquals("/api/environments/0/containers/counts", recorded[0].url.encodedPath)
        assertEquals("Bearer acc", recorded[0].headers[HttpHeaders.Authorization])
    }

    @Test
    fun apiKeyTakesPrecedenceOverBearer() = runTest {
        val store = InMemoryTokenStore(TokenPair("acc", "ref", future))
        val (c, recorded) = client(apiKey = "KEY", tokenStore = store) { jsonOk(okMessage) }
        c.use { it.rest.get<MessageResponse>("version") }
        assertEquals("KEY", recorded[0].headers["X-API-Key"])
        assertNull(recorded[0].headers[HttpHeaders.Authorization])
    }

    @Test
    fun refreshesOnceThenRetries() = runTest {
        val store = InMemoryTokenStore(TokenPair("old", "refresh-token", future))
        val (c, recorded) = client(tokenStore = store) { req ->
            when {
                req.url.encodedPath.endsWith("/auth/refresh") -> jsonOk(refreshBody("new"))
                req.headers[HttpHeaders.Authorization] == "Bearer old" ->
                    jsonStatus(HttpStatusCode.Unauthorized, """{"error":"expired"}""")
                else -> jsonOk(okMessage)
            }
        }
        val result = c.use { it.rest.get<MessageResponse>("ping") }
        assertEquals("ok", result.message)
        assertEquals(1, recorded.count { it.url.encodedPath.endsWith("/auth/refresh") })
        assertEquals("Bearer new", recorded.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun secondUnauthorizedClearsTokensAndThrows() = runTest {
        val store = InMemoryTokenStore(TokenPair("old", "refresh-token", future))
        val (c, _) = client(tokenStore = store) { req ->
            if (req.url.encodedPath.endsWith("/auth/refresh")) jsonOk(refreshBody("new"))
            else jsonStatus(HttpStatusCode.Unauthorized, """{"error":"nope"}""")
        }
        c.use {
            assertFailsWith<ArcaneError.Unauthorized> { it.rest.get<MessageResponse>("ping") }
        }
        assertNull(store.loadTokens())
    }

    @Test
    fun concurrentRefreshDeduplicates() = runTest {
        val store = InMemoryTokenStore(TokenPair("old", "refresh-token", future))
        val (c, recorded) = client(tokenStore = store) { req ->
            when {
                req.url.encodedPath.endsWith("/auth/refresh") -> {
                    delay(50) // hold the refresh in-flight so concurrent callers join the same Deferred
                    jsonOk(refreshBody("new"))
                }
                req.headers[HttpHeaders.Authorization] == "Bearer old" ->
                    jsonStatus(HttpStatusCode.Unauthorized, """{"error":"expired"}""")
                else -> jsonOk(okMessage)
            }
        }
        c.use { client ->
            val results = (1..5).map { async { client.rest.get<MessageResponse>("ping") } }.awaitAll()
            assertEquals(5, results.size)
            assertTrue(results.all { it.message == "ok" })
        }
        assertEquals(1, recorded.count { it.url.encodedPath.endsWith("/auth/refresh") })
    }

    @Test
    fun retriesIdempotentButNotPost() = runTest {
        var getCount = 0
        var postCount = 0
        val (c, _) = client { req ->
            if (req.method == HttpMethod.Post) postCount++ else getCount++
            jsonStatus(HttpStatusCode.ServiceUnavailable, """{"error":"down"}""")
        }
        c.use {
            assertFailsWith<ArcaneError.Server> { it.rest.get<MessageResponse>("ping") }
            assertFailsWith<ArcaneError.Server> { it.rest.postVoid("ping") }
        }
        assertEquals(3, getCount) // retried up to maxAttempts
        assertEquals(1, postCount) // POST is not retried
    }

    @Test
    fun loginSavesTokensAndDetectsRbac() = runTest {
        val store = InMemoryTokenStore()
        val (c, _) = client(tokenStore = store) {
            jsonOk(
                """
                {"success":true,"data":{
                  "token":"t","refreshToken":"r","expiresAt":"2030-01-01T00:00:00Z",
                  "user":{"id":"u1","username":"admin","permissionsByEnv":{"global":["*"]}}
                }}
                """.trimIndent(),
            )
        }
        c.use {
            val resp = it.auth.login("admin", "pw")
            assertEquals("u1", resp.user.id)
            assertTrue(resp.user.isGlobalAdmin)
            assertEquals(ServerCapabilities.Mode.RBAC, it.serverCapabilities().mode)
        }
        assertEquals("t", store.loadTokens()?.accessToken)
    }

    @Test
    fun publicUnauthorizedDoesNotClearActiveSession() = runTest {
        val tokens = TokenPair("active", "refresh", future)
        val store = InMemoryTokenStore(tokens)
        val (client, _) = client(tokenStore = store) {
            jsonStatus(HttpStatusCode.Unauthorized, """{"error":"unauthorized"}""")
        }
        client.use {
            assertFailsWith<ArcaneError.Unauthorized> {
                it.transport.rawRequestBytes("settings/public", authorized = false)
            }
        }
        assertEquals(tokens, store.loadTokens())
    }

    @Test
    fun offlineLogoutStillClearsLocalCredentials() = runTest {
        val store = InMemoryTokenStore(TokenPair("active", "refresh", future))
        val (client, _) = client(tokenStore = store) { throw IOException("offline") }
        client.use { assertFailsWith<ArcaneError.Transport> { it.auth.logout() } }
        assertNull(store.loadTokens())
    }

    @Test
    fun transientProactiveRefreshFailureKeepsStoredTokens() = runTest {
        val expired = TokenPair("stale", "refresh", Instant.parse("2020-01-01T00:00:00Z"))
        val store = InMemoryTokenStore(expired)
        val (client, recorded) = client(tokenStore = store) { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                jsonStatus(HttpStatusCode.ServiceUnavailable, """{"error":"offline"}""")
            } else {
                jsonOk(okMessage)
            }
        }
        client.use { it.rest.get<MessageResponse>("ping") }
        assertEquals(1, recorded.count { it.url.encodedPath.endsWith("/auth/refresh") })
        assertEquals(expired, store.loadTokens())
    }

    @Test
    fun destinationDownloadRetriesAndAtomicallyReplacesFile() = runTest {
        var attempts = 0
        val (client, _) = client { request ->
            if (request.url.encodedPath.endsWith("/download")) {
                attempts += 1
                if (attempts == 1) {
                    jsonStatus(HttpStatusCode.ServiceUnavailable, "retry")
                } else {
                    respond(
                        "new payload",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/octet-stream"),
                    )
                }
            } else {
                jsonOk(okMessage)
            }
        }
        val directory = Files.createTempDirectory("arcane-download-test-")
        val destination = directory.resolve("backup.tar")
        Files.writeString(destination, "old payload")
        try {
            client.use { it.transport.downloadRaw("download", destination, authorized = false) }
            assertEquals("new payload", Files.readString(destination))
            assertEquals(2, attempts)
            assertTrue(Files.list(directory).use { files -> files.noneMatch { it.fileName.toString().contains("arcane-download-") } })
        } finally {
            Files.deleteIfExists(destination)
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun finalUnauthorizedDoesNotClearReplacementCredentials() = runTest {
        val old = TokenPair("old", "", future)
        val replacement = TokenPair("replacement", "", future)
        val store = InMemoryTokenStore(old)
        val requestStarted = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val (client, _) = client(tokenStore = store) {
            requestStarted.complete(Unit)
            releaseResponse.await()
            jsonStatus(HttpStatusCode.Unauthorized, """{"error":"unauthorized"}""")
        }
        client.use {
            val request = async {
                assertFailsWith<ArcaneError.Unauthorized> { it.transport.rawRequestBytes("protected") }
            }
            requestStarted.await()
            it.authManager.save(replacement)
            releaseResponse.complete(Unit)
            request.await()
        }
        assertEquals(replacement, store.loadTokens())
    }

    @Test
    fun inFlightRefreshCannotRestoreCredentialsAfterClear() = runTest {
        val store = InMemoryTokenStore(TokenPair("old", "refresh", future))
        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val (client, _) = client(tokenStore = store) { request ->
            if (request.url.encodedPath.endsWith("/auth/refresh")) {
                refreshStarted.complete(Unit)
                releaseRefresh.await()
                jsonOk(refreshBody("new"))
            } else {
                jsonOk(okMessage)
            }
        }
        client.use {
            val refresh = async { it.authManager.refreshTokens() }
            refreshStarted.await()
            it.authManager.clear()
            releaseRefresh.complete(Unit)
            assertFailsWith<CancellationException> { refresh.await() }
        }
        assertNull(store.loadTokens())
    }
}
