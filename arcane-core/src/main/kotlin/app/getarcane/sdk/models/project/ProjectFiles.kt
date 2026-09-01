package app.getarcane.sdk.models.project

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An editable file or directory within a project directory. */
@Serializable
public data class ProjectFile(
    public val path: String,
    public val relativePath: String,
    public val name: String,
    public val isDirectory: Boolean,
    public val size: Long = 0,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val modTime: Instant? = null,
    public val protected: Boolean? = null,
    public val content: String? = null,
    public val mode: String? = null,
    public val linkTarget: String? = null,
    public val isSymlink: Boolean = false,
    public val editable: Boolean = false,
    public val readOnlyReason: String? = null,
)

/** The current project workspace file tree and its optimistic-concurrency revision. */
@Serializable
public data class ProjectWorkspace(
    public val files: List<ProjectFile>,
    public val fileTreeRevision: String,
    public val fileTreeTruncated: Boolean = false,
    public val activityId: String? = null,
)

/** Text content and editability metadata for one project workspace file. */
@Serializable
public data class ProjectWorkspaceFileContent(
    public val path: String,
    public val relativePath: String,
    public val name: String,
    public val content: String? = null,
    public val mimeType: String,
    public val size: Long = 0,
    public val editable: Boolean = false,
    public val readOnlyReason: String? = null,
)

/** A file or directory staged while creating a project. */
@Serializable
public data class ProjectFileDraft(
    public val relativePath: String,
    public val isDirectory: Boolean = false,
    public val content: String? = null,
)

/** Operations accepted by Arcane's project file-management API. */
@Serializable
public enum class ProjectFileChangeOperation(public val wire: String) {
    @SerialName("create_file")
    CREATE_FILE("create_file"),

    @SerialName("create_folder")
    CREATE_FOLDER("create_folder"),

    @SerialName("update_file")
    UPDATE_FILE("update_file"),

    @SerialName("rename")
    RENAME("rename"),

    @SerialName("move")
    MOVE("move"),

    @SerialName("delete")
    DELETE("delete"),
}

/** One staged file-tree operation for a project update. */
@Serializable
public data class ProjectFileChange(
    public val operation: ProjectFileChangeOperation,
    public val relativePath: String,
    public val newName: String? = null,
    public val newParentPath: String? = null,
    public val content: String? = null,
    public val recursive: Boolean? = null,
    /** Original content used to reject a concurrent edit to an updated file. */
    public val baselineContent: String? = null,
)

@Serializable
internal data class ProjectWorkspaceUpdateManifest(
    val fileTreeRevision: String,
    val fileChanges: List<ProjectWorkspaceManifestChange>,
)

@Serializable
internal data class CreateProjectWorkspaceManifest(
    val fileChanges: List<ProjectWorkspaceManifestChange>,
)

@Serializable
internal data class ProjectWorkspaceManifestChange(
    val operation: ProjectFileChangeOperation,
    val relativePath: String,
    val newName: String? = null,
    val newParentPath: String? = null,
    val uploadIndex: Int? = null,
    val baselineIndex: Int? = null,
    val recursive: Boolean? = null,
)
