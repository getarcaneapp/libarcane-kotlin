package app.getarcane.sdk

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Validates a wired service decodes a real response through client -> service -> transport. */
class ServicesTest {
    private fun clientReturning(body: String): Pair<ArcaneClient, MutableList<HttpRequestData>> {
        val recorded = mutableListOf<HttpRequestData>()
        val engine = MockEngine { req ->
            recorded.add(req)
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)) to recorded
    }

    @Test
    fun usersListPaginatedDecodesPageThroughWiredService() = runTest {
        val body = """
            {"success":true,
             "data":[{"id":"u1","username":"alice","roles":["admin"],"canDelete":true,"requiresPasswordChange":false}],
             "pagination":{"totalPages":1,"totalItems":1,"currentPage":1,"itemsPerPage":20}}
        """.trimIndent()
        val (client, recorded) = clientReturning(body)
        client.use { c ->
            val page = c.users.listPaginated(search = "ali", limit = 20)
            assertEquals(1, page.data.size)
            assertEquals("alice", page.data[0].username)
            assertEquals(1L, page.pagination.totalItems)
        }
        // start/limit appended by paginated(); search passed through; /api prefix applied.
        assertEquals("/api/users", recorded[0].url.encodedPath)
        assertEquals("ali", recorded[0].url.parameters["search"])
        assertEquals("20", recorded[0].url.parameters["limit"])
    }

    @Test
    fun newSwiftParityEndpointsUseExpectedPathsAndQueries() = runTest {
        val responses = ArrayDeque(
            listOf(
                """{"success":true,"data":{"imageRef":"app:latest","subjectDigest":"sha256:a","attestations":[]}}""",
                """{"success":true,"data":{"id":"job","status":"running"}}""",
                """{"success":true,"data":{"id":"job","status":"completed"}}""",
            ),
        )
        val recorded = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            recorded.add(request)
            respond(
                responses.removeFirst(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            client.images.attestations(
                id = "sha256:image",
                platform = "linux/amd64",
                predicateType = "https://spdx.dev/Document",
                includeStatement = true,
            )
            client.system.triggerUpdateAll()
            client.system.updateAllStatus()
        }

        assertEquals("/api/environments/0/images/sha256:image/attestations", recorded[0].url.encodedPath)
        assertEquals("linux/amd64", recorded[0].url.parameters["platform"])
        assertEquals("https://spdx.dev/Document", recorded[0].url.parameters["predicateType"])
        assertEquals("true", recorded[0].url.parameters["statement"])
        assertEquals("/api/environments/0/system/upgrade/all", recorded[1].url.encodedPath)
        assertEquals("/api/environments/0/system/upgrade/all/status", recorded[2].url.encodedPath)
    }

    @Test
    fun missingAvatarReturnsNull() = runTest {
        val engine = MockEngine {
            respond(
                """{"error":"not found"}""",
                HttpStatusCode.NotFound,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            assertNull(client.users.getAvatar("missing"))
        }
    }
}
