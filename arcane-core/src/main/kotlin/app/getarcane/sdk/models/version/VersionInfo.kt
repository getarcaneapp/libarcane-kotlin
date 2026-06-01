package app.getarcane.sdk.models.version

import kotlinx.serialization.Serializable

/**
 * Detailed application version information returned by `/app-version` and
 * `/environments/{id}/version`. Mirrors Swift `VersionInfo` (Models/Version/VersionInfo.swift).
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
)

/** Simplified version-check response from `/version`. Mirrors Swift `VersionCheck`. */
@Serializable
public data class VersionCheck(
    public val currentVersion: String,
    public val newestVersion: String? = null,
    public val updateAvailable: Boolean,
    public val releaseUrl: String? = null,
)
