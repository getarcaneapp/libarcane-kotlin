package app.getarcane.sdk.android.passkey

import app.getarcane.sdk.models.auth.PasskeyCredential
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasskeyBrowserProtocolTest {
    private val json = ArcaneJson.default
    private val manifest = PasskeyBridgeManifest(
        version = 2,
        path = "/mobile/passkey",
        requestFragmentParameter = "request",
        maximumRequestBytes = 65_536,
    )
    private val state = base64UrlEncode(ByteArray(32) { it.toByte() })

    @Test
    fun acceptsHttpsAndLoopbackHttpOriginsAndDropsNonOriginParts() {
        assertEquals("https://arcane.example:8443/", passkeyServerOrigin("https://User:Pass@Arcane.Example:8443/path?q=1#x").toASCIIString())
        assertEquals("http://localhost:3552/", passkeyServerOrigin("http://localhost:3552/api" ).toASCIIString())
        assertEquals("http://node.localhost/", passkeyServerOrigin("http://node.localhost").toASCIIString())
        assertEquals("http://127.99.1.2/", passkeyServerOrigin("http://127.99.1.2").toASCIIString())
        assertEquals("http://[::1]:3552/", passkeyServerOrigin("http://[::1]:3552/path").toASCIIString())

        listOf(
            "http://arcane.example",
            "http://127.evil.example",
            "http://127.0.0.999",
            "ftp://arcane.example",
            "https:///missing-host",
            "not a URL",
        ).forEach { value ->
            assertFailsWith<PasskeyBridgeUnsupportedOriginException> { passkeyServerOrigin(value) }
        }
    }

    @Test
    fun validatesExactVersionTwoManifestContract() {
        assertEquals(manifest, parseAndValidatePasskeyBridgeManifest(manifest))

        listOf(
            manifest.copy(version = 1),
            manifest.copy(path = "/wrong"),
            manifest.copy(requestFragmentParameter = "payload"),
            manifest.copy(maximumRequestBytes = 0),
            manifest.copy(maximumRequestBytes = 65_537),
        ).forEach { invalid ->
            assertFailsWith<PasskeyBridgeInvalidManifestException> {
                parseAndValidatePasskeyBridgeManifest(invalid)
            }
        }
    }

    @Test
    fun serializesStrictRegistrationRequestOnlyInUrlFragment() {
        val request = PasskeyBridgeRequest(
            state = state,
            operation = "register",
            options = buildJsonObject {
                put("challenge", "opaque-challenge")
                put("rp", buildJsonObject { put("id", "arcane.example") })
            },
        )
        val url = buildPasskeyBridgeAuthorizationUrl(
            passkeyServerOrigin("https://arcane.example/base"),
            manifest,
            request,
            json,
        )

        assertEquals("https://arcane.example/mobile/passkey", url.toASCIIString().substringBefore('#'))
        assertNull(url.rawQuery)
        val encoded = url.rawFragment.substringAfter("request=")
        val decoded = base64UrlDecode(encoded, PASSKEY_BRIDGE_MAX_REQUEST_BYTES)!!
        val objectValue = json.parseToJsonElement(decoded.toString(StandardCharsets.UTF_8)).jsonObject
        assertEquals(setOf("version", "state", "operation", "options"), objectValue.keys)
        assertEquals("register", objectValue["operation"]?.jsonPrimitive?.content)
        assertFalse(decoded.toString(StandardCharsets.UTF_8).contains("mobileLogin"))
    }

    @Test
    fun serializesVerifierBoundMobileLoginWithoutChangingOptions() {
        val options = buildJsonObject { put("challenge", "opaque") }
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val request = PasskeyBridgeRequest(
            state = state,
            operation = "authenticate",
            options = options,
            mobileLogin = PasskeyBridgeMobileLogin("ceremony-id", makePasskeyBridgeCodeChallenge(verifier)),
        )
        val url = buildPasskeyBridgeAuthorizationUrl(passkeyServerOrigin("https://arcane.example"), manifest, request, json)
        val decoded = base64UrlDecode(url.rawFragment.substringAfter('='), PASSKEY_BRIDGE_MAX_REQUEST_BYTES)!!
        val objectValue = json.parseToJsonElement(decoded.toString(StandardCharsets.UTF_8)).jsonObject

        assertEquals(options, objectValue["options"])
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            objectValue["mobileLogin"]?.jsonObject?.get("codeChallenge")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun enforcesManifestRequestSizeAtTheExactEncodedBoundary() {
        val request = PasskeyBridgeRequest(
            state = state,
            operation = "authenticate",
            options = buildJsonObject { put("challenge", "value") },
        )
        val size = json.encodeToString(request).toByteArray().size
        buildPasskeyBridgeAuthorizationUrl(
            passkeyServerOrigin("https://arcane.example"),
            manifest.copy(maximumRequestBytes = size),
            request,
            json,
        )
        assertFailsWith<PasskeyBridgeRequestTooLargeException> {
            buildPasskeyBridgeAuthorizationUrl(
                passkeyServerOrigin("https://arcane.example"),
                manifest.copy(maximumRequestBytes = size - 1),
                request,
                json,
            )
        }
    }

    @Test
    fun createsThirtyTwoByteStateAndStandardPkceChallenge() {
        val generated = makePasskeyBridgeState { count -> ByteArray(count) { it.toByte() } }
        assertEquals(43, generated.length)
        assertContentEquals(ByteArray(32) { it.toByte() }, base64UrlDecode(generated, 32))
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            makePasskeyBridgeCodeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
        assertFailsWith<PasskeyBridgeRandomStateException> { makePasskeyBridgeState { ByteArray(31) } }
        assertFailsWith<PasskeyBridgeRandomStateException> { makePasskeyBridgeState { error("raw entropy failure") } }
    }

    @Test
    fun parsesOpaqueCredentialCallback() {
        val credentialJson = """{"id":"credential-id","rawId":"opaque","type":"public-key","response":{"clientDataJSON":"secret"}}"""
        val callback = "arcane-mobile://passkey-callback?state=$state&response=${base64UrlEncode(credentialJson.toByteArray())}"

        val parsed = assertIs<ParsedPasskeyBridgeCallback.Credential>(
            parsePasskeyBridgeCallback(callback, state, expectsTransaction = false, json),
        )
        assertEquals(PasskeyCredential.fromProviderResponse(credentialJson), parsed.value)
        assertFalse(parsed.toString().contains("secret"))
    }

    @Test
    fun parsesVerifierBoundLoginTransactionCallback() {
        val callback = "arcane-mobile://passkey-callback?state=$state&transaction=transaction-id"
        val parsed = assertIs<ParsedPasskeyBridgeCallback.Transaction>(
            parsePasskeyBridgeCallback(callback, state, expectsTransaction = true, json),
        )
        assertEquals("transaction-id", parsed.id)
        assertFalse(parsed.toString().contains("transaction-id"))
    }

    @Test
    fun mapsOnlyTheDocumentedBridgeErrors() {
        PasskeyBrowserBridgeErrorCode.entries.forEach { code ->
            val parsed = assertIs<ParsedPasskeyBridgeCallback.Error>(
                parsePasskeyBridgeCallback(
                    "arcane-mobile://passkey-callback?state=$state&error=${code.wireValue}",
                    state,
                    expectsTransaction = false,
                    json,
                ),
            )
            assertEquals(code, parsed.code)
        }
        assertFailsWith<PasskeyBridgeInvalidCallbackException> {
            parsePasskeyBridgeCallback(
                "arcane-mobile://passkey-callback?state=$state&error=server_dump",
                state,
                expectsTransaction = false,
                json,
            )
        }
    }

    @Test
    fun rejectsWrongCallbackOriginShapeAndUnexpectedKeys() {
        val response = base64UrlEncode("{}".toByteArray())
        listOf(
            "https://passkey-callback?state=$state&response=$response",
            "arcane-mobile://evil?state=$state&response=$response",
            "arcane-mobile://user@passkey-callback?state=$state&response=$response",
            "arcane-mobile://passkey-callback:9?state=$state&response=$response",
            "arcane-mobile://passkey-callback/path?state=$state&response=$response",
            "arcane-mobile://passkey-callback?state=$state&response=$response#fragment",
            "arcane-mobile://passkey-callback?state=$state&response=$response&extra=value",
        ).forEach { callback ->
            assertFailsWith<PasskeyBridgeInvalidCallbackException> {
                parsePasskeyBridgeCallback(callback, state, expectsTransaction = false, json)
            }
        }
    }

    @Test
    fun rejectsDuplicateAndMismatchedCallbackStateWithoutLeakingValues() {
        val duplicate = assertFailsWith<PasskeyBridgeDuplicateCallbackItemException> {
            parsePasskeyBridgeCallback(
                "arcane-mobile://passkey-callback?state=$state&state=$state&response=e30",
                state,
                expectsTransaction = false,
                json,
            )
        }
        val mismatch = assertFailsWith<PasskeyBridgeStateMismatchException> {
            parsePasskeyBridgeCallback(
                "arcane-mobile://passkey-callback?state=wrong&response=e30",
                state,
                expectsTransaction = false,
                json,
            )
        }
        assertFalse(duplicate.toString().contains(state))
        assertFalse(mismatch.toString().contains("wrong"))
    }

    @Test
    fun rejectsMalformedOversizedAndNonCanonicalCredentialResponses() {
        listOf("=", "a", "e30=", "@@@", base64UrlEncode(byteArrayOf(0xc3.toByte(), 0x28)))
            .forEach { response ->
                assertFailsWith<PasskeyBrowserBridgeException> {
                    parsePasskeyBridgeCallback(
                        "arcane-mobile://passkey-callback?state=$state&response=$response",
                        state,
                        expectsTransaction = false,
                        json,
                    )
                }
            }
        val oversized = "A".repeat(((PASSKEY_BRIDGE_MAX_RESPONSE_BYTES * 4) / 3) + 5)
        assertFailsWith<PasskeyBridgeInvalidCallbackException> {
            parsePasskeyBridgeCallback(
                "arcane-mobile://passkey-callback?state=$state&response=$oversized",
                state,
                expectsTransaction = false,
                json,
            )
        }
    }

    @Test
    fun base64UrlCodecIsStrictAndPaddingFree() {
        val vectors = listOf(
            byteArrayOf(0) to "AA",
            byteArrayOf(0, 1) to "AAE",
            byteArrayOf(0, 1, 2) to "AAEC",
            "Arcane".toByteArray() to "QXJjYW5l",
        )
        vectors.forEach { (bytes, encoded) ->
            assertEquals(encoded, base64UrlEncode(bytes))
            assertContentEquals(bytes, base64UrlDecode(encoded, bytes.size))
        }
        assertNull(base64UrlDecode("AA==", 10))
        assertNull(base64UrlDecode("A", 10))
        assertNull(base64UrlDecode("AB", 10))
        assertNull(base64UrlDecode("AA", 0))
        assertTrue(base64UrlEncode(ByteArray(32)).none { it == '=' || it == '+' || it == '/' })
    }

    @Test
    fun publicFailuresNeverEmbedSensitiveInputs() {
        val secret = "challenge-credential-token-recovery"
        val failures = listOf(
            PasskeyBridgeInvalidCallbackException(),
            PasskeyBridgeStateMismatchException(),
            PasskeyBridgeInvalidCredentialException(),
            PasskeyBridgeRandomStateException(),
            PasskeyBrowserBridgeFailureException(PasskeyBrowserBridgeErrorCode.FAILED),
        )
        failures.forEach { assertFalse(it.toString().contains(secret)) }
    }
}
