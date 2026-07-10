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
)
