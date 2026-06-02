package app.getarcane.sdk.models.volume

import app.getarcane.sdk.models.base.PaginationResponse
import kotlinx.serialization.Serializable

/** A single volume backup record. */
@Serializable
public data class BackupEntry(
    public val id: String,
    public val volumeName: String,
    public val size: Long,
    public val createdAt: String,
)

/** The page envelope returned by `GET volumes/{name}/backups`. */
@Serializable
public data class VolumeBackupListPage(
    public val success: Boolean,
    public val data: List<BackupEntry>,
    public val pagination: PaginationResponse,
    public val warnings: List<String>? = null,
)

/** The response for the `has-path` lookup. */
@Serializable
public data class BackupHasPath(
    public val exists: Boolean,
)

/** Body for the partial restore endpoint. */
@Serializable
public data class RestoreBackupFilesRequest(
    public val paths: List<String>,
)
