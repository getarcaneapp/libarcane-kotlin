package app.getarcane.sdk

import app.getarcane.sdk.models.activity.ActivityStreamEventType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivitiesStreamContractTest {
    @Test
    fun activityStreamUsesMultiplexedContractAndMapsHeartbeat() = runTest {
        var requestedPath = ""
        var requestedChannels = ""
        val engine = MockEngine { request ->
            requestedPath = request.url.encodedPath
            requestedChannels = request.url.parameters["channels"].orEmpty()
            respond(
                buildString {
                    appendLine("""{"channel":"activities","activity":{"type":"error","environmentId":"edge","error":"offline","timestamp":"2026-09-11T12:00:00Z"},"timestamp":"2026-09-11T12:00:00Z"}""")
                    appendLine("""{"type":"heartbeat","timestamp":"2026-09-11T12:00:15Z"}""")
                },
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val events = client.activities.stream(limit = 25).toList()
            assertEquals(listOf(ActivityStreamEventType.ERROR, ActivityStreamEventType.HEARTBEAT), events.map { it.type })
            assertEquals("edge", events.first().environmentId)
        }
        assertEquals("/api/stream", requestedPath)
        assertEquals("activities", requestedChannels)
    }

    @Test
    fun optionalEnvironmentFilterKeepsHeartbeatsAndMatchingFrames() = runTest {
        val engine = MockEngine {
            respond(
                buildString {
                    appendLine("""{"channel":"activities","activity":{"type":"error","environmentId":"other","error":"offline","timestamp":"2026-09-11T12:00:00Z"},"timestamp":"2026-09-11T12:00:00Z"}""")
                    appendLine("""{"channel":"activities","activity":{"type":"error","environmentId":"edge","error":"offline","timestamp":"2026-09-11T12:00:01Z"},"timestamp":"2026-09-11T12:00:01Z"}""")
                    appendLine("""{"type":"heartbeat","timestamp":"2026-09-11T12:00:15Z"}""")
                },
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val events = client.activities.stream(envId = EnvironmentId("edge")).toList()
            assertEquals(listOf("edge", null), events.map { it.environmentId })
        }
    }
}
