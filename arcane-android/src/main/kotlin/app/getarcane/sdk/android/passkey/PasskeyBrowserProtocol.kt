package app.getarcane.sdk.android.passkey

import app.getarcane.sdk.models.auth.PasskeyCredential
import app.getarcane.sdk.models.auth.MobilePasskeyBridgeManifest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

internal const val PASSKEY_BRIDGE_VERSION: Int = 2
internal const val PASSKEY_BRIDGE_PATH: String = "/mobile/passkey"
internal const val PASSKEY_BRIDGE_REQUEST_PARAMETER: String = "request"
internal const val PASSKEY_BRIDGE_CALLBACK_SCHEME: String = "arcane-mobile"
internal const val PASSKEY_BRIDGE_CALLBACK_HOST: String = "passkey-callback"
internal const val PASSKEY_BRIDGE_MAX_REQUEST_BYTES: Int = 65_536
internal const val PASSKEY_BRIDGE_MAX_RESPONSE_BYTES: Int = 65_536
internal const val PASSKEY_BRIDGE_STATE_BYTES: Int = 32

internal typealias PasskeyBridgeManifest = MobilePasskeyBridgeManifest

@Serializable
internal data class PasskeyBridgeRequest(
    val version: Int = PASSKEY_BRIDGE_VERSION,
    val state: String,
    val operation: String,
    val options: JsonObject,
    val mobileLogin: PasskeyBridgeMobileLogin? = null,
) {
    override fun toString(): String = "PasskeyBridgeRequest([REDACTED])"
}

@Serializable
internal data class PasskeyBridgeMobileLogin(
    val ceremonyId: String,
    val codeChallenge: String,
) {
    override fun toString(): String = "PasskeyBridgeMobileLogin([REDACTED])"
}

internal sealed interface ParsedPasskeyBridgeCallback {
    data class Credential(val value: PasskeyCredential) : ParsedPasskeyBridgeCallback {
        override fun toString(): String = "ParsedPasskeyBridgeCallback.Credential([REDACTED])"
    }

    data class Transaction(val id: String) : ParsedPasskeyBridgeCallback {
        override fun toString(): String = "ParsedPasskeyBridgeCallback.Transaction([REDACTED])"
    }

    data class Error(val code: PasskeyBrowserBridgeErrorCode) : ParsedPasskeyBridgeCallback
}

internal fun parseAndValidatePasskeyBridgeManifest(
    manifest: PasskeyBridgeManifest,
): PasskeyBridgeManifest {
    if (
        manifest.version != PASSKEY_BRIDGE_VERSION ||
        manifest.path != PASSKEY_BRIDGE_PATH ||
        manifest.requestFragmentParameter != PASSKEY_BRIDGE_REQUEST_PARAMETER ||
        manifest.maximumRequestBytes !in 1..PASSKEY_BRIDGE_MAX_REQUEST_BYTES
    ) {
        throw PasskeyBridgeInvalidManifestException()
    }
    return manifest
}

internal fun passkeyServerOrigin(baseUrl: String): URI {
    val parsed = try {
        URI(baseUrl.trim())
    } catch (_: Exception) {
        throw PasskeyBridgeUnsupportedOriginException()
    }
    val scheme = parsed.scheme?.lowercase()
    val host = parsed.host?.lowercase()?.removeSurrounding("[", "]")
    if (
        host == null ||
        (scheme != "https" && !(scheme == "http" && isLoopbackHost(host)))
    ) {
        throw PasskeyBridgeUnsupportedOriginException()
    }
    return try {
        URI(scheme, null, host, parsed.port, "/", null, null)
    } catch (_: Exception) {
        throw PasskeyBridgeUnsupportedOriginException()
    }
}

private fun isLoopbackHost(host: String): Boolean {
    if (host == "localhost" || host.endsWith(".localhost") || host == "::1") return true
    val octets = host.split('.')
    return octets.size == 4 &&
        octets.first() == "127" &&
        octets.all { octet -> octet.isNotEmpty() && octet.all(Char::isDigit) && octet.toIntOrNull() in 0..255 }
}

