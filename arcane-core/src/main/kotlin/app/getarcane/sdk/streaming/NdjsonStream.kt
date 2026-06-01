package app.getarcane.sdk.streaming

import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.errors.fromResponse
import app.getarcane.sdk.http.ArcaneTransport
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

private const val NDJSON_ACCEPT = "application/x-ndjson, application/x-json-stream, application/json"

/**
 * Streams a newline-delimited JSON (NDJSON) endpoint as a cold [Flow]. Port of Swift `NDJSONStream`:
 * non-JSON lines (heartbeats) are skipped, but a JSON-shaped line that fails to decode throws
 * [ArcaneError.Decoding]. The request is sent (and a 401 refreshed once) when collection begins.
 * Used by image pull/build and project deploy/down/pull/build progress streams.
 */
public fun <T> ArcaneTransport.ndjsonFlow(
    path: String,
    deserializer: KSerializer<T>,
    method: String = "POST",
    query: List<Pair<String, String>> = emptyList(),
    body: Any? = null,
): Flow<T> = flow {
    var didRefresh = false
    while (true) {
        val headers = authManager.authenticationHeaders()
        val statement = httpClient.prepareRequest(buildUrl(path, query)) {
            this.method = HttpMethod.parse(method)
            header(HttpHeaders.Accept, NDJSON_ACCEPT)
            headers.forEach { (key, value) -> header(key, value) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        val retry = statement.execute { response ->
            val status = response.status.value
            if (status == 401 && !didRefresh && authManager.hasRefreshCredential()) {
                return@execute true
            }
            if (!response.status.isSuccess()) {
                throw ArcaneError.fromResponse(status, response.bodyAsText(), response.headers, json)
            }
            collectNdjson(response.bodyAsChannel(), deserializer, json)
            false
        }
        if (retry) {
            authManager.refreshTokens()
            didRefresh = true
            continue
        }
        break
    }
}

/** Reads NDJSON lines from [channel], emitting decoded elements. Shared by body and multipart streams. */
internal suspend fun <T> FlowCollector<T>.collectNdjson(
    channel: ByteReadChannel,
    deserializer: KSerializer<T>,
    json: Json,
) {
    while (true) {
        val line = channel.readUTF8Line() ?: break
        coroutineContext.ensureActive()
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        val looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[")
        try {
            emit(json.decodeFromString(deserializer, trimmed))
        } catch (e: SerializationException) {
            if (looksLikeJson) throw ArcaneError.Decoding(e.message ?: e.toString())
        } catch (e: IllegalArgumentException) {
            if (looksLikeJson) throw ArcaneError.Decoding(e.message ?: e.toString())
        }
    }
}
