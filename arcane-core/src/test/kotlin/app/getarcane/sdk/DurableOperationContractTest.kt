package app.getarcane.sdk

import app.getarcane.sdk.models.activity.Activity
import app.getarcane.sdk.models.image.ImageProgressEvent
import app.getarcane.sdk.models.project.PullProgressEvent
import app.getarcane.sdk.models.updater.UpdaterResult
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DurableOperationContractTest {
    private val json = ArcaneJson.default

    @Test
    fun operationModelsDecodeCorrelationAndTerminalFrames() {
        val activity = json.decodeFromString<Activity>(
            """{"id":"activity-1","environmentId":"0","batchId":"operation_1","type":"project_deploy","status":"running","startedAt":"2026-09-11T12:00:00Z","createdAt":"2026-09-11T12:00:00Z"}""",
        )
        val started = json.decodeFromString<PullProgressEvent>(
            """{"type":"activity","activityId":"activity-1"}""",
        )
        val done = json.decodeFromString<PullProgressEvent>("""{"done":true}""")
        val image = json.decodeFromString<ImageProgressEvent>(
            """{"type":"activity","activityId":"image-activity","phase":"building","log":"safe only in memory","done":false}""",
        )
        val updater = json.decodeFromString<UpdaterResult>(
            """{"duration":"1s","items":[],"activityId":"updater-activity"}""",
        )

        assertEquals("operation_1", activity.batchId)
        assertEquals("activity-1", started.activityId)
        assertEquals(true, done.done)
        assertEquals("image-activity", image.activityId)
        assertEquals("building", image.phase)
        assertEquals("safe only in memory", image.log)
        assertEquals("updater-activity", updater.activityId)
    }

    @Test
    fun projectStreamSendsActivityBatchIdAndKeepsCorrelationFrames() = runTest {
        var header: String? = null
        val engine = MockEngine { request ->
            header = request.headers["X-Arcane-Batch-Id"]
            respond(
                """{"type":"activity","activityId":"activity-1"}
{"done":true}
""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val events = client.projects.deployStream(
                projectId = "project-1",
                activityBatchId = "operation_1",
            ).toList()
            assertEquals("activity-1", events.first().activityId)
            assertTrue(events.last().done == true)
        }
        assertEquals("operation_1", header)
    }

    @Test
    fun requestBackedOperationSendsActivityBatchId() = runTest {
        var header: String? = null
        val engine = MockEngine { request ->
            header = request.headers["X-Arcane-Batch-Id"]
            respond(
                """{"success":true,"data":{"duration":"1s","items":[],"activityId":"activity-2"}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val result = client.updater.run(activityBatchId = "operation-2")
            assertEquals("activity-2", result.activityId)
        }
        assertEquals("operation-2", header)
    }

    @Test
    fun invalidActivityBatchIdIsRejectedBeforeSubmission() {
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local")).use { client ->
            assertFailsWith<IllegalArgumentException> {
                client.projects.deployStream(
                    projectId = "project-1",
                    activityBatchId = "contains spaces",
                )
            }
        }
    }
}
