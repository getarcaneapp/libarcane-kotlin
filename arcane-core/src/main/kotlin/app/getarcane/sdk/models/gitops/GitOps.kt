package app.getarcane.sdk.models.gitops

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A configured Git repository. */
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

/** Body for creating a Git repository. */
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

/** Body for updating a Git repository. */
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

/** A configured GitOps sync. */
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

/** Aggregate counts of GitOps syncs. */
@Serializable
public data class GitOpsSyncCounts(
    public val totalSyncs: Int,
    public val activeSyncs: Int,
    public val successfulSyncs: Int,
)

/** Body for creating a GitOps sync. */
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

/** Body for updating a GitOps sync. */
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

/** Result of a GitOps sync operation. */
@Serializable
public data class GitOpsSyncResult(
    public val success: Boolean,
    public val message: String,
    public val error: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val syncedAt: Instant,
)

/** Distinguishes file nodes from directory nodes in a GitOps file tree. */
@Serializable
public enum class GitOpsFileTreeNodeType(public val wire: String) {
    @SerialName("file")
    FILE("file"),

    @SerialName("directory")
    DIRECTORY("directory"),
}

/** A node in a GitOps file tree. The [children] field makes this a recursive tree node. */
@Serializable
public data class GitOpsFileTreeNode(
    public val name: String,
    public val path: String,
    public val type: GitOpsFileTreeNodeType,
    public val size: Long? = null,
    public val children: List<GitOpsFileTreeNode>? = null,
)

/** Response for browsing a GitOps repository path. */
@Serializable
public data class GitOpsBrowseResponse(
    public val path: String,
    public val files: List<GitOpsFileTreeNode>,
)

/** Information about a single Git branch. */
@Serializable
public data class GitOpsBranchInfo(
    public val name: String,
    public val isDefault: Boolean,
)

/** Response listing the branches of a repository. */
@Serializable
public data class GitOpsBranchesResponse(
    public val branches: List<GitOpsBranchInfo>,
)

/** Current status of a GitOps sync. */
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

/** A Git repository entry used when syncing repository configuration. */
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

/** Request body for syncing repository configuration. */
@Serializable
public data class GitRepositorySyncRequest(
    public val repositories: List<GitRepositorySync>,
)

/** Request body for importing a GitOps sync. */
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

/** Response for an import GitOps sync operation. */
@Serializable
public data class ImportGitOpsSyncResponse(
    public val successCount: Int,
    public val failedCount: Int,
    public val errors: List<String>,
)
