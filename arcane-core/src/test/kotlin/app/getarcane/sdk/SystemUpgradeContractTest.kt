package app.getarcane.sdk

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemUpgradeContractTest {
    @Test
    fun triggerUpgradeUsesEnvironmentRouteWithoutBodyAndDecodesCurrentResult() = runTest {
        var request: HttpRequestData? = null
        var body = ""
        val engine = MockEngine { recorded ->
            request = recorded
            body = renderBody(recorded)
            respond(
                """{"success":true,"data":{"message":"Upgrade accepted","upToDate":true}}""",
                HttpStatusCode.Accepted,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val result = client.system.triggerUpgrade(envId = EnvironmentId("edge"))
            assertEquals("Upgrade accepted", result.message)
            assertTrue(result.upToDate)
        }

        assertEquals(HttpMethod.Post, request?.method)
        assertEquals("/api/environments/edge/system/upgrade", request?.url?.encodedPath)
        assertEquals("", body)
    }

    @Test
    fun triggerUpgradeDefaultsMissingUpToDateForCompatibleServers() = runTest {
        var request: HttpRequestData? = null
        var body = "not-rendered"
        val engine = MockEngine { recorded ->
            request = recorded
            body = renderBody(recorded)
            respond(
                """{"success":true,"data":{"message":"Upgrade accepted"}}""",
                HttpStatusCode.Accepted,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val result = client.system.triggerUpgrade()
            assertEquals("Upgrade accepted", result.message)
            assertFalse(result.upToDate)
        }

        assertEquals(HttpMethod.Post, request?.method)
        assertEquals("/api/environments/0/system/upgrade", request?.url?.encodedPath)
        assertEquals("", body)
    }

    private suspend fun renderBody(request: HttpRequestData): String =
        when (val content = request.body) {
            is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
            is OutgoingContent.ReadChannelContent -> content.readFrom().toByteArray().decodeToString()
            is OutgoingContent.WriteChannelContent -> coroutineScope {
                val channel = ByteChannel()
                val reader = async { channel.toByteArray() }
                content.writeTo(channel)
                channel.close()
                reader.await().decodeToString()
            }
            else -> ""
        }
}
