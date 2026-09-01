package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.http.MultipartFile
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.multipartUpload
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.project.BuildProjectRequest
import app.getarcane.sdk.models.project.CreateProject
import app.getarcane.sdk.models.project.CreateProjectConfiguration
import app.getarcane.sdk.models.project.CreateProjectWorkspaceManifest
import app.getarcane.sdk.models.project.DeployOptions
import app.getarcane.sdk.models.project.DestroyProject
import app.getarcane.sdk.models.project.ImagePullRequest
import app.getarcane.sdk.models.project.IncludeFile
import app.getarcane.sdk.models.project.ProjectCreateResponse
import app.getarcane.sdk.models.project.ProjectDetails
import app.getarcane.sdk.models.project.ProjectFileChange
import app.getarcane.sdk.models.project.ProjectFileChangeOperation
import app.getarcane.sdk.models.project.ProjectStatusCounts
import app.getarcane.sdk.models.project.ProjectWorkspace
import app.getarcane.sdk.models.project.ProjectWorkspaceFileContent
import app.getarcane.sdk.models.project.ProjectWorkspaceManifestChange
import app.getarcane.sdk.models.project.ProjectWorkspaceUpdateManifest
import app.getarcane.sdk.models.project.PullProgressEvent
import app.getarcane.sdk.models.project.UpdateIncludeFile
import app.getarcane.sdk.models.project.UpdateProject
import app.getarcane.sdk.pagination.PaginatedResponse
import app.getarcane.sdk.streaming.LogLine
import app.getarcane.sdk.streaming.logStream
import app.getarcane.sdk.streaming.ndjsonFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString

/**
 * Docker Compose project management.
 *
 * The HTTP-streaming endpoints ([deploy], [build], [pullImages]) issue the request and resolve only
 * once the server finishes emitting its NDJSON progress stream. Use the `*Stream` variants to
 * consume progress events line-by-line.
 */
public class ProjectsService internal constructor(private val rest: RestService) {
    // MARK: - Listing

