package app.getarcane.sdk.models.image

import kotlinx.serialization.Serializable

/** One entry returned by Docker's image layer-history endpoint. */
@Serializable
public data class ImageHistoryItem(
    /** Docker layer ID, or the server's `<missing>` marker for metadata-only entries. */
    public val id: String = MISSING_LAYER_ID,
    /** Unix epoch seconds. Zero means the creation time was not supplied. */
    public val created: Long = 0,
    /** Dockerfile-compatible command recorded for this entry. */
    public val createdBy: String = "",
    public val tags: List<String> = emptyList(),
    /** Layer size in bytes. */
    public val size: Long = 0,
    public val comment: String = "",
) {
    public val isMissingLayer: Boolean get() = id.isBlank() || id == MISSING_LAYER_ID

    public companion object {
        public const val MISSING_LAYER_ID: String = "<missing>"
    }
}
