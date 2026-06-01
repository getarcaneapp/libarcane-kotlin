package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.MultipartFile
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.multipartUpload
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.models.builds.BuildFileContent
import app.getarcane.sdk.models.volume.FileEntry

/** Browses files in the build workspace for an environment. Port of Swift `BuildsService`. */
public class BuildsService internal constructor(private val rest: RestService) {
    /** List files and directories under the builds workspace root. */
    public suspend fun browse(path: String = "/", envId: EnvironmentId? = null): List<FileEntry> =
        rest.get(rest.environmentPath(envId, "builds/browse"), listOf("path" to path))

    /** Read file content under the builds workspace root. */
    public suspend fun getFileContent(
        path: String,
        maxBytes: Long = 1_048_576,
        envId: EnvironmentId? = null,
    ): BuildFileContent =
        rest.get(
            rest.environmentPath(envId, "builds/browse/content"),
            listOf("path" to path, "maxBytes" to maxBytes.toString()),
        )

    /** Create a directory under the builds workspace root. */
    public suspend fun createDirectory(path: String, envId: EnvironmentId? = null) {
        rest.postVoid(rest.environmentPath(envId, "builds/browse/mkdir"), query = listOf("path" to path))
    }

    /** Delete a file or directory under the builds workspace root. */
    public suspend fun delete(path: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "builds/browse"), query = listOf("path" to path))
    }

    /** Upload a file into the build workspace at [path]. */
    public suspend fun upload(
        path: String,
        content: ByteArray,
        filename: String,
        envId: EnvironmentId? = null,
    ) {
        val part = MultipartFile(fieldName = "file", filename = filename, content = content)
        rest.transport.multipartUpload(
            rest.environmentPath(envId, "builds/browse/upload"),
            MessageResponse.serializer(),
            files = listOf(part),
            query = listOf("path" to path),
        )
    }

    /** Download the raw bytes of a file in the build workspace. */
    public suspend fun downloadFile(path: String, envId: EnvironmentId? = null): ByteArray =
        rest.transport.downloadRaw(
            rest.environmentPath(envId, "builds/browse/download"),
            query = listOf("path" to path),
        )
}
