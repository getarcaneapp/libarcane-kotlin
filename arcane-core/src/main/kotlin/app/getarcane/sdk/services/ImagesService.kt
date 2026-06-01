package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.MultipartFile
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.multipartUpload
import app.getarcane.sdk.http.multipartUploadNdjson
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.containerregistry.ContainerRegistryCredential
import app.getarcane.sdk.models.image.ImageBuildRecord
import app.getarcane.sdk.models.image.ImageBuildRequest
import app.getarcane.sdk.models.image.ImageDetailSummary
import app.getarcane.sdk.models.image.ImageListResponse
import app.getarcane.sdk.models.image.ImageLoadResult
import app.getarcane.sdk.models.image.ImageProgressEvent
import app.getarcane.sdk.models.image.ImagePruneReport
import app.getarcane.sdk.models.image.ImagePullOptions
import app.getarcane.sdk.models.image.ImageSummary
import app.getarcane.sdk.models.image.ImageUpdateInfo
import app.getarcane.sdk.models.image.ImageUsageCounts
import app.getarcane.sdk.models.imageupdate.BatchImageUpdateRequest
import app.getarcane.sdk.models.imageupdate.CheckAllImagesRequest
import app.getarcane.sdk.models.imageupdate.ImageUpdateBatchResponse
import app.getarcane.sdk.models.imageupdate.ImageUpdateResponse
import app.getarcane.sdk.models.imageupdate.ImageUpdateSummary
import app.getarcane.sdk.models.project.PullProgressEvent
import app.getarcane.sdk.pagination.PaginatedResponse
import app.getarcane.sdk.streaming.ndjsonFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Request body for the image prune endpoint. Mirrors the Swift `ImagesService.prune` local `Body`. */
@Serializable
private data class PruneBody(
    val mode: String? = null,
    val until: String? = null,
    val dangling: Boolean? = null,
    val filters: Map<String, List<String>>? = null,
)

/** Image listing, pull/build streaming, upload, and update checks. Port of Swift `ImagesService`. */
public class ImagesService internal constructor(private val rest: RestService) {
    // MARK: - Listing

