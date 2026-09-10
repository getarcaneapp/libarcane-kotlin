package app.getarcane.sdk

import app.getarcane.sdk.auth.InMemoryTokenStore
import app.getarcane.sdk.auth.TokenPair
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.models.auth.AuthenticationResult
import app.getarcane.sdk.models.auth.LoginRequest
import app.getarcane.sdk.models.auth.OidcCallbackRequest
import app.getarcane.sdk.models.auth.OidcCallbackResponse
import app.getarcane.sdk.models.auth.OidcDeviceAuthResponse
import app.getarcane.sdk.models.auth.OidcDeviceTokenRequest
import app.getarcane.sdk.models.auth.OidcDeviceTokenResponse
import app.getarcane.sdk.models.auth.PasswordChange
import app.getarcane.sdk.models.auth.PasskeyCredential
import app.getarcane.sdk.models.auth.PasskeyCredentialDecodingException
import app.getarcane.sdk.models.auth.RefreshRequest
import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.models.user.CreateUser
import app.getarcane.sdk.models.user.UpdateUser
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PasskeysServiceTest {
    private fun client(
        store: InMemoryTokenStore = InMemoryTokenStore(),
        response: (HttpRequestData) -> String,
    ): Pair<ArcaneClient, MutableList<HttpRequestData>> {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                response(request),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return ArcaneClient(
            ArcaneConfiguration(baseUrl = "https://test.local", engine = engine, tokenStore = store),
        ) to requests
    }

    @Test
    fun pendingMfaDoesNotPersistAuthenticationMaterial() = runTest {
        val store = InMemoryTokenStore()
        val challengeValue = "challenge-that-must-not-appear"
        val transactionValue = "transaction-that-must-not-appear"
        val (client, _) = client(store) {
            """{"success":true,"data":{"success":true,"status":"mfa_required","mfa":{"transactionId":"$transactionValue","method":"passkey","options":{"challenge":"$challengeValue"},"expiresAt":"2030-01-01T00:00:00Z"}}}"""
        }

        client.use {
            val result = it.auth.authenticate("user", "password")
            assertIs<AuthenticationResult.MfaRequired>(result)
            assertNull(store.loadTokens())
            assertFalse(result.toString().contains(challengeValue))
            assertFalse(result.toString().contains(transactionValue))
        }
    }

    @Test
    fun pendingMfaPreservesPreExistingAuthenticationMaterial() = runTest {
        val previous = TokenPair("existing-access", "existing-refresh", Instant.parse("2030-01-01T00:00:00Z"))
        val store = InMemoryTokenStore(previous)
        val (client, _) = client(store) {
            """{"success":true,"data":{"success":true,"status":"mfa_required","mfa":{"transactionId":"transaction","method":"passkey","options":{"publicKey":{}},"expiresAt":"2030-01-01T00:00:00Z"}}}"""
        }

        client.use { assertIs<AuthenticationResult.MfaRequired>(it.auth.authenticate("user", "password")) }

        assertEquals(previous, store.loadTokens())
    }

    @Test
    fun unknownAuthenticationStatusFailsClosedWithoutReplacingTokens() = runTest {
        val previous = TokenPair("existing-access", "existing-refresh", Instant.parse("2030-01-01T00:00:00Z"))
        val store = InMemoryTokenStore(previous)
        val unknownStatus = "future-status-must-not-appear"
        val (client, _) = client(store) {
            """{"success":true,"data":{"success":true,"status":"$unknownStatus","token":"new-access","refreshToken":"new-refresh","expiresAt":"2030-01-01T00:00:00Z","user":{"id":"u1","username":"user"}}}"""
        }

        val failure = client.use {
            assertFailsWith<ArcaneError.Decoding> { it.auth.authenticate("user", "password") }
        }

        assertEquals("Response could not be decoded.", failure.detail)
        assertFalse(failure.toString().contains(unknownStatus))
        assertEquals(previous, store.loadTokens())
    }

    @Test
    fun malformedSensitiveResponsesHaveStableDiagnostics() = runTest {
        val secret = "token-and-recovery-material-must-not-appear"
        val (client, _) = client { """{"success":true,"data":{"token":"$secret""" }

        val failure = client.use {
            assertFailsWith<ArcaneError.Decoding> { it.auth.authenticate("user", "password") }
        }

        assertEquals("Response could not be decoded.", failure.detail)
        assertFalse(failure.toString().contains(secret))
    }

    @Test
    fun sensitiveRequestAndCredentialDiagnosticsAreRedacted() {
        val secret = "sensitive-material"
        assertFalse(LoginRequest("user", secret).toString().contains(secret))
        assertFalse(PasswordChange(secret, "$secret-new").toString().contains(secret))
        assertFalse(RefreshRequest(secret).toString().contains(secret))
        val expiresAt = Instant.parse("2030-01-01T00:00:00Z")
        val user = User(id = "u1", username = "user")
        listOf(
            TokenPair(secret, secret, expiresAt),
            OidcCallbackRequest(secret, secret),
            OidcCallbackResponse(true, secret, secret, expiresAt, user),
            OidcDeviceAuthResponse(secret, secret, "https://example.test", expiresIn = 60),
            OidcDeviceTokenRequest(secret),
            OidcDeviceTokenResponse(true, secret, secret, expiresAt, user),
            CreateUser("user", secret),
            UpdateUser(password = secret),
        ).forEach { value -> assertFalse(value.toString().contains(secret)) }

        val failure = assertFailsWith<PasskeyCredentialDecodingException> {
            PasskeyCredential.fromProviderResponse("{not-json:$secret}")
        }
        assertFalse(failure.toString().contains(secret))
    }

    @Test
    fun publicAvailabilityIsUnauthenticatedAndUsesTypedRoute() = runTest {
        val (client, requests) = client { """{"success":true,"data":{"available":false}}""" }
        client.use { assertFalse(it.passkeys.loginAvailability().available) }

        assertEquals("/api/auth/passkey/login/availability", requests.single().url.encodedPath)
        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun mobileBridgeManifestUsesConfiguredEngineAtServerOriginWithDefaultHeaders() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                """{"version":2,"path":"/mobile/passkey","requestFragmentParameter":"request","maximumRequestBytes":65536}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        ArcaneClient(
            ArcaneConfiguration(
                baseUrl = "https://test.local/nested/api",
                engine = engine,
                tokenStore = InMemoryTokenStore(
                    TokenPair("must-not-be-sent", "refresh", Instant.parse("2030-01-01T00:00:00Z")),
                ),
                defaultHeaders = mapOf(HttpHeaders.Cookie to "session-id=disposable"),
            ),
        ).use { client ->
            val manifest = client.passkeys.mobileBridgeManifest()
            assertEquals(2, manifest.version)
            assertEquals("/mobile/passkey", manifest.path)
        }

        assertEquals("/arcane-mobile-passkey.json", requests.single().url.encodedPath)
        assertEquals("session-id=disposable", requests.single().headers[HttpHeaders.Cookie])
        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun mobileBridgeManifestRejectsMalformedAndOversizedResponses() = runTest {
        val malformedEngine = MockEngine {
            respond("not-json", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = malformedEngine)).use { client ->
            assertFailsWith<ArcaneError.Decoding> { client.passkeys.mobileBridgeManifest() }
        }

        val invalidUtf8Engine = MockEngine {
            respond(byteArrayOf(0xc3.toByte(), 0x28), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = invalidUtf8Engine)).use { client ->
            assertFailsWith<ArcaneError.Decoding> { client.passkeys.mobileBridgeManifest() }
        }

        val oversizedEngine = MockEngine {
            respond(ByteArray(4_097), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = oversizedEngine)).use { client ->
            val failure = assertFailsWith<ArcaneError.Transport> { client.passkeys.mobileBridgeManifest() }
            assertEquals("Response exceeds the configured size limit.", failure.detail)
        }
    }

    @Test
    fun mobileBridgeManifestPreservesCancellation() = runTest {
        val cancelledEngine = MockEngine { throw CancellationException("cancelled") }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = cancelledEngine)).use { client ->
            assertFailsWith<CancellationException> { client.passkeys.mobileBridgeManifest() }
        }
    }

    @Test
    fun publicPasskeyLoginMobileMfaAndRecoveryContractsAreExact() = runTest {
        val store = InMemoryTokenStore()
        val (client, requests) = client(store) { request -> publicPasskeyResponse(request.url.encodedPath) }
        val credential = PasskeyCredential.fromProviderResponse(
            """{"id":"credential-id","response":{"clientDataJSON":"opaque-client-data"}}""",
        )
        val codeChallenge = "c".repeat(43)
        val codeVerifier = "v".repeat(43)

        client.use {
            it.passkeys.beginLogin()
            assertIs<AuthenticationResult.Authenticated>(it.passkeys.finishLogin("login-ceremony", credential))
            it.passkeys.finishMobileLogin("mobile-ceremony", credential, codeChallenge)
            assertIs<AuthenticationResult.Authenticated>(
                it.passkeys.exchangeMobileLogin("mobile-transaction", codeVerifier),
            )
            it.passkeys.beginMfa("mfa-transaction")
            assertIs<AuthenticationResult.Authenticated>(it.passkeys.finishMfa("mfa-transaction", credential))
            assertIs<AuthenticationResult.Authenticated>(
                it.passkeys.finishRecovery("mfa-transaction", "recovery-code"),
            )
        }

        val expected = listOf(
            "/api/auth/passkey/login/begin",
            "/api/auth/passkey/login/finish",
            "/api/auth/passkey/mobile/finish",
            "/api/auth/passkey/mobile/exchange",
            "/api/auth/mfa/passkey/begin",
            "/api/auth/mfa/passkey/finish",
            "/api/auth/mfa/recovery",
        )
        assertEquals(expected, requests.map { it.url.encodedPath })
        requests.forEach { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[STEP_UP_HEADER])
        }

        assertEquals("", renderBody(requests[0]))
        assertCredentialBody(requests[1], "ceremonyId", "login-ceremony")
        assertCredentialBody(requests[2], "ceremonyId", "mobile-ceremony")
        assertEquals(codeChallenge, bodyObject(requests[2])["codeChallenge"]?.jsonPrimitive?.content)
        assertEquals("mobile-transaction", bodyObject(requests[3])["transactionId"]?.jsonPrimitive?.content)
        assertEquals(codeVerifier, bodyObject(requests[3])["codeVerifier"]?.jsonPrimitive?.content)
        assertEquals("mfa-transaction", bodyObject(requests[4])["transactionId"]?.jsonPrimitive?.content)
        assertCredentialBody(requests[5], "transactionId", "mfa-transaction")
        assertEquals("mfa-transaction", bodyObject(requests[6])["transactionId"]?.jsonPrimitive?.content)
        assertEquals("recovery-code", bodyObject(requests[6])["code"]?.jsonPrimitive?.content)
        assertEquals("access", store.loadTokens()?.accessToken)
    }

    @Test
    fun authenticatedPasskeyManagementContractsAreExactAndStepUpIsRequestScoped() = runTest {
        val store = InMemoryTokenStore(
            TokenPair("access", "refresh", Instant.parse("2035-01-01T00:00:00Z")),
        )
        val (client, requests) = client(store) { request -> managementPasskeyResponse(request) }
        val credential = PasskeyCredential.fromProviderResponse(
            """{"id":"credential-id","response":{"attestationObject":"opaque-attestation"}}""",
        )

        client.use {
            assertEquals(1, it.passkeys.list().size)
            assertTrue(it.passkeys.capabilities().canEnrollWithActiveSession)
            it.passkeys.beginRegistration()
            it.passkeys.beginRegistration("begin-step-up")
            assertEquals(
                "passkey-1",
                it.passkeys.finishRegistration(
                    ceremonyId = "registration-ceremony",
                    credential = credential,
                    name = "  Phone key  ",
                    stepUpToken = "finish-step-up",
                ).id,
            )
            it.passkeys.rename("passkey-1", "Renamed key", "rename-step-up")
            it.passkeys.delete("passkey-1", "delete-step-up")
            it.passkeys.beginStepUp()
            it.passkeys.finishStepUp("reauth-transaction", credential)
            it.passkeys.passwordStepUp("account-password")
            assertFalse(it.passkeys.mfaStatus().enabled)
            it.passkeys.enableMfa("enable-step-up")
            it.passkeys.disableMfa("disable-step-up")
            it.passkeys.regenerateRecoveryCodes("recovery-step-up")
        }

        val expected = listOf(
            HttpMethod.Get to "/api/auth/me/passkeys",
            HttpMethod.Get to "/api/auth/me/passkeys/capabilities",
            HttpMethod.Post to "/api/auth/me/passkeys/register/begin",
            HttpMethod.Post to "/api/auth/me/passkeys/register/begin",
            HttpMethod.Post to "/api/auth/me/passkeys/register/finish",
            HttpMethod.Put to "/api/auth/me/passkeys/passkey-1",
            HttpMethod.Delete to "/api/auth/me/passkeys/passkey-1",
            HttpMethod.Post to "/api/auth/me/passkeys/reauth/begin",
            HttpMethod.Post to "/api/auth/me/passkeys/reauth/finish",
            HttpMethod.Post to "/api/auth/me/passkeys/reauth/password",
            HttpMethod.Get to "/api/auth/me/mfa",
            HttpMethod.Post to "/api/auth/me/mfa/enable",
            HttpMethod.Post to "/api/auth/me/mfa/disable",
            HttpMethod.Post to "/api/auth/me/mfa/recovery-codes/regenerate",
        )
        assertEquals(expected, requests.map { it.method to it.url.encodedPath })
        requests.forEach { request -> assertEquals("Bearer access", request.headers[HttpHeaders.Authorization]) }

        assertNull(requests[2].headers[STEP_UP_HEADER])
        assertEquals("begin-step-up", requests[3].headers[STEP_UP_HEADER])
        assertEquals("finish-step-up", requests[4].headers[STEP_UP_HEADER])
        assertEquals("rename-step-up", requests[5].headers[STEP_UP_HEADER])
        assertEquals("delete-step-up", requests[6].headers[STEP_UP_HEADER])
        assertNull(requests[7].headers[STEP_UP_HEADER])
        assertNull(requests[8].headers[STEP_UP_HEADER])
        assertNull(requests[9].headers[STEP_UP_HEADER])
        assertNull(requests[10].headers[STEP_UP_HEADER])
        assertEquals("enable-step-up", requests[11].headers[STEP_UP_HEADER])
        assertEquals("disable-step-up", requests[12].headers[STEP_UP_HEADER])
        assertEquals("recovery-step-up", requests[13].headers[STEP_UP_HEADER])

        assertEquals("", renderBody(requests[2]))
        assertEquals("", renderBody(requests[3]))
        assertCredentialBody(requests[4], "ceremonyId", "registration-ceremony")
        assertEquals("Phone key", bodyObject(requests[4])["name"]?.jsonPrimitive?.content)
        assertEquals("Renamed key", bodyObject(requests[5])["name"]?.jsonPrimitive?.content)
        assertEquals("", renderBody(requests[6]))
        assertEquals("", renderBody(requests[7]))
        assertCredentialBody(requests[8], "transactionId", "reauth-transaction")
        assertEquals("account-password", bodyObject(requests[9])["password"]?.jsonPrimitive?.content)
        assertEquals("", renderBody(requests[11]))
        assertEquals("", renderBody(requests[12]))
        assertEquals("", renderBody(requests[13]))
    }

    @Test
    fun recoveryCompletionStoresTokensOnlyAfterAuthenticatedResult() = runTest {
        val store = InMemoryTokenStore()
        val (client, requests) = client(store) {
            """{"success":true,"data":{"success":true,"status":"authenticated","token":"access","refreshToken":"refresh","expiresAt":"2030-01-01T00:00:00Z","user":{"id":"u1","username":"user"}}}"""
        }
        client.use {
            val result = it.passkeys.finishRecovery("transaction", "one-time-code")
            assertIs<AuthenticationResult.Authenticated>(result)
        }
        assertEquals("/api/auth/mfa/recovery", requests.single().url.encodedPath)
        assertEquals("access", store.loadTokens()?.accessToken)
    }

    @Test
    fun managementStepUpHeaderIsRequestScoped() = runTest {
        val store = InMemoryTokenStore(TokenPair("access", "refresh", Instant.parse("2030-01-01T00:00:00Z")))
        val responses = ArrayDeque(
            listOf(
                """{"success":true,"data":{"enabled":false,"passkeyCount":0,"recoveryCodesRemaining":0}}""",
                """{"success":true,"data":{"codes":["code-never-diagnosed"]}}""",
            ),
        )
        val (client, requests) = client(store) { responses.removeFirst() }
        client.use {
            it.passkeys.mfaStatus()
            val codes = it.passkeys.enableMfa("step-up-secret")
            assertEquals("RecoveryCodesResponse([REDACTED])", codes.toString())
        }

        assertNull(requests[0].headers["X-Step-Up-Token"])
        assertEquals("step-up-secret", requests[1].headers["X-Step-Up-Token"])
        assertTrue(requests[1].url.encodedPath.endsWith("/auth/me/mfa/enable"))
    }

    private fun publicPasskeyResponse(path: String): String = when (path) {
        "/api/auth/passkey/login/begin" -> challengeEnvelope("login-ceremony")
        "/api/auth/passkey/login/finish",
        "/api/auth/passkey/mobile/exchange",
        "/api/auth/mfa/passkey/finish",
        "/api/auth/mfa/recovery",
        -> authenticatedEnvelope()
        "/api/auth/passkey/mobile/finish" ->
            """{"success":true,"data":{"transactionId":"mobile-transaction","expiresAt":"2030-01-01T00:00:00Z"}}"""
        "/api/auth/mfa/passkey/begin" ->
            """{"success":true,"data":{"transactionId":"mfa-transaction","method":"passkey","options":{},"expiresAt":"2030-01-01T00:00:00Z"}}"""
        else -> error("Unexpected public passkey request: $path")
    }

    private fun managementPasskeyResponse(request: HttpRequestData): String = when (request.url.encodedPath) {
        "/api/auth/me/passkeys" -> """{"success":true,"data":[${passkeySummary()}]}"""
        "/api/auth/me/passkeys/capabilities" ->
            """{"success":true,"data":{"passkeyMfaEnabled":false,"passkeyCount":1,"hasLocalPassword":true,"hasOidcFallback":false,"canEnrollWithActiveSession":true,"canDeleteLastPasskey":true,"requiresStepUp":true}}"""
        "/api/auth/me/passkeys/register/begin",
        "/api/auth/me/passkeys/reauth/begin",
        -> challengeEnvelope("management-ceremony", transactionId = "reauth-transaction")
        "/api/auth/me/passkeys/register/finish" -> """{"success":true,"data":${passkeySummary()}}"""
        "/api/auth/me/passkeys/passkey-1" -> if (request.method == HttpMethod.Delete) {
            """{"success":true,"data":{"message":"deleted"}}"""
        } else {
            """{"success":true,"data":${passkeySummary()}}"""
        }
        "/api/auth/me/passkeys/reauth/finish",
        "/api/auth/me/passkeys/reauth/password",
        -> """{"success":true,"data":{"token":"step-up-token","expiresAt":"2030-01-01T00:00:00Z"}}"""
        "/api/auth/me/mfa" ->
            """{"success":true,"data":{"enabled":false,"passkeyCount":1,"recoveryCodesRemaining":0}}"""
        "/api/auth/me/mfa/enable",
        "/api/auth/me/mfa/recovery-codes/regenerate",
        -> """{"success":true,"data":{"codes":["one-time-code"]}}"""
        "/api/auth/me/mfa/disable" -> """{"success":true,"data":{"message":"disabled"}}"""
        else -> error("Unexpected authenticated passkey request: ${request.url.encodedPath}")
    }

    private fun challengeEnvelope(ceremonyId: String, transactionId: String? = null): String =
        """{"success":true,"data":{"ceremonyId":"$ceremonyId"${transactionId?.let { ",\"transactionId\":\"$it\"" }.orEmpty()},"options":{},"expiresAt":"2030-01-01T00:00:00Z"}}"""

    private fun authenticatedEnvelope(): String =
        """{"success":true,"data":{"success":true,"status":"authenticated","token":"access","refreshToken":"refresh","expiresAt":"2030-01-01T00:00:00Z","user":{"id":"u1","username":"user"}}}"""

    private fun passkeySummary(): String =
        """{"id":"passkey-1","name":"Phone key","rpId":"test.local","createdAt":"2030-01-01T00:00:00Z"}"""

    private suspend fun assertCredentialBody(request: HttpRequestData, idKey: String, idValue: String) {
        val body = bodyObject(request)
        assertEquals(idValue, body[idKey]?.jsonPrimitive?.content)
        assertEquals("credential-id", body["credential"]?.jsonObject?.get("id")?.jsonPrimitive?.content)
    }

    private suspend fun bodyObject(request: HttpRequestData) =
        ArcaneJson.default.parseToJsonElement(renderBody(request)).jsonObject

    private suspend fun renderBody(request: HttpRequestData): String =
        when (val content = request.body) {
            is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
            is OutgoingContent.ReadChannelContent -> content.readFrom().toByteArray().decodeToString()
            is OutgoingContent.WriteChannelContent -> coroutineScope {
                val channel = ByteChannel()
                val reader = async { channel.toByteArray() }
                content.writeTo(channel)
                channel.close()
                reader.await().decodeToString()
            }
            else -> ""
        }

    private companion object {
        const val STEP_UP_HEADER: String = "X-Step-Up-Token"
    }
}
