package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.MultipartFile
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.multipartUpload
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.volume.BackupEntry
import app.getarcane.sdk.models.volume.BackupHasPath
import app.getarcane.sdk.models.volume.CreateVolume
import app.getarcane.sdk.models.volume.FileContent
import app.getarcane.sdk.models.volume.FileEntry
import app.getarcane.sdk.models.volume.RestoreBackupFilesRequest
import app.getarcane.sdk.models.volume.Volume
import app.getarcane.sdk.models.volume.VolumePruneReport
import app.getarcane.sdk.models.volume.VolumeSizeInfo
import app.getarcane.sdk.models.volume.VolumeUsage
import app.getarcane.sdk.models.volume.VolumeUsageCounts
import app.getarcane.sdk.pagination.PaginatedResponse

/**
 * Groups all volume, browse, and backup endpoints registered under `/environments/{id}/volumes`.
 */
public class VolumesService internal constructor(private val rest: RestService) {
    // MARK: - Volumes

    /** Paginated list of Docker volumes for the environment. */
    public suspend fun list(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        inUse: Boolean? = null,
        includeInternal: Boolean = false,
    ): PaginatedResponse<Volume> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            inUse?.let { add("inUse" to it.toString()) }
            if (includeInternal) add("includeInternal" to "true")
        }
        return rest.transport.paginated(
            rest.environmentPath(envId, "volumes"),
            query.start ?: 0,
            query.limit ?: 20,
            items,
        )
    }

    /** Get a single volume by name. */
    public suspend fun inspect(envId: EnvironmentId? = null, name: String): Volume =
        rest.get(rest.environmentPath(envId, "volumes/$name"))

    /** Create a new Docker volume. */
    public suspend fun create(envId: EnvironmentId? = null, request: CreateVolume): Volume =
        rest.post(rest.environmentPath(envId, "volumes"), body = request)

    /** Remove a volume, optionally forcing removal even if it is in use. */
    public suspend fun remove(envId: EnvironmentId? = null, name: String, force: Boolean = false) {
        val items = buildList { if (force) add("force" to "true") }
        rest.deleteVoid(rest.environmentPath(envId, "volumes/$name"), query = items)
    }

    /** Prune unused volumes and return the prune report. */
    public suspend fun prune(envId: EnvironmentId? = null): VolumePruneReport =
        rest.post(rest.environmentPath(envId, "volumes/prune"))

    /** Get container usage information for a single volume. */
    public suspend fun usage(envId: EnvironmentId? = null, name: String): VolumeUsage =
        rest.get(rest.environmentPath(envId, "volumes/$name/usage"))

    /** Get aggregate usage counts (in use / unused / total). */
    public suspend fun counts(envId: EnvironmentId? = null, includeInternal: Boolean = false): VolumeUsageCounts {
        val items = buildList { if (includeInternal) add("includeInternal" to "true") }
        return rest.get(rest.environmentPath(envId, "volumes/counts"), items)
    }

    /** Compute disk usage sizes for all volumes (slow). */
    public suspend fun sizes(envId: EnvironmentId? = null): List<VolumeSizeInfo> =
        rest.get(rest.environmentPath(envId, "volumes/sizes"))

    // MARK: - Browse

    /** List directory entries inside a volume. */
    public suspend fun browse(envId: EnvironmentId? = null, name: String, path: String = "/"): List<FileEntry> =
        rest.get(
            rest.environmentPath(envId, "volumes/$name/browse"),
            query = listOf("path" to path),
        )

    /** Get a preview of a file in a volume. */
    public suspend fun fileContent(
        envId: EnvironmentId? = null,
        name: String,
        path: String,
        maxBytes: Long = 1_048_576,
    ): FileContent =
        rest.get(
            rest.environmentPath(envId, "volumes/$name/browse/content"),
            query = listOf("path" to path, "maxBytes" to maxBytes.toString()),
        )

    /** Create a new directory inside a volume. */
    public suspend fun createDirectory(envId: EnvironmentId? = null, name: String, path: String) {
        rest.postVoid(
            rest.environmentPath(envId, "volumes/$name/browse/mkdir"),
            query = listOf("path" to path),
        )
    }

    /** Delete a file or directory inside a volume. */
    public suspend fun deleteFile(envId: EnvironmentId? = null, name: String, path: String) {
        rest.deleteVoid(
            rest.environmentPath(envId, "volumes/$name/browse"),
            query = listOf("path" to path),
        )
    }

    // MARK: - Backups

    /** Paginated list of backups for a volume. */
    public suspend fun listBackups(
        envId: EnvironmentId? = null,
        name: String,
        query: SearchPaginationSort = SearchPaginationSort(),
    ): PaginatedResponse<BackupEntry> =
        rest.transport.paginated(
            rest.environmentPath(envId, "volumes/$name/backups"),
            query.start ?: 0,
            query.limit ?: 20,
            query.nonPaginationQueryItems,
        )

    /** Create a new backup of a volume. */
    public suspend fun createBackup(envId: EnvironmentId? = null, name: String): BackupEntry =
        rest.post(rest.environmentPath(envId, "volumes/$name/backups"))

    /** Restore an entire backup over a volume. */
    public suspend fun restoreBackup(envId: EnvironmentId? = null, name: String, backupId: String) {
        rest.postVoid(rest.environmentPath(envId, "volumes/$name/backups/$backupId/restore"))
    }

    /** Restore selected files from a backup. */
    public suspend fun restoreBackupFiles(
        envId: EnvironmentId? = null,
        name: String,
        backupId: String,
        paths: List<String>,
    ) {
        rest.postVoid(
            rest.environmentPath(envId, "volumes/$name/backups/$backupId/restore-files"),
            body = RestoreBackupFilesRequest(paths = paths),
        )
    }

    /** Delete a backup. */
    public suspend fun deleteBackup(envId: EnvironmentId? = null, backupId: String) {
        rest.deleteVoid(rest.environmentPath(envId, "volumes/backups/$backupId"))
    }

    /** Check whether a backup contains the given path. */
    public suspend fun backupHasPath(
        envId: EnvironmentId? = null,
        backupId: String,
        path: String,
    ): BackupHasPath =
        rest.get(
            rest.environmentPath(envId, "volumes/backups/$backupId/has-path"),
            query = listOf("path" to path),
        )

    /** List the files contained inside a backup. */
    public suspend fun listBackupFiles(envId: EnvironmentId? = null, backupId: String): List<String> =
        rest.get(rest.environmentPath(envId, "volumes/backups/$backupId/files"))

    // MARK: - Binary uploads & downloads

    /** Upload a file or directory into a volume at [path] (multipart upload). */
    public suspend fun uploadFile(
        envId: EnvironmentId? = null,
        name: String,
        path: String,
        content: ByteArray,
        filename: String,
    ) {
        val part = MultipartFile(fieldName = "file", filename = filename, content = content)
        rest.transport.multipartUpload(
            rest.environmentPath(envId, "volumes/$name/browse/upload"),
            MessageResponse.serializer(),
            files = listOf(part),
            query = listOf("path" to path),
        )
    }

    /** Upload a backup tarball to a volume. */
    public suspend fun uploadBackup(
        envId: EnvironmentId? = null,
        name: String,
        content: ByteArray,
        filename: String,
    ): BackupEntry {
        val part = MultipartFile(fieldName = "backup", filename = filename, content = content)
        return rest.transport.multipartUpload(
            rest.environmentPath(envId, "volumes/$name/backups/upload"),
            BackupEntry.serializer(),
            files = listOf(part),
        )
    }

    /** Download the raw bytes of a file inside a volume. */
    public suspend fun downloadFile(envId: EnvironmentId? = null, name: String, path: String): ByteArray =
        rest.transport.downloadRaw(
            rest.environmentPath(envId, "volumes/$name/browse/download"),
            query = listOf("path" to path),
        )

    /** Download a backup tarball as raw bytes. */
    public suspend fun downloadBackup(envId: EnvironmentId? = null, backupId: String): ByteArray =
        rest.transport.downloadRaw(rest.environmentPath(envId, "volumes/backups/$backupId/download"))
}
