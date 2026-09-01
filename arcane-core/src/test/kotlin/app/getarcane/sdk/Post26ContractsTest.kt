package app.getarcane.sdk

import app.getarcane.sdk.models.notification.NotificationProvider
import app.getarcane.sdk.models.notification.NotificationSettings
import app.getarcane.sdk.models.version.VersionInfo
import app.getarcane.sdk.serialization.ArcaneJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Post26ContractsTest {
    private val json = ArcaneJson.default

    @Test
    fun googleChatNotificationSettingsDecode() {
        val settings = json.decodeFromString<List<NotificationSettings>>(
            """
            [{
              "id": 42,
              "provider": "googlechat",
              "enabled": true,
              "config": {"webhookUrl": "https://chat.googleapis.com/example"}
            }]
            """.trimIndent(),
        ).single()

        assertEquals(NotificationProvider.GOOGLE_CHAT, settings.provider)
        assertTrue(settings.enabled)
    }

    @Test
    fun unknownNotificationProviderDoesNotRejectTheSettingsList() {
        val settings = json.decodeFromString<List<NotificationSettings>>(
            """
            [
              {"id": 1, "provider": "future-provider", "enabled": true, "config": {}},
              {"id": 2, "provider": "discord", "enabled": false, "config": {}}
            ]
            """.trimIndent(),
        )

        assertEquals(
            listOf(NotificationProvider.UNKNOWN, NotificationProvider.DISCORD),
            settings.map { it.provider },
        )
    }

    @Test
    fun post26FeatureGateRequiresSemverAtLeast270() {
        assertFalse(version("2.6.9").supportsPost26MobileFeatures)
        assertTrue(version("2.7.0").supportsPost26MobileFeatures)
        assertTrue(version("v2.8.1+build.4").supportsPost26MobileFeatures)
        assertFalse(version("dev").supportsPost26MobileFeatures)
        assertFalse(version("2.7").supportsPost26MobileFeatures)
        assertFalse(version("2.7.0", isSemver = false).supportsPost26MobileFeatures)

        assertFalse(version("2.7.9").supportsProjectWorkspaceContract)
        assertTrue(version("2.8.0").supportsProjectWorkspaceContract)
        assertTrue(version("v2.8.1+build.4").supportsProjectWorkspaceContract)
        assertFalse(version("dev").supportsProjectWorkspaceContract)
    }

    private fun version(currentVersion: String, isSemver: Boolean = true): VersionInfo = VersionInfo(
        currentVersion = currentVersion,
        revision = "revision",
        shortRevision = "revision",
        goVersion = "go1.25",
        displayVersion = currentVersion,
        isSemverVersion = isSemver,
        updateAvailable = false,
    )
}