internal fun buildPasskeyBridgeAuthorizationUrl(
    origin: URI,
    manifest: PasskeyBridgeManifest,
    request: PasskeyBridgeRequest,
    json: Json,
): URI {
    val bytes = json.encodeToString(PasskeyBridgeRequest.serializer(), request).toByteArray(StandardCharsets.UTF_8)
    if (bytes.size > minOf(manifest.maximumRequestBytes, PASSKEY_BRIDGE_MAX_REQUEST_BYTES)) {
        throw PasskeyBridgeRequestTooLargeException()
    }
    val bridge = origin.resolve(manifest.path)
    return URI("${bridge.toASCIIString()}#$PASSKEY_BRIDGE_REQUEST_PARAMETER=${base64UrlEncode(bytes)}")
}

internal fun makePasskeyBridgeState(randomBytes: (Int) -> ByteArray = ::secureRandomBytes): String {
    val bytes = try {
        randomBytes(PASSKEY_BRIDGE_STATE_BYTES)
    } catch (failure: PasskeyBrowserBridgeException) {
        throw failure
    } catch (_: Exception) {
        throw PasskeyBridgeRandomStateException()
    }
    if (bytes.size != PASSKEY_BRIDGE_STATE_BYTES) throw PasskeyBridgeRandomStateException()
    return base64UrlEncode(bytes)
}

internal fun makePasskeyBridgeCodeChallenge(codeVerifier: String): String =
    base64UrlEncode(MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.UTF_8)))

private fun secureRandomBytes(count: Int): ByteArray = try {
    ByteArray(count).also(SecureRandom()::nextBytes)
} catch (_: Exception) {
    throw PasskeyBridgeRandomStateException()
}

internal fun parsePasskeyBridgeCallback(
    callbackUrl: String,
    expectedState: String,
    expectsTransaction: Boolean,
    json: Json,
): ParsedPasskeyBridgeCallback {
    val uri = try {
        URI(callbackUrl)
    } catch (_: Exception) {
        throw PasskeyBridgeInvalidCallbackException()
    }
    if (
        uri.scheme != PASSKEY_BRIDGE_CALLBACK_SCHEME ||
        uri.host != PASSKEY_BRIDGE_CALLBACK_HOST ||
        !uri.rawPath.isNullOrEmpty() ||
        uri.rawUserInfo != null ||
        uri.port != -1 ||
        uri.rawFragment != null ||
        uri.rawQuery == null
    ) {
        throw PasskeyBridgeInvalidCallbackException()
    }

    val items = parseStrictQuery(uri.rawQuery)
    if (items.values.any { it.size != 1 }) throw PasskeyBridgeDuplicateCallbackItemException()
    if (items["state"]?.singleOrNull() != expectedState) throw PasskeyBridgeStateMismatchException()

    val error = items["error"]?.singleOrNull()
    if (error != null) {
        if (items.keys != setOf("state", "error")) throw PasskeyBridgeInvalidCallbackException()
        val code = PasskeyBrowserBridgeErrorCode.fromWireValue(error)
            ?: throw PasskeyBridgeInvalidCallbackException()
        return ParsedPasskeyBridgeCallback.Error(code)
    }

    if (expectsTransaction) {
        if (items.keys != setOf("state", "transaction")) throw PasskeyBridgeInvalidCallbackException()
        val transactionId = items["transaction"]?.singleOrNull()
            ?.takeIf { it.isNotEmpty() && it.toByteArray(StandardCharsets.UTF_8).size <= 128 }
            ?: throw PasskeyBridgeInvalidCallbackException()
        return ParsedPasskeyBridgeCallback.Transaction(transactionId)
    }

    if (items.keys != setOf("state", "response")) throw PasskeyBridgeInvalidCallbackException()
    val encoded = items["response"]?.singleOrNull()
        ?.takeIf { it.toByteArray(StandardCharsets.UTF_8).size <= ((PASSKEY_BRIDGE_MAX_RESPONSE_BYTES * 4) / 3) + 4 }
        ?: throw PasskeyBridgeInvalidCallbackException()
    val response = base64UrlDecode(encoded, PASSKEY_BRIDGE_MAX_RESPONSE_BYTES)
        ?: throw PasskeyBridgeInvalidCallbackException()
    val credential = try {
        PasskeyCredential.fromProviderResponse(decodeUtf8Strict(response), json)
    } catch (_: Exception) {
        throw PasskeyBridgeInvalidCredentialException()
    }
    return ParsedPasskeyBridgeCallback.Credential(credential)
}

