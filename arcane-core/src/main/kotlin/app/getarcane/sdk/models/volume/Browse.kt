package app.getarcane.sdk.models.volume

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** A single file or directory inside a volume. */
@Serializable
public data class FileEntry(
    public val name: String,
    public val path: String,
    public val isDirectory: Boolean,
    public val size: Long,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val modTime: Instant,
    public val mode: String,
    public val isSymlink: Boolean = false,
    public val linkTarget: String? = null,
)

/** Extends [FileEntry] with MIME and text/binary hints. */
@Serializable
public data class FileMetadata(
    public val name: String,
    public val path: String,
    public val isDirectory: Boolean,
    public val size: Long,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val modTime: Instant,
    public val mode: String,
    public val isSymlink: Boolean = false,
    public val linkTarget: String? = null,
    public val mimeType: String,
    public val isText: Boolean,
    public val isBinary: Boolean,
)

/**
 * The response payload returned by `GET volumes/{name}/browse/content`. [content] holds the
 * Base64-encoded file bytes.
 */
@Serializable
public data class FileContent(
    /** Base64-encoded file bytes. */
    public val content: String,
    public val mimeType: String,
)
