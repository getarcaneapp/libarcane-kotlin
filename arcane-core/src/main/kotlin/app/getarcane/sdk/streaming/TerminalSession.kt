package app.getarcane.sdk.streaming

import app.getarcane.sdk.http.ArcaneTransport
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A bidirectional container exec terminal over a WebSocket: [send] writes input, [output] is a cold
 * [Flow] of decoded output frames. Open via [connect].
 */
public class TerminalSession internal constructor(
    private val session: DefaultClientWebSocketSession,
) {
    /** Output frames as raw bytes (text frames are UTF-8 encoded). Single-consumer. */
    public val output: Flow<ByteArray> = session.incoming.receiveAsFlow().mapNotNull { frame ->
        when (frame) {
            is Frame.Text -> frame.readText().encodeToByteArray()
            is Frame.Binary -> frame.data
            else -> null
        }
    }

    public suspend fun send(text: String) {
        session.send(Frame.Text(text))
    }

    public suspend fun close() {
        session.close()
    }

    public companion object {
        public suspend fun connect(
            transport: ArcaneTransport,
            path: String,
            shell: String = "/bin/sh",
        ): TerminalSession {
            val headers = transport.authManager.authenticationHeaders()
            val url = transport.webSocketUrl(path, listOf("shell" to shell))
            val session = transport.httpClient.webSocketSession(url.toString()) {
                headers.forEach { (key, value) -> header(key, value) }
            }
            return TerminalSession(session)
        }
    }
}