    /** Paginated list of projects on the environment. */
    public suspend fun list(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        status: String? = null,
        updates: String? = null,
        archived: String? = null,
    ): PaginatedResponse<ProjectDetails> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            status?.let { add("status" to it) }
            updates?.let { add("updates" to it) }
            archived?.let { add("archived" to it) }
        }
        return rest.transport.paginated<ProjectDetails>(
            rest.environmentPath(envId, "projects"),
            query.start ?: 0,
            query.limit ?: 20,
            items,
        )
    }

    /** Aggregate counts of projects by status. */
    public suspend fun statusCounts(envId: EnvironmentId? = null): ProjectStatusCounts =
        rest.get(rest.environmentPath(envId, "projects/counts"))

    // MARK: - Single project read

    /** Get a single project by ID (no extra detail flags). */
    public suspend fun get(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId"))

    /** Get the project compose details (compose content, includes, configs). */
    public suspend fun compose(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/compose"))

    /** Get the project's on-disk directory files. */
    @Deprecated("Use workspace() with current Arcane servers")
    public suspend fun files(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/files"))

    /** Get the project's runtime service state. */
    public suspend fun runtime(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/runtime"))

    /** Get the project's image update summary. */
    public suspend fun updates(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/updates"))

    /** Get the contents of a single project-related file by relative path. */
    @Deprecated("Use workspaceFile() with current Arcane servers")
    public suspend fun file(
        envId: EnvironmentId? = null,
        projectId: String,
        relativePath: String,
    ): IncludeFile =
        rest.get(
            rest.environmentPath(envId, "projects/$projectId/file"),
            listOf("relativePath" to relativePath),
        )

    /** Get the current editable project workspace tree. */
    public suspend fun workspace(envId: EnvironmentId? = null, projectId: String): ProjectWorkspace =
        try {
            rest.get(rest.environmentPath(envId, "projects/$projectId/workspace"))
        } catch (_: ArcaneError.NotFound) {
            val legacy = files(envId = envId, projectId = projectId)
            ProjectWorkspace(
                files = legacy.projectFiles.orEmpty().map { file ->
                    file.copy(
                        editable = file.protected != true,
                        readOnlyReason = if (file.protected == true) "protected" else null,
                    )
                },
                fileTreeRevision = legacy.fileTreeRevision.orEmpty(),
            )
        }

    /** Get text content and editability metadata for one project workspace file. */
    public suspend fun workspaceFile(
        envId: EnvironmentId? = null,
        projectId: String,
        relativePath: String,
    ): ProjectWorkspaceFileContent = try {
        rest.get(
            rest.environmentPath(envId, "projects/$projectId/workspace/file"),
            listOf("relativePath" to relativePath),
        )
    } catch (_: ArcaneError.NotFound) {
        val legacy = file(envId = envId, projectId = projectId, relativePath = relativePath)
        ProjectWorkspaceFileContent(
            path = legacy.path,
            relativePath = legacy.relativePath,
            name = legacy.relativePath.substringAfterLast('/'),
            content = legacy.content,
            mimeType = "text/plain; charset=utf-8",
            size = legacy.content?.encodeToByteArray()?.size?.toLong() ?: 0,
            editable = true,
        )
    }

    /** Download a project workspace file without decoding it as text. */
    public suspend fun downloadWorkspaceFile(
        envId: EnvironmentId? = null,
        projectId: String,
        relativePath: String,
    ): ByteArray =
        rest.transport.downloadRaw(
            rest.environmentPath(envId, "projects/$projectId/workspace/file/download"),
            query = listOf("relativePath" to relativePath),
        )

    // MARK: - Mutations

    /** Create a new Docker Compose project. */
    public suspend fun create(
        envId: EnvironmentId? = null,
        request: CreateProject,
        useWorkspaceContract: Boolean = true,
    ): ProjectCreateResponse {
        if (!useWorkspaceContract) {
            val modernFields = buildList {
                if (!request.projectFiles.isNullOrEmpty()) add("projectFiles")
                if (!request.tags.isNullOrEmpty()) add("tags")
                if (!request.tagColors.isNullOrEmpty()) add("tagColors")
            }
            if (modernFields.isNotEmpty()) {
                throw ArcaneError.Validation(
                    modernFields.associateWith {
                        listOf("This field requires the Arcane 2.8 project workspace contract.")
                    },
                )
            }
            return rest.post(rest.environmentPath(envId, "projects"), body = request)
        }
        val changes = request.projectFiles.orEmpty().map { draft ->
            ProjectFileChange(
                operation = if (draft.isDirectory) {
                    ProjectFileChangeOperation.CREATE_FOLDER
                } else {
                    ProjectFileChangeOperation.CREATE_FILE
                },
                relativePath = draft.relativePath,
                content = draft.content,
            )
        }
        val multipart = buildWorkspaceMultipart(changes, allowEmptyCreateContent = true)
        val project = CreateProjectConfiguration(
            name = request.name,
            composeContent = request.composeContent,
            envContent = request.envContent,
            tags = request.tags,
            tagColors = request.tagColors,
        )
        val manifest = CreateProjectWorkspaceManifest(fileChanges = multipart.changes)
        return rest.transport.multipartUpload(
            rest.environmentPath(envId, "projects"),
            ProjectCreateResponse.serializer(),
            files = multipart.files,
            fields = mapOf(
                "project" to rest.transport.json.encodeToString(project),
                "manifest" to rest.transport.json.encodeToString(manifest),
            ),
        )
    }

    /** Update a project's name and/or compose/env content. */
    public suspend fun update(
        envId: EnvironmentId? = null,
        projectId: String,
        request: UpdateProject,
    ): ProjectDetails =
        rest.put(rest.environmentPath(envId, "projects/$projectId"), body = request)

    /** Update a single include file inside a project. */
    public suspend fun updateInclude(
        envId: EnvironmentId? = null,
        projectId: String,
        request: UpdateIncludeFile,
    ): ProjectDetails =
        rest.put(rest.environmentPath(envId, "projects/$projectId/includes"), body = request)

    /**
     * Atomically apply ordered file-tree changes to a project workspace.
     *
     * Updated file content and its optional baseline are emitted as separate indexed multipart
     * uploads. Supplying [ProjectFileChange.baselineContent] prevents overwriting a concurrent
     * content edit even when the structural file-tree revision is unchanged.
     */
    public suspend fun updateWorkspace(
        envId: EnvironmentId? = null,
        projectId: String,
        fileTreeRevision: String,
        changes: List<ProjectFileChange>,
    ): ProjectWorkspace {
        val multipart = buildWorkspaceMultipart(changes)
        val manifest = ProjectWorkspaceUpdateManifest(
            fileTreeRevision = fileTreeRevision,
            fileChanges = multipart.changes,
        )
        return try {
            rest.transport.multipartUpload(
                rest.environmentPath(envId, "projects/$projectId/workspace"),
                ProjectWorkspace.serializer(),
                files = multipart.files,
                method = "PUT",
                fields = mapOf("manifest" to rest.transport.json.encodeToString(manifest)),
            )
        } catch (_: ArcaneError.NotFound) {
            val legacy = update(
                envId = envId,
                projectId = projectId,
                request = UpdateProject(fileTreeRevision = fileTreeRevision, fileChanges = changes),
            )
            ProjectWorkspace(
                files = legacy.projectFiles.orEmpty(),
                fileTreeRevision = legacy.fileTreeRevision ?: fileTreeRevision,
            )
        }
    }

    // MARK: - Lifecycle

    /**
     * Deploy a project (docker compose up).
     *
     * The server streams NDJSON progress; this call resolves only when the deploy is fully
     * complete. Use [deployStream] for live progress.
     */
    public suspend fun deploy(
        envId: EnvironmentId? = null,
        projectId: String,
        options: DeployOptions? = null,
    ) {
        deployStream(envId, projectId, options).collect()
    }

    /** Bring down a project (docker compose down). */
    public suspend fun down(envId: EnvironmentId? = null, projectId: String) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/down"))
    }

    /** Redeploy a project (down + up). */
    public suspend fun redeploy(
        envId: EnvironmentId? = null,
        projectId: String,
        options: DeployOptions? = null,
    ) {
        redeployStream(envId, projectId, options).collect()
    }

    /** Restart all containers in a project. */
    public suspend fun restart(envId: EnvironmentId? = null, projectId: String) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/restart"))
    }

    /**
     * Destroy a project, optionally removing files and/or volumes.
     *
     * The destroy options are passed as URL query parameters since the shared DELETE helpers do not
     * forward a request body.
     */
    public suspend fun destroy(
        envId: EnvironmentId? = null,
        projectId: String,
        options: DestroyProject? = null,
    ) {
        val items = buildList {
            options?.removeFiles?.let { add("removeFiles" to it.toString()) }
            options?.removeVolumes?.let { add("removeVolumes" to it.toString()) }
        }
        rest.deleteVoid(rest.environmentPath(envId, "projects/$projectId/destroy"), query = items)
    }

    /** Archive a project (project must be stopped). */
    public suspend fun archive(envId: EnvironmentId? = null, projectId: String) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/archive"))
    }

    /** Unarchive a project. */
    public suspend fun unarchive(envId: EnvironmentId? = null, projectId: String) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/unarchive"))
    }

    /**
     * Pull all images for a project. The server streams progress as NDJSON; the call resolves once
     * the pull is complete. Use [pullImagesStream] for live progress.
     */
    public suspend fun pullImages(
        envId: EnvironmentId? = null,
        projectId: String,
        request: ImagePullRequest? = null,
    ) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/pull"), body = request)
    }

    /**
     * Build compose services that declare a `build` directive. The server streams build progress as
     * NDJSON; this call resolves once the build is complete. Use [buildStream] for live progress.
     */
    public suspend fun build(
        envId: EnvironmentId? = null,
        projectId: String,
        request: BuildProjectRequest? = null,
    ) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/build"), body = request)
    }

    // MARK: - NDJSON progress streams

    /** Deploy a project and stream NDJSON progress events. */
    public fun deployStream(
        envId: EnvironmentId? = null,
        projectId: String,
        options: DeployOptions? = null,
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "projects/$projectId/up"),
            PullProgressEvent.serializer(),
            method = "POST",
            body = options,
        )

    /** Tear down a project and stream NDJSON progress events. */
    public fun downStream(
        envId: EnvironmentId? = null,
        projectId: String,
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "projects/$projectId/down"),
            PullProgressEvent.serializer(),
            method = "POST",
        )

    /** Redeploy a project and stream NDJSON progress events. */
    public fun redeployStream(
        envId: EnvironmentId? = null,
        projectId: String,
        options: DeployOptions? = null,
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "projects/$projectId/redeploy"),
            PullProgressEvent.serializer(),
            method = "POST",
            body = options,
        )

    /** Pull a project's images and stream NDJSON progress events. */
    public fun pullImagesStream(
        envId: EnvironmentId? = null,
        projectId: String,
        request: ImagePullRequest? = null,
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "projects/$projectId/pull"),
            PullProgressEvent.serializer(),
            method = "POST",
            body = request,
        )

    /** Build a project's images and stream NDJSON progress events. */
    public fun buildStream(
        envId: EnvironmentId? = null,
        projectId: String,
        request: BuildProjectRequest? = null,
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "projects/$projectId/build"),
            PullProgressEvent.serializer(),
            method = "POST",
            body = request,
        )

    // MARK: - Streaming

    /** Stream project logs over a WebSocket. */
    public fun logs(
        envId: EnvironmentId? = null,
        projectId: String,
        follow: Boolean = true,
        tail: String = "200",
        since: String? = null,
        timestamps: Boolean = false,
    ): Flow<LogLine> {
        val query = buildList {
            add("follow" to follow.toString())
            add("tail" to tail)
            add("timestamps" to timestamps.toString())
            since?.let { add("since" to it) }
        }
        return rest.transport.logStream(rest.environmentPath(envId, "ws/projects/$projectId/logs"), query)
    }
}

