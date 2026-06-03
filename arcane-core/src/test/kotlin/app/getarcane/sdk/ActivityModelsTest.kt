package app.getarcane.sdk

import app.getarcane.sdk.models.activity.Activity
import app.getarcane.sdk.models.activity.ActivityStatus
import app.getarcane.sdk.models.activity.ActivityStreamEvent
import app.getarcane.sdk.models.activity.ActivityStreamEventType
import app.getarcane.sdk.models.activity.ActivityType
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Decoding of background activity payloads and the activity stream envelope. */
class ActivityModelsTest {
    private val json = ArcaneJson.default

    @Test
    fun decodeRunningActivity() {
        val activity = json.decodeFromString<Activity>(
            """
            {
              "id": "act_1",
              "environmentId": "0",
              "type": "project_deploy",
              "status": "running",
              "resourceType": "project",
              "resourceId": "web",
              "resourceName": "web",
              "progress": 42,
              "step": "pulling",
              "latestMessage": "Pulling web ...",
              "startedBy": {"username": "admin", "displayName": "Admin"},
              "startedAt": "2026-06-01T12:00:00Z",
              "createdAt": "2026-06-01T12:00:00Z",
              "metadata": {"attempt": 1}
            }
            """.trimIndent(),
        )
        assertEquals("act_1", activity.id)
        assertEquals(ActivityType.PROJECT_DEPLOY, activity.type)
        assertEquals(ActivityStatus.RUNNING, activity.status)
        assertEquals(42, activity.progress)
        assertEquals("admin", activity.startedBy?.username)
        assertEquals(null, activity.endedAt)
    }

    @Test
    fun unknownEnumsCoerceToUnknown() {
        val activity = json.decodeFromString<Activity>(
            """
            {"id":"a","environmentId":"0","type":"warp_drive_engage","status":"sideways",
             "startedAt":"2026-06-01T12:00:00Z","createdAt":"2026-06-01T12:00:00Z"}
            """.trimIndent(),
        )
        assertEquals(ActivityType.UNKNOWN, activity.type)
        assertEquals(ActivityStatus.UNKNOWN, activity.status)
        assertEquals("", activity.step) // default applied
    }

    @Test
    fun decodeSnapshotStreamEvent() {
        val event = json.decodeFromString<ActivityStreamEvent>(
            """
            {
              "type": "snapshot",
              "activities": [
                {"id":"a1","environmentId":"0","type":"image_pull","status":"success",
                 "startedAt":"2026-06-01T12:00:00Z","createdAt":"2026-06-01T12:00:00Z"}
              ],
              "timestamp": "2026-06-01T12:00:01Z"
            }
            """.trimIndent(),
        )
        assertEquals(ActivityStreamEventType.SNAPSHOT, event.type)
        assertEquals(1, event.activities.size)
        assertTrue(event.activity == null)
        assertEquals(ActivityType.IMAGE_PULL, event.activities.first().type)
    }
}