private fun decodeUtf8Strict(bytes: ByteArray): String =
    StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

private fun parseStrictQuery(rawQuery: String): Map<String, List<String>> {
    if (rawQuery.isEmpty()) throw PasskeyBridgeInvalidCallbackException()
    val result = linkedMapOf<String, MutableList<String>>()
    for (item in rawQuery.split('&')) {
        val separator = item.indexOf('=')
        if (separator <= 0) throw PasskeyBridgeInvalidCallbackException()
        val name = decodePercentEncoded(item.substring(0, separator))
        val value = decodePercentEncoded(item.substring(separator + 1))
        result.getOrPut(name) { mutableListOf() }.add(value)
    }
    return result
}

private fun decodePercentEncoded(value: String): String {
    val output = ByteArray(value.length)
    var inputIndex = 0
    var outputIndex = 0
    while (inputIndex < value.length) {
        val character = value[inputIndex]
        when {
            character == '%' -> {
                if (inputIndex + 2 >= value.length) throw PasskeyBridgeInvalidCallbackException()
                val high = value[inputIndex + 1].digitToIntOrNull(16)
                val low = value[inputIndex + 2].digitToIntOrNull(16)
                if (high == null || low == null) throw PasskeyBridgeInvalidCallbackException()
                output[outputIndex++] = ((high shl 4) or low).toByte()
                inputIndex += 3
            }
            character.code <= 0x7f -> {
                output[outputIndex++] = character.code.toByte()
                inputIndex++
            }
            else -> throw PasskeyBridgeInvalidCallbackException()
        }
    }
    return try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output, 0, outputIndex))
            .toString()
    } catch (_: Exception) {
        throw PasskeyBridgeInvalidCallbackException()
    }
}

internal fun base64UrlEncode(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    val output = StringBuilder((bytes.size * 4 + 2) / 3)
    var index = 0
    while (index + 2 < bytes.size) {
        val value = ((bytes[index].toInt() and 0xff) shl 16) or
            ((bytes[index + 1].toInt() and 0xff) shl 8) or
            (bytes[index + 2].toInt() and 0xff)
        output.append(alphabet[value ushr 18])
        output.append(alphabet[(value ushr 12) and 0x3f])
        output.append(alphabet[(value ushr 6) and 0x3f])
        output.append(alphabet[value and 0x3f])
        index += 3
    }
    val remaining = bytes.size - index
    if (remaining == 1) {
        val value = (bytes[index].toInt() and 0xff) shl 16
        output.append(alphabet[value ushr 18])
        output.append(alphabet[(value ushr 12) and 0x3f])
    } else if (remaining == 2) {
        val value = ((bytes[index].toInt() and 0xff) shl 16) or
            ((bytes[index + 1].toInt() and 0xff) shl 8)
        output.append(alphabet[value ushr 18])
        output.append(alphabet[(value ushr 12) and 0x3f])
        output.append(alphabet[(value ushr 6) and 0x3f])
    }
    return output.toString()
}

internal fun base64UrlDecode(value: String, maximumBytes: Int): ByteArray? {
    if (value.isEmpty() || value.length % 4 == 1 || value.any { it.base64UrlValue() < 0 }) return null
    val expectedBytes = (value.length * 6) / 8
    if (expectedBytes > maximumBytes) return null
    val output = ByteArray(expectedBytes)
    var accumulator = 0
    var bits = 0
    var outputIndex = 0
    for (character in value) {
        accumulator = (accumulator shl 6) or character.base64UrlValue()
        bits += 6
        if (bits >= 8) {
            bits -= 8
            output[outputIndex++] = (accumulator ushr bits).toByte()
            accumulator = accumulator and ((1 shl bits) - 1)
        }
    }
    if (accumulator != 0 || outputIndex != expectedBytes) return null
    return output.takeIf { base64UrlEncode(it) == value }
}

private fun Char.base64UrlValue(): Int = when (this) {
    in 'A'..'Z' -> code - 'A'.code
    in 'a'..'z' -> code - 'a'.code + 26
    in '0'..'9' -> code - '0'.code + 52
    '-' -> 62
    '_' -> 63
    else -> -1
}
