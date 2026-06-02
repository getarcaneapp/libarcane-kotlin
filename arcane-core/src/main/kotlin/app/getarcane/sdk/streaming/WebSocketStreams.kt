package app.getarcane.sdk.streaming

import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.http.ArcaneTransport
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Receive-only WebSocket log stream as a cold [Flow]: appends `format=json`, decodes each frame as a
 * [LogLine] (falling back to raw text). The socket opens on collection and closes when the flow is
 * cancelled or completes.
 */
public fun ArcaneTransport.logStream(
    path: String,
    query: List<Pair<String, String>> = emptyList(),
): Flow<LogLine> = flow {
    val headers = authManager.authenticationHeaders()
    val url = webSocketUrl(path, query + ("format" to "json"))
    val session = httpClient.webSocketSession(url.toString()) {
        headers.forEach { (key, value) -> header(key, value) }
    }
    try {
        for (frame in session.incoming) {
            val text = when (frame) {
                is Frame.Text -> frame.readText()
                is Frame.Binary -> frame.data.decodeToString()
                else -> null
            } ?: continue
            emit(parseLogLine(text, json))
        }
    } finally {
        session.close()
    }
}

/**
 * Receive-only WebSocket stats stream as a cold [Flow]: decodes each frame as [T] and throws
 * [ArcaneError.Decoding] on malformed frames.
 */
public fun <T> ArcaneTransport.statsStream(
    path: String,
    deserializer: KSerializer<T>,
    query: List<Pair<String, String>> = emptyList(),
): Flow<T> = flow {
    val headers = authManager.authenticationHeaders()
    val url = webSocketUrl(path, query)
    val session = httpClient.webSocketSession(url.toString()) {
        headers.forEach { (key, value) -> header(key, value) }
    }
    try {
        for (frame in session.incoming) {
            val text = when (frame) {
                is Frame.Text -> frame.readText()
                is Frame.Binary -> frame.data.decodeToString()
                else -> null
            } ?: continue
            try {
                emit(json.decodeFromString(deserializer, text))
            } catch (e: SerializationException) {
                throw ArcaneError.Decoding(e.message ?: e.toString())
            } catch (e: IllegalArgumentException) {
                throw ArcaneError.Decoding(e.message ?: e.toString())
            }
        }
    } finally {
        session.close()
    }
}

internal fun parseLogLine(raw: String, json: Json): LogLine =
    try {
        val msg = json.decodeFromString(LogLineMessage.serializer(), raw)
        LogLine(
            text = msg.message,
            seq = msg.seq,
            level = msg.level,
            service = msg.service,
            timestamp = msg.timestamp,
        )
    } catch (e: SerializationException) {
        LogLine(text = raw)
    } catch (e: IllegalArgumentException) {
        LogLine(text = raw)
    }
