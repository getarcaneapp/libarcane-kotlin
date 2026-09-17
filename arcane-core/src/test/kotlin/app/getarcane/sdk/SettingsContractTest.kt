package app.getarcane.sdk

import app.getarcane.sdk.models.settings.UpdateSettings
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsContractTest {
    @Test
    fun currentEnvironmentSettingsEncodeAsPartialUpdate() {
        val encoded = ArcaneJson.default.encodeToString(
            UpdateSettings(
                templatesDirectory = "/templates",
                activityHistoryRetentionDays = "30",
                activityHistoryMaxEntries = "1000",
                eventCleanupInterval = "0 3 * * *",
                expiredSessionsCleanupInterval = "0 * * * *",
                trivyConfig = "severity: HIGH",
                trivyIgnore = "CVE-2026-0001",
            ),
        )

        assertTrue("\"templatesDirectory\":\"/templates\"" in encoded)
        assertTrue("\"activityHistoryRetentionDays\":\"30\"" in encoded)
        assertTrue("\"trivyConfig\":\"severity: HIGH\"" in encoded)
        assertFalse("projectsDirectory" in encoded)
    }
}
