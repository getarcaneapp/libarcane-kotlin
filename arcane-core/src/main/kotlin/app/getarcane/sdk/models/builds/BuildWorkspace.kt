package app.getarcane.sdk.models.builds

import kotlinx.serialization.Serializable

/**
 * Response body for `GET /environments/{id}/builds/browse/content`. On the wire [content] is a
 * Base64-encoded string, so it maps to [String] here.
 */
@Serializable
public data class BuildFileContent(
    /** Base64-encoded file bytes. */
    public val content: String,
    public val mimeType: String,
)
