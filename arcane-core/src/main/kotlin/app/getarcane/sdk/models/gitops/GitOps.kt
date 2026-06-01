package app.getarcane.sdk.models.gitops

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors Swift `GitRepository`. */
@Serializable
public data class GitRepository(
    public val id: String,
    public val name: String,
    public val url: String,
    public val authType: String,
    public val username: String? = null,
    public val sshHostKeyVerification: String? = null,
    public val description: String? = null,
    public val enabled: Boolean,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/** Mirrors Swift `CreateGitRepository`. */
@Serializable
public data class CreateGitRepository(
    public val name: String,
    public val url: String,
    public val authType: String,
    public val username: String? = null,
    public val token: String? = null,
    public val sshKey: String? = null,
    public val sshHostKeyVerification: String? = null,
    public val description: String? = null,
    public val enabled: Boolean? = null,
)

/** Mirrors Swift `UpdateGitRepository`. */
@Serializable
public data class UpdateGitRepository(
    public val name: String? = null,
    public val url: String? = null,
    public val authType: String? = null,
    public val username: String? = null,
    public val token: String? = null,
    public val sshKey: String? = null,
    public val sshHostKeyVerification: String? = null,
    public val description: String? = null,
    public val enabled: Boolean? = null,
)

/** Mirrors Swift `GitOpsSync`. */
@Serializable
public data class GitOpsSync(
    public val id: String,
    public val name: String,
    public val environmentId: String,
    public val repositoryId: String,
    public val repository: GitRepository? = null,
    public val branch: String,
    public val composePath: String,
    public val targetType: String,
    public val projectName: String,
    public val projectId: String? = null,
    public val autoSync: Boolean,
    public val syncInterval: Int,
    public val syncDirectory: Boolean,
    public val syncedFiles: String? = null,
    public val maxSyncFiles: Int,
    public val maxSyncTotalSize: Long,
    public val maxSyncBinarySize: Long,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastSyncAt: Instant? = null,
    public val lastSyncStatus: String? = null,
    public val lastSyncError: String? = null,
    public val lastSyncCommit: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/** Mirrors Swift `GitOpsSyncCounts`. */
@Serializable
public data class GitOpsSyncCounts(
    public val totalSyncs: Int,
    public val activeSyncs: Int,
    public val successfulSyncs: Int,
)

/** Mirrors Swift `CreateGitOpsSync`. */
@Serializable
public data class CreateGitOpsSync(
    public val name: String,
    public val repositoryId: String,
    public val branch: String,
    public val composePath: String,
    public val targetType: String? = null,
    public val projectName: String? = null,
    public val autoSync: Boolean? = null,
    public val syncInterval: Int? = null,
    public val syncDirectory: Boolean? = null,
    public val maxSyncFiles: Int? = null,
    public val maxSyncTotalSize: Long? = null,
    public val maxSyncBinarySize: Long? = null,
)

/** Mirrors Swift `UpdateGitOpsSync`. */
@Serializable
public data class UpdateGitOpsSync(
    public val name: String? = null,
    public val repositoryId: String? = null,
    public val branch: String? = null,
    public val composePath: String? = null,
    public val targetType: String? = null,
    public val projectName: String? = null,
    public val autoSync: Boolean? = null,
    public val syncInterval: Int? = null,
    public val syncDirectory: Boolean? = null,
    public val maxSyncFiles: Int? = null,
    public val maxSyncTotalSize: Long? = null,
    public val maxSyncBinarySize: Long? = null,
)

/** Mirrors Swift `GitOpsSyncResult`. */
@Serializable
public data class GitOpsSyncResult(
    public val success: Boolean,
    public val message: String,
    public val error: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val syncedAt: Instant,
)

/** Mirrors Swift `GitOpsFileTreeNodeType`. */
@Serializable
public enum class GitOpsFileTreeNodeType(public val wire: String) {
    @SerialName("file")
    FILE("file"),

    @SerialName("directory")
    DIRECTORY("directory"),
}

/** Mirrors Swift `GitOpsFileTreeNode`. The [children] field makes this a recursive tree node. */
@Serializable
public data class GitOpsFileTreeNode(
    public val name: String,
    public val path: String,
    public val type: GitOpsFileTreeNodeType,
    public val size: Long? = null,
    public val children: List<GitOpsFileTreeNode>? = null,
)

/** Mirrors Swift `GitOpsBrowseResponse`. */
@Serializable
public data class GitOpsBrowseResponse(
    public val path: String,
    public val files: List<GitOpsFileTreeNode>,
)

/** Mirrors Swift `GitOpsBranchInfo`. */
@Serializable
public data class GitOpsBranchInfo(
    public val name: String,
    public val isDefault: Boolean,
)

/** Mirrors Swift `GitOpsBranchesResponse`. */
@Serializable
public data class GitOpsBranchesResponse(
    public val branches: List<GitOpsBranchInfo>,
)

/** Mirrors Swift `GitOpsSyncStatus`. */
@Serializable
public data class GitOpsSyncStatus(
    public val id: String,
    public val autoSync: Boolean,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val nextSyncAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastSyncAt: Instant? = null,
    public val lastSyncStatus: String? = null,
    public val lastSyncError: String? = null,
    public val lastSyncCommit: String? = null,
)

/** Mirrors Swift `GitRepositorySync`. */
@Serializable
public data class GitRepositorySync(
    public val id: String,
    public val name: String,
    public val url: String,
    public val authType: String,
    public val username: String? = null,
    public val token: String? = null,
    public val sshKey: String? = null,
    public val sshHostKeyVerification: String? = null,
    public val description: String? = null,
    public val enabled: Boolean,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/** Mirrors Swift `GitRepositorySyncRequest`. */
@Serializable
public data class GitRepositorySyncRequest(
    public val repositories: List<GitRepositorySync>,
)

/** Mirrors Swift `ImportGitOpsSyncRequest`. */
@Serializable
public data class ImportGitOpsSyncRequest(
    public val syncName: String,
    public val gitRepo: String,
    public val branch: String,
    public val dockerComposePath: String,
    public val autoSync: Boolean,
    public val syncInterval: Int,
    public val syncDirectory: Boolean? = null,
    public val maxSyncFiles: Int? = null,
    public val maxSyncTotalSize: Long? = null,
    public val maxSyncBinarySize: Long? = null,
)

/** Mirrors Swift `ImportGitOpsSyncResponse`. */
@Serializable
public data class ImportGitOpsSyncResponse(
    public val successCount: Int,
    public val failedCount: Int,
    public val errors: List<String>,
)
