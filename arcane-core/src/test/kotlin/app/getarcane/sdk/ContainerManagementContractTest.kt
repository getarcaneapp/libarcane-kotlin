package app.getarcane.sdk

import app.getarcane.sdk.models.container.ContainerCommitRequest
import app.getarcane.sdk.models.container.ContainerEdit
import app.getarcane.sdk.models.container.ContainerGenerateComposeRequest
import app.getarcane.sdk.models.container.HostConfigEdit
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContainerManagementContractTest {
    @Test
    fun editConfigDecodesPreservationSnapshot() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/api/environments/edge/containers/c1/edit-config", request.url.encodedPath)
            respond(
                """
                {"success":true,"data":{"id":"c1","name":"worker","image":"busybox:latest",
                 "hostConfig":{"binds":["data:/data"],"restartPolicy":{"name":"unless-stopped"},
                 "privileged":false,"capAdd":["NET_ADMIN"]},"running":true,
                 "environment":["TOKEN=preserved"],"networks":{"bridge":{"aliases":["worker"]}}}}
                """.trimIndent(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val config = client.containers.editConfig(EnvironmentId("edge"), "c1")
            assertEquals("TOKEN=preserved", config.environment?.single())
            assertEquals(listOf("NET_ADMIN"), config.hostConfig.capAdd)
            assertTrue(config.running)
        }
    }

    @Test
    fun mutationContractsUseCurrentRoutesAndOmitUnownedSections() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val responses = ArrayDeque(
            listOf(
                containerDetailsResponse(id = "c2"),
                """{"success":true,"data":{"id":"sha256:image"}}""",
                """{"success":true,"data":{"composeContent":"services:\n  worker:\n    image: busybox"}}""",
            ),
        )
        val engine = MockEngine { request ->
            requests += request
            respond(
                responses.removeFirst(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val edited = client.containers.edit(
                EnvironmentId("edge"),
                "c1",
                ContainerEdit(image = "busybox:1.37", hostConfig = HostConfigEdit(privileged = false)),
            )
            assertEquals("c2", edited.id)
            assertEquals(
                "sha256:image",
                client.containers.commit(
                    EnvironmentId("edge"),
                    "c2",
                    ContainerCommitRequest(repository = "audit/worker", tag = "snapshot", noPause = false),
                ).id,
            )
            assertTrue(
                client.containers.generateCompose(
                    EnvironmentId("edge"),
                    ContainerGenerateComposeRequest(listOf("c2")),
                ).composeContent.startsWith("services:"),
            )
        }

        assertEquals("/api/environments/edge/containers/c1/edit", requests[0].url.encodedPath)
        assertEquals("/api/environments/edge/containers/c2/commit", requests[1].url.encodedPath)
        assertEquals("/api/environments/edge/containers/generate-compose", requests[2].url.encodedPath)
        val editBody = renderBody(requests[0])
        assertTrue("\"image\":\"busybox:1.37\"" in editBody)
        assertTrue("\"privileged\":false" in editBody)
        assertFalse("environment" in editBody)
        assertFalse("credentials" in editBody)
        assertTrue("\"noPause\":false" in renderBody(requests[1]))
        assertTrue("\"containerIds\":[\"c2\"]" in renderBody(requests[2]))
    }

    private fun containerDetailsResponse(id: String): String =
        """
        {"success":true,"data":{"id":"$id","name":"worker","image":"busybox:1.37",
         "imageId":"sha256:base","created":"2026-09-17T00:00:00Z",
         "state":{"status":"running","running":true},"config":{},"hostConfig":{},
         "networkSettings":{"networks":{}},"ports":[],"mounts":[]}}
        """.trimIndent()

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
