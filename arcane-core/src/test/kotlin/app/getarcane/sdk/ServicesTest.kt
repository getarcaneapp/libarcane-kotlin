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
}
