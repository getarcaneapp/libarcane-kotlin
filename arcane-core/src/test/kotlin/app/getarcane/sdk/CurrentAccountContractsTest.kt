package app.getarcane.sdk

import app.getarcane.sdk.models.user.AvatarImageFormat
import app.getarcane.sdk.models.user.UpdateProfile
import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.models.user.UserPreferences
import app.getarcane.sdk.models.user.UserTimeFormat
import app.getarcane.sdk.models.user.isGlobalAdmin
import app.getarcane.sdk.serialization.ArcaneJson
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurrentAccountContractsTest {
    private val json = ArcaneJson.default

    @Test
    fun currentUserFieldsDecodeAndServerAdminFlagIsAuthoritative() {
        val user = json.decodeFromString<User>(
            """
            {
              "id":"u1","username":"alice","roles":["admin"],
              "displayName":"Alice","email":"alice@example.com",
              "avatarUrl":"/api/users/u1/avatar","fontSize":16,"timeFormat":"24h",
              "lastLogin":"2026-09-09T20:00:00Z","isGlobalAdmin":false,
              "preferences":{
                "themeMode":"dark","applicationTheme":"ocean","accentColor":"#007AFF",
                "iconCatalog":"selfhst","oledMode":true,"glassEffectsEnabled":false,
                "animationsEnabled":true,"sidebarHoverExpansion":false,
                "keyboardShortcutsEnabled":true,"mobileNavigationMode":"docked",
                "mobileNavigationShowLabels":true,"defaultLandingPage":"/projects"
              }
            }
            """.trimIndent(),
        )

        assertEquals("/api/users/u1/avatar", user.avatarUrl)
        assertEquals(16, user.fontSize)
        assertEquals(UserTimeFormat.TWENTY_FOUR_HOUR, user.timeFormat)
        assertEquals("2026-09-09T20:00:00Z", user.lastLogin)
        assertEquals("ocean", user.preferences?.applicationTheme)
        assertEquals("/projects", user.preferences?.defaultLandingPage)
        assertFalse(user.isGlobalAdmin)
    }

    @Test
    fun unknownTimeFormatDecodesDefensively() {
        val user = json.decodeFromString<User>(
            """{"id":"u1","username":"alice","timeFormat":"future"}""",
        )

        assertEquals(UserTimeFormat.UNKNOWN, user.timeFormat)
    }

    @Test
    fun completeSelfProfileFieldsUseCurrentWireNames() {
        val encoded = json.parseToJsonElement(
            json.encodeToString(
                UpdateProfile(
                    displayName = "Alice",
                    email = "alice@example.com",
                    locale = "en-US",
                    timeFormat = UserTimeFormat.TWELVE_HOUR,
                    fontSize = 15,
                    preferences = UserPreferences(
                        themeMode = "system",
                        mobileNavigationShowLabels = true,
                    ),
                ),
            ),
        ).jsonObject

        assertEquals("12h", encoded["timeFormat"]?.jsonPrimitive?.content)
        assertEquals("15", encoded["fontSize"]?.jsonPrimitive?.content)
        assertEquals("system", encoded["preferences"]?.jsonObject?.get("themeMode")?.jsonPrimitive?.content)
        assertEquals(
            "true",
            encoded["preferences"]?.jsonObject?.get("mobileNavigationShowLabels")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun selfAvatarUploadUsesTypedMultipartRouteAndReturnsUpdatedUser() = runTest {
        var request: HttpRequestData? = null
        var body = ""
        val engine = MockEngine { recorded ->
            request = recorded
            body = renderBody(recorded)
            respond(userEnvelope(avatarUrl = "/api/users/u1/avatar"), headers = jsonHeaders)
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val updated = client.auth.uploadAvatar(
                content = "png-image".encodeToByteArray(),
                format = AvatarImageFormat.PNG,
                filename = "profile.png",
            )
            assertEquals("/api/users/u1/avatar", updated.avatarUrl)
        }

        assertEquals(HttpMethod.Post, request?.method)
        assertEquals("/api/auth/me/avatar", request?.url?.encodedPath)
        assertTrue(body.contains("name=file"))
        assertTrue(body.contains("filename=\"profile.png\""))
        assertTrue(body.contains("Content-Type: image/png"))
        assertTrue(body.contains("png-image"))
    }

    @Test
    fun selfAvatarFilenameCannotInjectMultipartHeaders() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { recorded ->
            requests += recorded
            respond(userEnvelope(avatarUrl = "/api/users/u1/avatar"), headers = jsonHeaders)
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            client.auth.uploadAvatar(
                content = "png-image".encodeToByteArray(),
                format = AvatarImageFormat.PNG,
                filename = "profile\".png",
            )
            assertFailsWith<IllegalArgumentException> {
                client.auth.uploadAvatar(
                    content = "png-image".encodeToByteArray(),
                    format = AvatarImageFormat.PNG,
                    filename = "profile.png\r\nX-Injected: true",
                )
            }
        }

        assertEquals(1, requests.size)
        val body = renderBody(requests.single())
        assertTrue(body.contains("filename=\"profile\\\".png\""))
        assertFalse(body.contains("\r\nX-Injected"))
    }

    @Test
    fun selfAvatarDeleteUsesTypedRouteAndReturnsUpdatedUser() = runTest {
        var request: HttpRequestData? = null
        val engine = MockEngine { recorded ->
            request = recorded
            respond(userEnvelope(), headers = jsonHeaders)
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            assertEquals(null, client.auth.deleteAvatar().avatarUrl)
        }

        assertEquals(HttpMethod.Delete, request?.method)
        assertEquals("/api/auth/me/avatar", request?.url?.encodedPath)
    }

    private fun userEnvelope(avatarUrl: String? = null): String =
        """{"success":true,"data":{"id":"u1","username":"alice"${avatarUrl?.let { ",\"avatarUrl\":\"$it\"" }.orEmpty()}}}"""

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

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
