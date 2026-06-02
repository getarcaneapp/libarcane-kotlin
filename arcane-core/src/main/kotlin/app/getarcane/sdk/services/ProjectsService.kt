package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.project.BuildProjectRequest
import app.getarcane.sdk.models.project.CreateProject
import app.getarcane.sdk.models.project.DeployOptions
import app.getarcane.sdk.models.project.DestroyProject
import app.getarcane.sdk.models.project.ImagePullRequest
import app.getarcane.sdk.models.project.IncludeFile
import app.getarcane.sdk.models.project.ProjectCreateResponse
import app.getarcane.sdk.models.project.ProjectDetails
import app.getarcane.sdk.models.project.ProjectStatusCounts
import app.getarcane.sdk.models.project.PullProgressEvent
import app.getarcane.sdk.models.project.UpdateIncludeFile
import app.getarcane.sdk.models.project.UpdateProject
import app.getarcane.sdk.pagination.PaginatedResponse
import app.getarcane.sdk.streaming.LogLine
import app.getarcane.sdk.streaming.logStream
import app.getarcane.sdk.streaming.ndjsonFlow
import kotlinx.coroutines.flow.Flow

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
    public suspend fun files(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/files"))

    /** Get the project's runtime service state. */
    public suspend fun runtime(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/runtime"))

    /** Get the project's image update summary. */
    public suspend fun updates(envId: EnvironmentId? = null, projectId: String): ProjectDetails =
        rest.get(rest.environmentPath(envId, "projects/$projectId/updates"))

    /** Get the contents of a single project-related file by relative path. */
    public suspend fun file(
        envId: EnvironmentId? = null,
        projectId: String,
        relativePath: String,
    ): IncludeFile =
        rest.get(
            rest.environmentPath(envId, "projects/$projectId/file"),
            listOf("relativePath" to relativePath),
        )

    // MARK: - Mutations

    /** Create a new Docker Compose project. */
    public suspend fun create(
        envId: EnvironmentId? = null,
        request: CreateProject,
    ): ProjectCreateResponse =
        rest.post(rest.environmentPath(envId, "projects"), body = request)

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
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/up"), body = options)
    }

    /** Bring down a project (docker compose down). */
    public suspend fun down(envId: EnvironmentId? = null, projectId: String) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/down"))
    }

    /** Redeploy a project (down + up). */
    public suspend fun redeploy(envId: EnvironmentId? = null, projectId: String) {
        rest.postVoid(rest.environmentPath(envId, "projects/$projectId/redeploy"))
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
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "projects/$projectId/redeploy"),
            PullProgressEvent.serializer(),
            method = "POST",
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