private data class ProjectWorkspaceMultipart(
    val changes: List<ProjectWorkspaceManifestChange>,
    val files: List<MultipartFile>,
)

private fun buildWorkspaceMultipart(
    changes: List<ProjectFileChange>,
    allowEmptyCreateContent: Boolean = false,
): ProjectWorkspaceMultipart {
    val files = mutableListOf<MultipartFile>()
    val manifestChanges = changes.map { change ->
        var uploadIndex: Int? = null
        var baselineIndex: Int? = null
        if (change.operation == ProjectFileChangeOperation.CREATE_FILE ||
            change.operation == ProjectFileChangeOperation.UPDATE_FILE
        ) {
            val content = change.content ?: if (
                allowEmptyCreateContent && change.operation == ProjectFileChangeOperation.CREATE_FILE
            ) {
                ""
            } else {
                throw ArcaneError.Validation(
                    mapOf("fileChanges" to listOf("File content is required for create and update operations.")),
                )
            }
            uploadIndex = files.size
            files += workspaceMultipartFile(change.relativePath, content)
        }
        if (change.operation == ProjectFileChangeOperation.UPDATE_FILE && change.baselineContent != null) {
            baselineIndex = files.size
            files += workspaceMultipartFile(change.relativePath, change.baselineContent)
        }
        ProjectWorkspaceManifestChange(
            operation = change.operation,
            relativePath = change.relativePath,
            newName = change.newName,
            newParentPath = change.newParentPath,
            uploadIndex = uploadIndex,
            baselineIndex = baselineIndex,
            recursive = change.recursive,
        )
    }
    return ProjectWorkspaceMultipart(manifestChanges, files)
}

private fun workspaceMultipartFile(relativePath: String, content: String): MultipartFile = MultipartFile(
    fieldName = "files",
    filename = relativePath.substringAfterLast('/').ifEmpty { "file" },
    content = content.encodeToByteArray(),
    contentType = "text/plain; charset=utf-8",
)
