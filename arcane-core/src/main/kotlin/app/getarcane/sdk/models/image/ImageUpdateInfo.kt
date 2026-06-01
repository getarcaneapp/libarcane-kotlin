package app.getarcane.sdk.models.image

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Image update availability info attached to image and container summaries. Mirrors Swift `ImageUpdateInfo` (Models/image/ImageUpdateInfo.swift). */
@Serializable
public data class ImageUpdateInfo(
    public val hasUpdate: Boolean,
    public val updateType: String,
    public val currentVersion: String,
    public val latestVersion: String,
    public val currentDigest: String,
    public val latestDigest: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val checkTime: Instant,
    public val responseTimeMs: Int,
    public val error: String,
    public val authMethod: String? = null,
    public val authUsername: String? = null,
    public val authRegistry: String? = null,
    public val usedCredential: Boolean? = null,
)
