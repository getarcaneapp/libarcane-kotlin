package app.getarcane.sdk

import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.models.base.PaginationResponse
import app.getarcane.sdk.pagination.PaginatedResponse
import app.getarcane.sdk.pagination.arcanePaginator
import app.getarcane.sdk.streaming.ndjsonFlow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class Progress(val status: String, val percent: Int = 0)

/** Phase G streaming tests: NDJSON Flow line handling + paginator. */
class StreamingTest {
    private fun ndjsonClient(body: String): ArcaneClient {
        val engine = MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/x-ndjson"))
        }
        return ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine))
    }

    @Test
    fun ndjsonFlowEmitsAndSkipsNonJsonLines() = runTest {
        val body = buildString {
            appendLine("""{"status":"start","percent":0}""")
            appendLine("") // blank line -> skipped
            appendLine("pulling layer abc123") // non-JSON heartbeat -> skipped
            appendLine("""{"status":"done","percent":100}""")
        }
        ndjsonClient(body).use { c ->
            val events = c.transport
                .ndjsonFlow("environments/0/images/pull", Progress.serializer(), method = "POST")
                .toList()
            assertEquals(listOf(Progress("start", 0), Progress("done", 100)), events)
        }
    }

    @Test
    fun ndjsonFlowThrowsOnMalformedJsonLine() = runTest {
        val body = buildString {
            appendLine("""{"status":"ok","percent":1}""")
            appendLine("""{"status":}""") // JSON-shaped but invalid -> must throw
        }
        ndjsonClient(body).use { c ->
            assertFailsWith<ArcaneError.Decoding> {
                c.transport.ndjsonFlow("environments/0/images/pull", Progress.serializer(), method = "POST").toList()
            }
        }
    }

    @Test
    fun paginatorWalksPagesUntilExhausted() = runTest {
        val pages = listOf(
            PaginatedResponse(true, listOf("a", "b"), PaginationResponse(2, 3, 1, 2)),
            PaginatedResponse(true, listOf("c"), PaginationResponse(2, 3, 2, 2)),
        )
        var call = 0
        val all = arcanePaginator(limit = 2) { _, _ -> pages[call++] }.toList()
        assertEquals(listOf("a", "b", "c"), all)
        assertEquals(2, call)
    }
}
