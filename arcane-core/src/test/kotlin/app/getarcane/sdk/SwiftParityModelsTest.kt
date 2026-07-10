package app.getarcane.sdk

import app.getarcane.sdk.models.container.ContainerDetails
import app.getarcane.sdk.models.container.ContainerNetworkSettings
import app.getarcane.sdk.models.container.ContainerSummary
import app.getarcane.sdk.models.dashboard.ActionItemKind
import app.getarcane.sdk.models.dashboard.ActionItemSeverity
import app.getarcane.sdk.models.dashboard.DashboardStreamErrorCode
import app.getarcane.sdk.models.dashboard.DashboardStreamEvent
import app.getarcane.sdk.models.dashboard.DashboardStreamEventType
import app.getarcane.sdk.models.environment.Environment
import app.getarcane.sdk.models.image.ImageAttestationList
import app.getarcane.sdk.models.image.ImageSummary
import app.getarcane.sdk.models.imageupdate.ImageUpdateResponse
import app.getarcane.sdk.models.project.CreateProject
import app.getarcane.sdk.models.project.ProjectDetails
import app.getarcane.sdk.models.project.ProjectFileChange
import app.getarcane.sdk.models.project.ProjectFileChangeOperation
import app.getarcane.sdk.models.project.ProjectFileDraft
import app.getarcane.sdk.models.project.UpdateProject
import app.getarcane.sdk.models.network.NetworkInspect
import app.getarcane.sdk.models.network.NetworkSummary
import app.getarcane.sdk.models.system.EnvironmentUpdateJob
import app.getarcane.sdk.models.system.EnvironmentUpdateJobStatus
import app.getarcane.sdk.models.system.EnvironmentUpdateResultStatus
import app.getarcane.sdk.models.volume.Volume
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwiftParityModelsTest {
    private val json = ArcaneJson.default

    @Test
    fun projectFileTreeModelsDecodeAndEncode() {
        val project = json.decodeFromString<ProjectDetails>(
            """
            {
              "id":"project-id","name":"Project","path":"/srv/projects/project",
              "status":"running","serviceCount":1,"runningCount":1,"isArchived":false,
              "createdAt":"2026-06-20T00:00:00Z","updatedAt":"2026-06-20T00:00:00Z",
              "fileTreeRevision":"revision-1",
              "projectFiles":[{
                "path":"/srv/projects/project/config/app.yaml","relativePath":"config/app.yaml",
                "name":"app.yaml","isDirectory":false,"size":24,
                "modTime":"2026-06-20T00:01:00Z","protected":true,"content":"enabled: true\n"
              }]
            }
            """.trimIndent(),
        )
        assertEquals("revision-1", project.fileTreeRevision)
        assertEquals("config/app.yaml", project.projectFiles?.single()?.relativePath)
        assertEquals(true, project.projectFiles?.single()?.protected)

        val create = CreateProject(
            name = "with-files",
            composeContent = "services: {}\n",
            projectFiles = listOf(ProjectFileDraft("config", isDirectory = true)),
        )
        val createObject = json.parseToJsonElement(json.encodeToString(create)).jsonObject
        assertTrue(createObject["projectFiles"]!!.jsonArray.single().jsonObject["isDirectory"]!!.jsonPrimitive.content.toBoolean())

        val update = UpdateProject(
            envContent = "TOKEN=value\n",
            fileTreeRevision = "revision-1",
            fileChanges = listOf(
                ProjectFileChange(
                    operation = ProjectFileChangeOperation.UPDATE_FILE,
                    relativePath = "config/app.yaml",
                    content = "enabled: false\n",
                ),
            ),
        )
        val updateObject = json.parseToJsonElement(json.encodeToString(update)).jsonObject
        assertEquals("revision-1", updateObject["fileTreeRevision"]?.jsonPrimitive?.content)
        assertEquals(
            "update_file",
            updateObject["fileChanges"]!!.jsonArray.single().jsonObject["operation"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun dashboardStreamDecodesTrimmedSnapshotsAndFutureValues() {
        val event = json.decodeFromString<DashboardStreamEvent>(
            """
            {
              "type":"snapshot","environmentId":"0",
              "snapshot":{
                "containers":{"data":null,"counts":{"runningContainers":7,"stoppedContainers":2,"totalContainers":9},"pagination":{"totalPages":1,"totalItems":9,"currentPage":1,"itemsPerPage":10}},
                "images":{"data":null,"pagination":{"totalPages":1,"totalItems":14,"currentPage":1,"itemsPerPage":10}},
                "imageUsageCounts":{"imagesInuse":9,"imagesUnused":5,"totalImages":14,"totalImageSize":4815162342},
                "actionItems":{"items":[
                  {"kind":"stopped_containers","count":2,"severity":"warning"},
                  {"kind":"future_kind","count":1,"severity":"catastrophic"}
                ]},
                "settings":{},
                "versionInfo":{"currentVersion":"2.0.2","revision":"abc","shortRevision":"abc","goVersion":"go1.24","displayVersion":"2.0.2","isSemverVersion":true,"newestVersion":"2.1.0","updateAvailable":true}
              },
              "timestamp":"2026-06-10T17:00:00Z"
            }
            """.trimIndent(),
        )
        assertEquals(DashboardStreamEventType.SNAPSHOT, event.type)
        assertEquals(EnvironmentId.LOCAL_DOCKER, event.resolvedEnvironmentId)
        assertTrue(event.snapshot!!.containers.data.isEmpty())
        assertTrue(event.snapshot.images.data.isEmpty())
        assertEquals(ActionItemKind.STOPPED_CONTAINERS, event.snapshot.actionItems.items.first().kind)
        assertEquals(ActionItemKind.UNKNOWN, event.snapshot.actionItems.items.last().kind)
        assertEquals(ActionItemSeverity.UNKNOWN, event.snapshot.actionItems.items.last().severity)
        assertEquals("2.1.0", event.snapshot.versionInfo?.newestVersion)

        val unknown = json.decodeFromString<DashboardStreamEvent>(
            """{"type":"future_thing","timestamp":"2026-06-10T17:00:00Z"}""",
        )
        assertEquals(DashboardStreamEventType.UNKNOWN, unknown.type)

        val error = json.decodeFromString<DashboardStreamEvent>(
            """{"type":"error","errorCode":"agent_incompatible","timestamp":"2026-06-10T17:00:00Z"}""",
        )
        assertEquals(DashboardStreamErrorCode.AGENT_INCOMPATIBLE, error.errorCode)

        val futureError = json.decodeFromString<DashboardStreamEvent>(
            """{"type":"error","errorCode":"future_code","timestamp":"2026-06-10T17:00:00Z"}""",
        )
        assertEquals(DashboardStreamErrorCode.UNKNOWN, futureError.errorCode)
    }

    @Test
    fun fleetUpdateJobModelsUnknownStatusesSafely() {
        val job = json.decodeFromString<EnvironmentUpdateJob>(
            """
            {
              "id":"job_1","status":"pending_restart","managerTargetVersion":"1.6.0",
              "results":[
                {"environmentId":"0","environmentName":"Manager","status":"updating","toVersion":"1.6.0"},
                {"environmentId":"env_2","environmentName":"Edge","status":"skipped_offline"}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(EnvironmentUpdateJobStatus.PENDING_RESTART, job.status)
        assertFalse(job.isTerminal)
        assertEquals(EnvironmentUpdateResultStatus.UPDATING, job.managerResult?.status)
        assertEquals(EnvironmentUpdateResultStatus.SKIPPED_OFFLINE, job.results?.last()?.status)

        val future = json.decodeFromString<EnvironmentUpdateJob>(
            """{"id":"job_2","status":"future","results":[{"environmentId":"0","environmentName":"Manager","status":"future"}]}""",
        )
        assertEquals(EnvironmentUpdateJobStatus.UNKNOWN, future.status)
        assertEquals(EnvironmentUpdateResultStatus.UNKNOWN, future.managerResult?.status)
    }

    @Test
    fun imageAttestationsAndNewOptionalFieldsDecode() {
        val attestations = json.decodeFromString<ImageAttestationList>(
            """
            {
              "imageRef":"example/app:latest","subjectDigest":"sha256:abc","platform":"linux/amd64",
              "attestations":[{
                "digest":"sha256:def","mediaType":"application/vnd.oci.image.manifest.v1+json",
                "predicateType":"https://spdx.dev/Document","subject":[{"name":"app","digest":{"sha256":"abc"}}],
                "size":42,"statement":{"_type":"https://in-toto.io/Statement/v1"}
              }]
            }
            """.trimIndent(),
        )
        assertEquals("sha256:defhttps://spdx.dev/Document", attestations.attestations.single().id)

        val update = json.decodeFromString<ImageUpdateResponse>(
            """{"hasUpdate":true,"activityId":"activity-1"}""",
        )
        assertEquals("activity-1", update.activityId)
        assertNull(update.checkTime)

        val environment = json.decodeFromString<Environment>(
            """{"id":"env_1","apiUrl":"https://edge.example","status":"online","lastEdgeTransport":"grpc"}""",
        )
        assertEquals("grpc", environment.lastEdgeTransport)
    }

    @Test
    fun themedIconUrlsDecodeOnContainersAndProjects() {
        val summary = json.decodeFromString<ContainerSummary>(
            """{"id":"c","names":["/app"],"image":"app:latest","imageId":"sha256:i","command":"","created":0,"ports":[],"labels":{},"state":"running","status":"Up","hostConfig":{},"networkSettings":{"networks":{}},"mounts":[],"iconLightUrl":"light","iconDarkUrl":"dark"}""",
        )
        assertEquals("light", summary.iconLightUrl)
        assertEquals("dark", summary.iconDarkUrl)

        val details = json.decodeFromString<ContainerDetails>(
            """{"id":"c","name":"/app","image":"app:latest","imageId":"sha256:i","created":"now","state":{"status":"running","running":true},"config":{},"hostConfig":{},"networkSettings":{"networks":{}},"ports":[],"mounts":[],"iconLightUrl":"light","iconDarkUrl":"dark"}""",
        )
        assertEquals("light", details.iconLightUrl)
    }

    @Test
    fun nullDockerResourceMapsDecodeAsEmptyMaps() {
        val networkSettings = json.decodeFromString<ContainerNetworkSettings>("""{"networks":null}""")
        assertTrue(networkSettings.networks.isEmpty())

        val image = json.decodeFromString<ImageSummary>(
            """{"id":"sha256:i","repoTags":[],"repoDigests":[],"created":0,"size":0,"virtualSize":0,"labels":null,"inUse":false,"repo":"app","tag":"latest"}""",
        )
        assertTrue(image.labels.isEmpty())

        val summary = json.decodeFromString<NetworkSummary>(
            """{"id":"n","name":"bridge","driver":"bridge","scope":"local","created":"2026-06-01T00:00:00Z","options":null,"labels":null}""",
        )
        assertTrue(summary.options.isEmpty())
        assertTrue(summary.labels.isEmpty())

        val inspect = json.decodeFromString<NetworkInspect>(
            """{"id":"n","name":"bridge","driver":"bridge","scope":"local","created":"2026-06-01T00:00:00Z","ipam":{},"containers":null,"options":null,"labels":null}""",
        )
        assertTrue(inspect.containers.isEmpty())
        assertTrue(inspect.options.isEmpty())
        assertTrue(inspect.labels.isEmpty())

        val volume = json.decodeFromString<Volume>(
            """{"id":"v","options":null,"labels":null,"containers":null}""",
        )
        assertTrue(volume.options.isEmpty())
        assertTrue(volume.labels.isEmpty())
        assertTrue(volume.containers.isEmpty())
    }
}
