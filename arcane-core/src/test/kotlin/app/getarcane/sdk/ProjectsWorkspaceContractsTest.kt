package app.getarcane.sdk

import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.models.containerregistry.ContainerRegistry
import app.getarcane.sdk.models.containerregistry.ContainerRegistrySync
import app.getarcane.sdk.models.containerregistry.CreateContainerRegistry
import app.getarcane.sdk.models.containerregistry.UpdateContainerRegistry
import app.getarcane.sdk.models.project.CreateProject
import app.getarcane.sdk.models.project.DeployOptions
import app.getarcane.sdk.models.project.DeployPullPolicy
import app.getarcane.sdk.models.project.ProjectFileChange
import app.getarcane.sdk.models.project.ProjectFileChangeOperation
import app.getarcane.sdk.models.project.ProjectFileDraft
import app.getarcane.sdk.models.project.ProjectDetails
import app.getarcane.sdk.models.project.UpdateProject
import app.getarcane.sdk.models.template.TemplateSourceFilter
import app.getarcane.sdk.serialization.ArcaneJson
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectsWorkspaceContractsTest {
    private val json = ArcaneJson.default

    @Test
    fun workspaceReadFileAndDownloadUseCurrentRoutes() = runTest {
        val responses = ArrayDeque(
            listOf(
                """{"success":true,"data":{"files":[{"modTime":"2026-09-01T00:00:00Z","name":"app.yaml","path":"/projects/p/config/app.yaml","relativePath":"config/app.yaml","mode":"-rw-r--r--","size":12,"isDirectory":false,"isSymlink":false,"editable":true}],"fileTreeRevision":"rev-1","fileTreeTruncated":true,"activityId":"activity-1"}}""",
                """{"success":true,"data":{"path":"/projects/p/config/app.yaml","relativePath":"config/app.yaml","name":"app.yaml","content":"enabled: true","mimeType":"text/plain; charset=utf-8","size":12,"editable":false,"readOnlyReason":"gitops_managed"}}""",
                "raw-file",
            ),
        )
        val recorded = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            recorded += request
            respond(
                responses.removeFirst(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, if (recorded.size == 3) "application/octet-stream" else "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val workspace = client.projects.workspace(projectId = "p")
            assertEquals("rev-1", workspace.fileTreeRevision)
            assertTrue(workspace.fileTreeTruncated)
            assertEquals("activity-1", workspace.activityId)
            assertTrue(workspace.files.single().editable)

            val file = client.projects.workspaceFile(projectId = "p", relativePath = "config/app.yaml")
            assertFalse(file.editable)
            assertEquals("gitops_managed", file.readOnlyReason)
            assertEquals(
                "raw-file",
                client.projects.downloadWorkspaceFile(
                    projectId = "p",
                    relativePath = "config/app.yaml",
                ).decodeToString(),
            )
        }

        assertEquals("/api/environments/0/projects/p/workspace", recorded[0].url.encodedPath)
        assertEquals("/api/environments/0/projects/p/workspace/file", recorded[1].url.encodedPath)
        assertEquals("config/app.yaml", recorded[1].url.parameters["relativePath"])
        assertEquals("/api/environments/0/projects/p/workspace/file/download", recorded[2].url.encodedPath)
    }

    @Test
    fun createAndWorkspaceUpdateUseMultipartManifestsWithOrderedBaseline() = runTest {
        val requests = mutableListOf<Pair<HttpRequestData, String>>()
        val responses = ArrayDeque(
            listOf(
                """{"success":true,"data":{"id":"p","name":"project","path":"/projects/p","status":"stopped","serviceCount":0,"runningCount":0,"isArchived":false,"createdAt":"now","updatedAt":"now","tags":[]}}""",
                """{"success":true,"data":{"files":[],"fileTreeRevision":"rev-2","fileTreeTruncated":false}}""",
            ),
        )
        val engine = MockEngine { request ->
            requests += request to renderBody(request)
            respond(
                responses.removeFirst(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            client.projects.create(
                request = CreateProject(
                    name = "project",
                    composeContent = "services: {}\n",
                    projectFiles = listOf(ProjectFileDraft("config/app.yaml", content = "enabled: true\n")),
                ),
            )
            val result = client.projects.updateWorkspace(
                projectId = "p",
                fileTreeRevision = "rev-1",
                changes = listOf(
                    ProjectFileChange(
                        operation = ProjectFileChangeOperation.UPDATE_FILE,
                        relativePath = "config/app.yaml",
                        content = "enabled: false\n",
                        baselineContent = "enabled: true\n",
                    ),
                    ProjectFileChange(
                        operation = ProjectFileChangeOperation.RENAME,
                        relativePath = "config/app.yaml",
                        newName = "app.prod.yaml",
                    ),
                ),
            )
            assertEquals("rev-2", result.fileTreeRevision)
        }

        val (createRequest, createBody) = requests[0]
        assertEquals(HttpMethod.Post, createRequest.method)
        assertEquals("/api/environments/0/projects", createRequest.url.encodedPath)
        assertTrue(createBody.contains("name=project"))
        assertTrue(createBody.contains("name=manifest"))
        assertTrue(createBody.contains("\"operation\":\"create_file\""))
        assertTrue(createBody.contains("\"uploadIndex\":0"))
        assertTrue(createBody.contains("enabled: true"))

        val (updateRequest, updateBody) = requests[1]
        assertEquals(HttpMethod.Put, updateRequest.method)
        assertEquals("/api/environments/0/projects/p/workspace", updateRequest.url.encodedPath)
        assertTrue(updateBody.contains("\"fileTreeRevision\":\"rev-1\""))
        assertTrue(updateBody.contains("\"uploadIndex\":0"))
        assertTrue(updateBody.contains("\"baselineIndex\":1"))
        assertTrue(updateBody.indexOf("enabled: false") < updateBody.indexOf("enabled: true"))
    }

    @Test
    fun workspaceConflictMapsToTypedConflict() = runTest {
        val engine = MockEngine {
            respond(
                """{"title":"Conflict","status":409,"detail":"project workspace changed"}""",
                HttpStatusCode.Conflict,
                headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val error = assertFailsWith<ArcaneError.Conflict> {
                client.projects.updateWorkspace(
                    projectId = "p",
                    fileTreeRevision = "stale",
                    changes = listOf(
                        ProjectFileChange(
                            operation = ProjectFileChangeOperation.DELETE,
                            relativePath = "obsolete.txt",
                        ),
                    ),
                )
            }
            assertEquals("project workspace changed", error.detail)
        }
    }

    @Test
    fun deployOptionsAreTypedAndReachDeployAndRedeployStreams() = runTest {
        val recorded = mutableListOf<Pair<HttpRequestData, String>>()
        val engine = MockEngine { request ->
            recorded += request to renderBody(request)
            respond(
                "{\"done\":true}\n",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }
        val options = DeployOptions(
            pullPolicy = DeployPullPolicy.ALWAYS,
            forceRecreate = true,
            removeOrphans = false,
            recreateVolumes = false,
        )
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            client.projects.deployStream(projectId = "p", options = options).collect()
            client.projects.redeployStream(projectId = "p", options = options).collect()
        }

        assertEquals("/api/environments/0/projects/p/up", recorded[0].first.url.encodedPath)
        assertEquals("/api/environments/0/projects/p/redeploy", recorded[1].first.url.encodedPath)
        assertTrue(recorded.all { (_, body) -> body.contains("\"pullPolicy\":\"always\"") })
        assertTrue(recorded.all { (_, body) -> body.contains("\"forceRecreate\":true") })
        val encoded = json.parseToJsonElement(json.encodeToString(options)).jsonObject
        assertEquals("always", encoded["pullPolicy"]?.jsonPrimitive?.content)
        assertTrue(encoded["forceRecreate"]!!.jsonPrimitive.content.toBoolean())
        assertFalse(encoded["removeOrphans"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(DeployPullPolicy.UNKNOWN, json.decodeFromString<DeployPullPolicy>("\"future\""))
        assertFailsWith<SerializationException> {
            json.encodeToString(DeployPullPolicy.UNKNOWN)
        }
    }

    @Test
    fun legacyCreateIsExplicitlyVersionScopedAndUsesJson() = runTest {
        var request: HttpRequestData? = null
        var body = ""
        val engine = MockEngine { recorded ->
            request = recorded
            body = renderBody(recorded)
            respond(
                """{"success":true,"data":{"id":"legacy","name":"project","path":"/projects/legacy","status":"stopped","serviceCount":0,"runningCount":0,"isArchived":false,"createdAt":"now","updatedAt":"now"}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val created = client.projects.create(
                request = CreateProject(name = "project", composeContent = "services: {}\n"),
                useWorkspaceContract = false,
            )
            assertEquals("legacy", created.id)
        }

        assertEquals(HttpMethod.Post, request?.method)
        assertEquals("application/json", request?.body?.contentType?.withoutParameters()?.toString())
        assertTrue(body.contains("\"composeContent\":\"services: {}\\n\""))
        assertFalse(body.contains("name=manifest"))

        assertFailsWith<ArcaneError.Validation> {
            ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
                client.projects.create(
                    request = CreateProject(
                        name = "project",
                        composeContent = "services: {}\n",
                        projectFiles = listOf(ProjectFileDraft("notes.txt", content = "do not drop")),
                    ),
                    useWorkspaceContract = false,
                )
            }
        }
    }

    @Test
    fun legacyWorkspaceRoutesAreUsedOnlyAfterCurrent404() = runTest {
        val recorded = mutableListOf<HttpRequestData>()
        val responses = ArrayDeque(
            listOf(
                HttpStatusCode.NotFound to "{}",
                HttpStatusCode.OK to """{"success":true,"data":{"id":"p","name":"project","path":"/projects/p","status":"stopped","serviceCount":0,"runningCount":0,"isArchived":false,"createdAt":"now","updatedAt":"now","projectFiles":[{"path":"/projects/p/notes.txt","relativePath":"notes.txt","name":"notes.txt","isDirectory":false,"content":"old"}],"fileTreeRevision":"legacy-rev"}}""",
                HttpStatusCode.NotFound to "{}",
                HttpStatusCode.OK to """{"success":true,"data":{"path":"/projects/p/notes.txt","relativePath":"notes.txt","content":"old"}}""",
                HttpStatusCode.NotFound to "{}",
                HttpStatusCode.OK to """{"success":true,"data":{"id":"p","name":"project","path":"/projects/p","status":"stopped","serviceCount":0,"runningCount":0,"isArchived":false,"createdAt":"now","updatedAt":"now","projectFiles":[],"fileTreeRevision":"legacy-next"}}""",
            ),
        )
        val engine = MockEngine { request ->
            recorded += request
            val (status, payload) = responses.removeFirst()
            respond(payload, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            val legacyWorkspace = client.projects.workspace(projectId = "p")
            assertEquals("legacy-rev", legacyWorkspace.fileTreeRevision)
            assertTrue(legacyWorkspace.files.single().editable)
            assertEquals("old", client.projects.workspaceFile(projectId = "p", relativePath = "notes.txt").content)
            assertEquals(
                "legacy-next",
                client.projects.updateWorkspace(
                    projectId = "p",
                    fileTreeRevision = "legacy-rev",
                    changes = listOf(
                        ProjectFileChange(
                            operation = ProjectFileChangeOperation.UPDATE_FILE,
                            relativePath = "notes.txt",
                            content = "new",
                            baselineContent = "old",
                        ),
                    ),
                ).fileTreeRevision,
            )
        }

        assertEquals(
            listOf(
                "/api/environments/0/projects/p/workspace",
                "/api/environments/0/projects/p/files",
                "/api/environments/0/projects/p/workspace/file",
                "/api/environments/0/projects/p/file",
                "/api/environments/0/projects/p/workspace",
                "/api/environments/0/projects/p",
            ),
            recorded.map { it.url.encodedPath },
        )
    }

    @Test
    fun templateSourceQueryAndRemoteDownloadUseExactContract() = runTest {
        val recorded = mutableListOf<HttpRequestData>()
        val responses = ArrayDeque(
            listOf(
                """{"success":true,"data":[],"pagination":{"totalPages":0,"totalItems":0,"currentPage":1,"itemsPerPage":20}}""",
                """{"success":true,"data":{"id":"local","name":"Imported","description":"","content":"services: {}","isCustom":true,"isRemote":false,"metadata":{"author":"Arcane","tags":["app"]}}}""",
            ),
        )
        val engine = MockEngine { request ->
            recorded += request
            respond(responses.removeFirst(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        ArcaneClient(ArcaneConfiguration(baseUrl = "https://test.local", engine = engine)).use { client ->
            client.templates.listPaginated(search = "proxy", source = TemplateSourceFilter.REMOTE)
            val imported = client.templates.download("remote:registry:proxy")
            assertEquals("Arcane", imported.metadata?.author)
        }

        assertEquals("/api/templates", recorded[0].url.encodedPath)
        assertEquals("proxy", recorded[0].url.parameters["search"])
        assertEquals("true", recorded[0].url.parameters["type"])
        assertEquals("/api/templates/remote:registry:proxy/download", recorded[1].url.encodedPath)
        assertEquals(HttpMethod.Post, recorded[1].method)
    }

    @Test
    fun registryRepositoryNamesDecodeCompatiblyAndUnchangedCredentialsAreOmitted() {
        val old = json.decodeFromString<ContainerRegistry>(registryJson())
        assertTrue(old.repositoryNames.isEmpty())
        val current = json.decodeFromString<ContainerRegistry>(
            registryJson(",\"repositoryNames\":[\"team/app\"]"),
        )
        assertEquals(listOf("team/app"), current.repositoryNames)

        val update = UpdateContainerRegistry(url = "ghcr.io", repositoryNames = emptyList())
        val encoded = json.parseToJsonElement(json.encodeToString(update)).jsonObject
        assertNull(encoded["token"])
        assertNull(encoded["awsSecretAccessKey"])
        assertEquals(0, encoded["repositoryNames"]?.jsonArray?.size)

        val create = json.parseToJsonElement(
            json.encodeToString(
                CreateContainerRegistry(
                    url = "ghcr.io",
                    username = "user",
                    token = "secret",
                    repositoryNames = listOf("team/app"),
                ),
            ),
        ).jsonObject
        assertEquals("team/app", create["repositoryNames"]?.jsonArray?.single()?.jsonPrimitive?.content)

        val timestamp = Instant.parse("2026-09-01T00:00:00Z")
        val sync = json.parseToJsonElement(
            json.encodeToString(
                ContainerRegistrySync(
                    id = "r",
                    url = "ghcr.io",
                    username = "user",
                    token = "secret",
                    insecure = false,
                    enabled = true,
                    registryType = "generic",
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    repositoryNames = listOf("team/app"),
                ),
            ),
        ).jsonObject
        assertEquals("team/app", sync["repositoryNames"]?.jsonArray?.single()?.jsonPrimitive?.content)
    }

    @Test
    fun currentProjectConfigurationFieldsEncodeAndDecode() {
        val update = json.parseToJsonElement(
            json.encodeToString(UpdateProject(overrideContent = "services: {}\n")),
        ).jsonObject
        assertEquals("services: {}\n", update["overrideContent"]?.jsonPrimitive?.content)

        val details = json.decodeFromString<ProjectDetails>(
            """
            {
              "id":"p","name":"project","path":"/projects/p","status":"stopped",
              "serviceCount":0,"runningCount":0,"isArchived":false,
              "createdAt":"now","updatedAt":"now","activityId":"activity-1",
              "composeFiles":["compose.yaml","compose.prod.yaml"],
              "overrideContent":"services: {}\n","overrideFileName":"compose.override.yaml",
              "tags":[{"name":"production","color":"green","sources":["ui"]}]
            }
            """.trimIndent(),
        )
        assertEquals("activity-1", details.activityId)
        assertEquals(listOf("compose.yaml", "compose.prod.yaml"), details.composeFiles)
        assertEquals("compose.override.yaml", details.overrideFileName)
        assertEquals("production", details.tags.single().name)
    }

    private fun registryJson(extra: String = ""): String =
        """{"id":"r","url":"ghcr.io","username":"user","insecure":false,"enabled":true,"registryType":"generic","createdAt":"2026-09-01T00:00:00Z","updatedAt":"2026-09-01T00:00:00Z"$extra}"""

    private suspend fun renderBody(request: HttpRequestData): String {
        return when (val content = request.body) {
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
}
