package app.getarcane.sdk

import app.getarcane.sdk.models.notification.NotificationProvider
import app.getarcane.sdk.models.notification.UpdateNotificationSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationsServiceTest {
    @Test
    fun settingsEndpointsDecodeDirectResponses() = runTest {
        val responses = ArrayDeque(
            listOf(
                """[{"id":1,"provider":"googlechat","enabled":true,"config":{"webhookUrl":"redacted"}}]""",
                """{"id":2,"provider":"discord","enabled":false,"config":{}}""",
                """{"id":2,"provider":"discord","enabled":true,"config":{}}""",
            ),
        )
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests.add(request)
            respond(
                responses.removeFirst(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val environmentId = EnvironmentId("manager")
            val listed = client.notifications.listSettings(environmentId).single()
            val loaded = client.notifications.getSettings(NotificationProvider.DISCORD, environmentId)
            val updated = client.notifications.upsertSettings(
                UpdateNotificationSettings(
                    provider = NotificationProvider.DISCORD,
                    enabled = true,
                ),
                environmentId,
            )

            assertEquals(NotificationProvider.GOOGLE_CHAT, listed.provider)
            assertTrue(listed.enabled)
            assertEquals(NotificationProvider.DISCORD, loaded.provider)
            assertFalse(loaded.enabled)
            assertTrue(updated.enabled)
        }

        assertEquals(
            listOf(
                "/api/environments/manager/notifications/settings",
                "/api/environments/manager/notifications/settings/discord",
                "/api/environments/manager/notifications/settings",
            ),
            requests.map { it.url.encodedPath },
        )
        assertEquals(listOf(HttpMethod.Get, HttpMethod.Get, HttpMethod.Post), requests.map { it.method })
    }
}
