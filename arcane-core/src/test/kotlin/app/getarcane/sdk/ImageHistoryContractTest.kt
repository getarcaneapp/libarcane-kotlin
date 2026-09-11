package app.getarcane.sdk

import app.getarcane.sdk.models.image.ImageHistoryItem
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageHistoryContractTest {
    @Test
    fun historyUsesEnvironmentScopedPerImageRouteAndDecodesDockerOrder() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                """
                {"success":true,"data":[
                  {"id":"sha256:newest","created":1700000002,"createdBy":"RUN second","tags":["fixture:latest"],"size":22,"comment":"new"},
                  {"id":"sha256:oldest","created":1700000001,"createdBy":"RUN first","tags":[],"size":11,"comment":"old"}
                ]}
                """.trimIndent(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val history = ArcaneClient(
            ArcaneConfiguration(baseUrl = "https://test.local", engine = engine),
        ).use { client ->
            client.images.history(EnvironmentId("edge-1"), "sha256:image")
        }

        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals(
            "/api/environments/edge-1/images/sha256:image/history",
            requests.single().url.encodedPath,
        )
        assertEquals(listOf("sha256:newest", "sha256:oldest"), history.map { it.id })
        assertEquals(listOf("fixture:latest"), history.first().tags)
    }

    @Test
    fun missingNullAndUnknownFieldsDecodeDefensively() {
        val items = ArcaneJson.default.decodeFromString<List<ImageHistoryItem>>(
            """
            [
              {"future":"ignored"},
              {"id":null,"created":null,"createdBy":null,"tags":null,"size":null,"comment":null},
              {"id":"","created":-1,"createdBy":"","tags":["a","b"],"size":-9,"comment":"future-safe"}
            ]
            """.trimIndent(),
        )

        assertEquals(3, items.size)
        assertEquals(ImageHistoryItem.MISSING_LAYER_ID, items[0].id)
        assertTrue(items[0].isMissingLayer)
        assertEquals(ImageHistoryItem(), items[1])
        assertTrue(items[1].isMissingLayer)
        assertTrue(items[2].isMissingLayer)
        assertEquals(listOf("a", "b"), items[2].tags)
        assertEquals(-1, items[2].created)
        assertEquals(-9, items[2].size)
        assertFalse(ImageHistoryItem(id = "sha256:layer").isMissingLayer)
    }
}
