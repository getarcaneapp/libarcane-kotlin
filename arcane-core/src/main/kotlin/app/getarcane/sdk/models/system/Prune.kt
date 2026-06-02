package app.getarcane.sdk.models.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Selects which containers a prune operation should remove. */
@Serializable
public enum class PruneContainerMode(public val wire: String) {
    @SerialName("none")
    NONE("none"),

    @SerialName("stopped")
    STOPPED("stopped"),

    @SerialName("olderThan")
    OLDER_THAN("olderThan"),
}

/** Selects which images a prune operation should remove. */
@Serializable
public enum class PruneImageMode(public val wire: String) {
    @SerialName("none")
    NONE("none"),

    @SerialName("dangling")
    DANGLING("dangling"),

    @SerialName("all")
    ALL("all"),

    @SerialName("olderThan")
    OLDER_THAN("olderThan"),
}

/** Selects which volumes a prune operation should remove. */
@Serializable
public enum class PruneVolumeMode(public val wire: String) {
    @SerialName("none")
    NONE("none"),

    @SerialName("anonymous")
    ANONYMOUS("anonymous"),

    @SerialName("all")
    ALL("all"),
}

/** Selects which networks a prune operation should remove. */
@Serializable
public enum class PruneNetworkMode(public val wire: String) {
    @SerialName("none")
    NONE("none"),

    @SerialName("unused")
    UNUSED("unused"),

    @SerialName("olderThan")
    OLDER_THAN("olderThan"),
}

/** Selects which build cache entries a prune operation should remove. */
@Serializable
public enum class PruneBuildCacheMode(public val wire: String) {
    @SerialName("none")
    NONE("none"),

    @SerialName("unused")
    UNUSED("unused"),

    @SerialName("all")
    ALL("all"),

    @SerialName("olderThan")
    OLDER_THAN("olderThan"),
}

/** Options controlling a container prune operation. */
@Serializable
public data class PruneContainersOptions(
    public val mode: PruneContainerMode,
    public val until: String? = null,
)

/** Options controlling an image prune operation. */
@Serializable
public data class PruneImagesOptions(
    public val mode: PruneImageMode,
    public val until: String? = null,
)

/** Options controlling a volume prune operation. */
@Serializable
public data class PruneVolumesOptions(
    public val mode: PruneVolumeMode,
)

/** Options controlling a network prune operation. */
@Serializable
public data class PruneNetworksOptions(
    public val mode: PruneNetworkMode,
    public val until: String? = null,
)

/** Options controlling a build cache prune operation. */
@Serializable
public data class PruneBuildCacheOptions(
    public val mode: PruneBuildCacheMode,
    public val until: String? = null,
)

/** Body for `POST /environments/{id}/system/prune`. */
@Serializable
public data class PruneAllRequest(
    public val containers: PruneContainersOptions? = null,
    public val images: PruneImagesOptions? = null,
    public val volumes: PruneVolumesOptions? = null,
    public val networks: PruneNetworksOptions? = null,
    public val buildCache: PruneBuildCacheOptions? = null,
)

/**
 * Result of a system prune. Byte counters are represented as [Long] to match the SDK's JSON number
 * handling.
 */
@Serializable
public data class PruneAllResult(
    public val containersPruned: List<String>? = null,
    public val imagesDeleted: List<String>? = null,
    public val volumesDeleted: List<String>? = null,
    public val networksDeleted: List<String>? = null,
    public val spaceReclaimed: Long = 0,
    public val containerSpaceReclaimed: Long? = null,
    public val imageSpaceReclaimed: Long? = null,
    public val volumeSpaceReclaimed: Long? = null,
    public val buildCacheSpaceReclaimed: Long? = null,
    public val success: Boolean = false,
    public val errors: List<String>? = null,
)
