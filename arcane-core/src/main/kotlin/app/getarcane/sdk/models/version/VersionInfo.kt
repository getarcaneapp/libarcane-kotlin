package app.getarcane.sdk.models.version

import kotlinx.serialization.Serializable

/**
 * Detailed application version information returned by `/app-version` and
 * `/environments/{id}/version`.
 */
@Serializable
public data class VersionInfo(
    public val currentVersion: String,
    public val currentTag: String? = null,
    public val currentDigest: String? = null,
    public val revision: String,
    public val shortRevision: String,
    public val goVersion: String,
    public val enabledFeatures: List<String>? = null,
    public val buildTime: String? = null,
    public val displayVersion: String,
    public val isSemverVersion: Boolean,
    public val newestVersion: String? = null,
    public val newestDigest: String? = null,
    public val updateAvailable: Boolean,
    public val releaseUrl: String? = null,
    public val releaseNotes: String? = null,
    public val releasedAt: String? = null,
) {
    /** Whether the connected Arcane version supports mobile features introduced in 2.7.0. */
    public val supportsPost26MobileFeatures: Boolean
        get() = isSemverVersion &&
            currentVersion.toSemanticVersion()?.let { it >= POST_26_VERSION } == true
}

private data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)
}

private val POST_26_VERSION = SemanticVersion(major = 2, minor = 7, patch = 0)

private fun String.toSemanticVersion(): SemanticVersion? {
    val normalized = trim().removePrefix("v").substringBefore('+').substringBefore('-')
    val components = normalized.split('.')
    if (components.size != 3) return null
    val major = components[0].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val minor = components[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    val patch = components[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
    return SemanticVersion(major, minor, patch)
}

/** Simplified version-check response from `/version`. */
@Serializable
public data class VersionCheck(
    public val currentVersion: String,
    public val newestVersion: String? = null,
    public val updateAvailable: Boolean,
    public val releaseUrl: String? = null,
)