    /** Paginated list of images in an environment. */
    public suspend fun list(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        inUse: String? = null,
        updates: String? = null,
    ): ImageListResponse {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            inUse?.let { add("inUse" to it) }
            updates?.let { add("updates" to it) }
        }
        val response: PaginatedResponse<ImageSummary> =
            rest.transport.paginated<ImageSummary>(
                rest.environmentPath(envId, "images"),
                query.start ?: 0,
                query.limit ?: 20,
                items,
            )
        return ImageListResponse(
            success = response.success,
            data = response.data,
            pagination = response.pagination,
        )
    }

    /** Aggregate counts of images (in use / unused / total / total size). */
    public suspend fun usageCounts(envId: EnvironmentId? = null): ImageUsageCounts =
        rest.get(rest.environmentPath(envId, "images/counts"))

    /** Inspect a single image by ID. */
    public suspend fun inspect(envId: EnvironmentId? = null, id: String): ImageDetailSummary =
        rest.get(rest.environmentPath(envId, "images/$id"))

    /** Remove an image, optionally forcing removal even if in use. */
    public suspend fun remove(envId: EnvironmentId? = null, id: String, force: Boolean = false) {
        rest.deleteVoid(rest.environmentPath(envId, "images/$id"), query = listOf("force" to force.toString()))
    }

    // MARK: - Pull / Build (streaming endpoints)

    /**
     * Initiate an image pull and resolve once the server has accepted the request. Use [pullStream]
     * for progress.
     */
    public suspend fun pull(envId: EnvironmentId? = null, options: ImagePullOptions) {
        rest.postVoid(rest.environmentPath(envId, "images/pull"), body = options)
    }

    /** Initiate an image pull and stream NDJSON progress frames as they arrive. */
    public fun pullStream(
        envId: EnvironmentId? = null,
        options: ImagePullOptions,
    ): Flow<PullProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "images/pull"),
            PullProgressEvent.serializer(),
            method = "POST",
            body = options,
        )

    /** Initiate an image build. Use [buildStream] for progress. */
    public suspend fun build(envId: EnvironmentId? = null, request: ImageBuildRequest) {
        rest.postVoid(rest.environmentPath(envId, "images/build"), body = request)
    }

    /** Initiate an image build and stream NDJSON progress frames as they arrive. */
    public fun buildStream(
        envId: EnvironmentId? = null,
        request: ImageBuildRequest,
    ): Flow<ImageProgressEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "images/build"),
            ImageProgressEvent.serializer(),
            method = "POST",
            body = request,
        )

    // MARK: - Upload

    /**
     * Upload a Docker image tarball as a multipart file. Returns the result of loading the image
     * into the local registry.
     */
    public suspend fun upload(
        envId: EnvironmentId? = null,
        content: ByteArray,
        filename: String,
        fieldName: String = "image",
    ): ImageLoadResult {
        val part = MultipartFile(fieldName = fieldName, filename = filename, content = content)
        return rest.transport.multipartUpload(
            rest.environmentPath(envId, "images/upload"),
            ImageLoadResult.serializer(),
            files = listOf(part),
        )
    }

    /** Upload a Docker image tarball and stream NDJSON progress frames as the server loads the layers. */
    public fun uploadStream(
        envId: EnvironmentId? = null,
        content: ByteArray,
        filename: String,
        fieldName: String = "image",
    ): Flow<ImageProgressEvent> {
        val part = MultipartFile(fieldName = fieldName, filename = filename, content = content)
        return rest.transport.multipartUploadNdjson(
            rest.environmentPath(envId, "images/upload"),
            ImageProgressEvent.serializer(),
            files = listOf(part),
        )
    }

    // MARK: - Build history

    /** Paginated list of historical builds for this environment. */
    public suspend fun listBuilds(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        status: String? = null,
        provider: String? = null,
    ): PaginatedResponse<ImageBuildRecord> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            status?.let { add("status" to it) }
            provider?.let { add("provider" to it) }
        }
        return rest.transport.paginated<ImageBuildRecord>(
            rest.environmentPath(envId, "images/builds"),
            query.start ?: 0,
            query.limit ?: 20,
            items,
        )
    }

    /** Inspect a single build history entry. */
    public suspend fun getBuild(envId: EnvironmentId? = null, buildId: String): ImageBuildRecord =
        rest.get(rest.environmentPath(envId, "images/builds/$buildId"))

    // MARK: - Prune

    /** Prune unused images. [mode] is typically "all" or "dangling". */
    public suspend fun prune(
        envId: EnvironmentId? = null,
        mode: String? = null,
        until: String? = null,
        dangling: Boolean? = null,
        filters: Map<String, List<String>>? = null,
    ): ImagePruneReport {
        val body = PruneBody(mode = mode, until = until, dangling = dangling, filters = filters)
        return rest.post(rest.environmentPath(envId, "images/prune"), body = body)
    }

    // MARK: - Image updates

    /** Check whether an update is available for an image referenced by repo:tag. */
    public suspend fun checkUpdateByRef(
        envId: EnvironmentId? = null,
        imageRef: String,
    ): ImageUpdateResponse =
        rest.get(rest.environmentPath(envId, "image-updates/check"), listOf("imageRef" to imageRef))

    /** Check whether an update is available for an image by its Docker image ID (GET). */
    public suspend fun checkUpdateByID(
        envId: EnvironmentId? = null,
        imageId: String,
    ): ImageUpdateResponse =
        rest.get(rest.environmentPath(envId, "image-updates/check/$imageId"))

    /**
     * Check whether an update is available for an image by its Docker image ID (POST). The POST
     * variant exists for clients that prefer non-idempotent semantics.
     */
    public suspend fun checkUpdateByIDPost(
        envId: EnvironmentId? = null,
        imageId: String,
    ): ImageUpdateResponse =
        rest.post(rest.environmentPath(envId, "image-updates/check/$imageId"))

    /** Batch update check for a list of image references. */
    public suspend fun checkBatchUpdates(
        envId: EnvironmentId? = null,
        imageRefs: List<String>,
        credentials: List<ContainerRegistryCredential>? = null,
    ): ImageUpdateBatchResponse {
        val body = BatchImageUpdateRequest(imageRefs = imageRefs, credentials = credentials)
        return rest.post(rest.environmentPath(envId, "image-updates/check-batch"), body = body)
    }

    /** Check updates for all images in the environment. */
    public suspend fun checkAllUpdates(
        envId: EnvironmentId? = null,
        credentials: List<ContainerRegistryCredential>? = null,
    ): ImageUpdateBatchResponse {
        val body = CheckAllImagesRequest(credentials = credentials)
        return rest.post(rest.environmentPath(envId, "image-updates/check-all"), body = body)
    }

    /** Look up persisted update info for a set of image references. */
    public suspend fun updateInfoByRefs(
        envId: EnvironmentId? = null,
        imageRefs: List<String>,
    ): Map<String, ImageUpdateInfo?> {
        val joined = imageRefs.joinToString(",")
        return rest.get(rest.environmentPath(envId, "image-updates/by-refs"), listOf("imageRefs" to joined))
    }

    /** Aggregate summary of image updates for the environment. */
    public suspend fun updateSummary(envId: EnvironmentId? = null): ImageUpdateSummary =
        rest.get(rest.environmentPath(envId, "image-updates/summary"))
}
