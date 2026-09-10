package app.getarcane.sdk.android.passkey

import app.getarcane.sdk.ArcaneClient
import app.getarcane.sdk.ArcaneConfiguration
import app.getarcane.sdk.models.auth.AuthenticationResult
import app.getarcane.sdk.models.auth.MobilePasskeyBridgeManifest
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidPasskeyBrowserBridgeStateTest {
    private val manifest = MobilePasskeyBridgeManifest(
        version = 2,
        path = "/mobile/passkey",
        requestFragmentParameter = "request",
        maximumRequestBytes = 65_536,
    )

    @Test
    fun rejectsConcurrentStartWhilePreparingAndCancellationPreventsLaunch() = runTest {
        val reachedManifest = CompletableDeferred<Unit>()
        val releaseManifest = CompletableDeferred<Unit>()
        val source = PasskeyBridgeManifestSource {
            reachedManifest.complete(Unit)
            releaseManifest.await()
            manifest
        }
        val (client, _) = client()
        client.use {
            supervisorScope {
                val target = RecordingLaunchTarget()
                val bridge = bridge(it, source)
                val first = async { bridge.startLogin(target) }
                reachedManifest.await()

                assertTrue(bridge.hasActiveSession)
                assertFailsWith<PasskeyBridgeSessionActiveException> { bridge.startStepUp(target) }
                bridge.cancel()
                releaseManifest.complete(Unit)
                assertFailsWith<PasskeyBrowserBridgeCancelledException> { first.await() }
                assertFalse(bridge.hasActiveSession)
                assertTrue(target.urls.isEmpty())
            }
        }
    }

    @Test
    fun coroutineCancellationClearsPreparingSession() = runTest {
        val reachedManifest = CompletableDeferred<Unit>()
        val source = PasskeyBridgeManifestSource {
            reachedManifest.complete(Unit)
            awaitCancellation()
        }
        val (client, _) = client()
        client.use {
            val bridge = bridge(it, source)
            val job = launch { bridge.startLogin(RecordingLaunchTarget()) }
            reachedManifest.await()
            assertTrue(bridge.hasActiveSession)

            job.cancelAndJoin()
            assertFalse(bridge.hasActiveSession)
        }
    }

    @Test
    fun invalidCallbackCanBeRetriedAndValidCallbackIsConsumedExactlyOnce() = runTest {
        val (client, requests) = client()
        client.use {
            val target = RecordingLaunchTarget()
            val bridge = bridge(it)
            bridge.startLogin(target)
            val state = requestState(target.urls.single())

            assertFailsWith<PasskeyBridgeStateMismatchException> {
                bridge.complete(loginCallback("stale-state", "mobile-transaction"))
            }
            assertTrue(bridge.hasActiveSession)

            val result = assertIs<PasskeyBrowserBridgeResult.Login>(
                bridge.complete(loginCallback(state, "mobile-transaction")),
            )
            assertIs<AuthenticationResult.Authenticated>(result.result)
            assertFalse(bridge.hasActiveSession)
            assertFailsWith<PasskeyBridgeNoActiveSessionException> {
                bridge.complete(loginCallback(state, "mobile-transaction"))
            }
        }
        assertEquals(1, requests.count { it.url.encodedPath == "/api/auth/passkey/mobile/exchange" })
    }

    @Test
    fun cancelRejectsPreviouslyValidCallback() = runTest {
        val (client, requests) = client()
        client.use {
            val target = RecordingLaunchTarget()
            val bridge = bridge(it)
            bridge.startLogin(target)
            val state = requestState(target.urls.single())
            bridge.cancel()

            assertFalse(bridge.hasActiveSession)
            assertFailsWith<PasskeyBridgeNoActiveSessionException> {
                bridge.complete(loginCallback(state, "mobile-transaction"))
            }
        }
        assertEquals(0, requests.count { it.url.encodedPath == "/api/auth/passkey/mobile/exchange" })
    }

    @Test
    fun staleCallbackCannotConsumeReplacementSession() = runTest {
        val (client, requests) = client()
        client.use {
            val target = RecordingLaunchTarget()
            val bridge = bridge(it)
            bridge.startLogin(target)
            val staleState = requestState(target.urls.last())
            bridge.cancel()
            bridge.startLogin(target)
            val currentState = requestState(target.urls.last())

            assertFailsWith<PasskeyBridgeStateMismatchException> {
                bridge.complete(loginCallback(staleState, "stale-transaction"))
            }
            assertTrue(bridge.hasActiveSession)
            assertIs<PasskeyBrowserBridgeResult.Login>(
                bridge.complete(loginCallback(currentState, "current-transaction")),
            )
        }
        assertEquals(1, requests.count { it.url.encodedPath == "/api/auth/passkey/mobile/exchange" })
    }

    @Test
    fun mapsAllFourCeremoniesToTheirTypedFinishOperations() = runTest {
        val (client, requests) = client()
        client.use {
            val target = RecordingLaunchTarget()
            val bridge = bridge(it)

            bridge.startLogin(target)
            assertIs<PasskeyBrowserBridgeResult.Login>(
                bridge.complete(loginCallback(requestState(target.urls.last()), "mobile-transaction")),
            )

            bridge.startMfa(target, "primary-mfa-transaction")
            assertIs<PasskeyBrowserBridgeResult.Mfa>(
                bridge.complete(credentialCallback(requestState(target.urls.last()))),
            )

            bridge.startRegistration(target, name = "Phone", stepUpToken = "step-up-token")
            val registration = assertIs<PasskeyBrowserBridgeResult.Registration>(
                bridge.complete(credentialCallback(requestState(target.urls.last()))),
            )
            assertEquals("Phone", registration.passkey.name)

            bridge.startStepUp(target)
            assertIs<PasskeyBrowserBridgeResult.StepUp>(
                bridge.complete(credentialCallback(requestState(target.urls.last()))),
            )
        }

        assertTrue(requests.any { it.url.encodedPath == "/api/auth/passkey/mobile/exchange" })
        assertTrue(requests.any { it.url.encodedPath == "/api/auth/mfa/passkey/begin" })
        assertTrue(requests.any { it.url.encodedPath == "/api/auth/mfa/passkey/finish" })
        assertTrue(requests.any { it.url.encodedPath == "/api/auth/me/passkeys/register/begin" })
        assertTrue(requests.any { it.url.encodedPath == "/api/auth/me/passkeys/register/finish" })
        assertTrue(requests.any { it.url.encodedPath == "/api/auth/me/passkeys/reauth/begin" })
        assertTrue(requests.any { it.url.encodedPath == "/api/auth/me/passkeys/reauth/finish" })
    }

    @Test
    fun launchFailureClearsSessionAndAllowsFreshStart() = runTest {
        val (client, _) = client()
        client.use {
            val bridge = bridge(it)
            val failure = assertFailsWith<PasskeyBridgeLaunchException> {
                bridge.startLogin(RecordingLaunchTarget(fail = true))
            }
            assertFalse(failure.toString().contains("sensitive URI"))
            assertFalse(failure.toString().contains("#request="))
            assertFalse(bridge.hasActiveSession)

            val successful = RecordingLaunchTarget()
            bridge.startLogin(successful)
            assertTrue(bridge.hasActiveSession)
            assertEquals(1, successful.urls.size)
        }
    }

    @Test
    fun manifestFailureClearsSessionAndAllowsFreshStart() = runTest {
        val calls = AtomicInteger()
        val source = PasskeyBridgeManifestSource {
            if (calls.getAndIncrement() == 0) throw IllegalStateException("raw network detail")
            manifest
        }
        val (client, _) = client()
        client.use {
            val bridge = bridge(it, source)
            val target = RecordingLaunchTarget()
            val failure = assertFailsWith<PasskeyBridgeUnavailableException> { bridge.startLogin(target) }
            assertFalse(failure.toString().contains("raw network detail"))
            assertFalse(bridge.hasActiveSession)

            bridge.startLogin(target)
            assertTrue(bridge.hasActiveSession)
            assertEquals(1, target.urls.size)
        }
    }

    private fun bridge(
        client: ArcaneClient,
        source: PasskeyBridgeManifestSource = PasskeyBridgeManifestSource { manifest },
    ): AndroidPasskeyBrowserBridge {
        var generation = 0
        return AndroidPasskeyBrowserBridge(
            client = client,
            manifestSource = source,
            randomBytes = { count -> ByteArray(count) { (generation + it).toByte() }.also { generation += count } },
            launchDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun client(): Pair<ArcaneClient, MutableList<HttpRequestData>> {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            val response = when (request.url.encodedPath) {
                "/api/auth/passkey/login/begin" -> challenge("login-ceremony")
                "/api/auth/passkey/mobile/exchange" -> authenticated()
                "/api/auth/mfa/passkey/begin" -> mfaChallenge()
                "/api/auth/mfa/passkey/finish" -> authenticated()
                "/api/auth/me/passkeys/register/begin" -> challenge("registration-ceremony")
                "/api/auth/me/passkeys/register/finish" -> passkey()
                "/api/auth/me/passkeys/reauth/begin" -> challenge("step-up-ceremony", "step-up-transaction")
                "/api/auth/me/passkeys/reauth/finish" -> stepUp()
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)) to requests
    }

    private fun challenge(ceremonyId: String, transactionId: String? = null): String =
        """{"success":true,"data":{"ceremonyId":"$ceremonyId",${transactionId?.let { "\"transactionId\":\"$it\"," } ?: ""}"options":{"challenge":"opaque"},"expiresAt":"2030-01-01T00:00:00Z"}}"""

    private fun mfaChallenge(): String =
        """{"success":true,"data":{"transactionId":"mfa-transaction","method":"passkey","options":{"challenge":"opaque"},"expiresAt":"2030-01-01T00:00:00Z"}}"""

    private fun authenticated(): String =
        """{"success":true,"data":{"success":true,"status":"authenticated","token":"access","refreshToken":"refresh","expiresAt":"2030-01-01T00:00:00Z","user":{"id":"u1","username":"user"}}}"""

    private fun passkey(): String =
        """{"success":true,"data":{"id":"p1","name":"Phone","rpId":"test.local","createdAt":"2030-01-01T00:00:00Z"}}"""

    private fun stepUp(): String =
        """{"success":true,"data":{"token":"step-up-grant","expiresAt":"2030-01-01T00:00:00Z"}}"""

    private fun requestState(url: URI): String {
        val encoded = url.rawFragment.substringAfter("request=")
        val bytes = base64UrlDecode(encoded, PASSKEY_BRIDGE_MAX_REQUEST_BYTES)!!
        return ArcaneJson.default.parseToJsonElement(bytes.decodeToString())
            .jsonObject["state"]!!.jsonPrimitive.content
    }

    private fun loginCallback(state: String, transactionId: String): String =
        "arcane-mobile://passkey-callback?state=$state&transaction=$transactionId"

    private fun credentialCallback(state: String): String {
        val credential = """{"id":"credential","rawId":"opaque","type":"public-key","response":{"clientDataJSON":"opaque"}}"""
        return "arcane-mobile://passkey-callback?state=$state&response=${base64UrlEncode(credential.toByteArray())}"
    }

    private class RecordingLaunchTarget(
        private val fail: Boolean = false,
    ) : PasskeyBridgeLaunchTarget {
        val urls = mutableListOf<URI>()

        override fun launch(url: URI) {
            if (fail) throw IllegalStateException("browser failure with sensitive URI $url")
            urls += url
        }
    }
}
