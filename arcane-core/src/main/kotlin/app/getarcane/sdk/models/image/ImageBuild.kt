package app.getarcane.sdk.models.image

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Options for building an image with BuildKit. */
@Serializable
public data class ImageBuildRequest(
    public val contextDir: String,
    public val dockerfile: String? = null,
    public val dockerfileInline: String? = null,
    public val tags: List<String>? = null,
    public val target: String? = null,
    public val buildArgs: Map<String, String>? = null,
    public val labels: Map<String, String>? = null,
    public val cacheFrom: List<String>? = null,
    public val cacheTo: List<String>? = null,
    public val noCache: Boolean? = null,
    public val pull: Boolean? = null,
    public val network: String? = null,
    public val isolation: String? = null,
    public val shmSize: Long? = null,
    public val ulimits: Map<String, String>? = null,
    public val entitlements: List<String>? = null,
    public val privileged: Boolean? = null,
    public val extraHosts: List<String>? = null,
    public val platforms: List<String>? = null,
    public val push: Boolean? = null,
    public val load: Boolean? = null,
    public val provider: String? = null,
)

/** Result of an image build. */
@Serializable
public data class ImageBuildResult(
    public val provider: String,
    public val tags: List<String>? = null,
    public val digest: String? = null,
)

/** A historical image build entry. */
@Serializable
public data class ImageBuildRecord(
    public val id: String,
    public val environmentId: String,
    public val userId: String? = null,
    public val username: String? = null,
    public val status: String,
    public val provider: String? = null,
    public val contextDir: String,
    public val dockerfile: String? = null,
    public val target: String? = null,
    public val tags: List<String>? = null,
    public val platforms: List<String>? = null,
    public val buildArgs: Map<String, String>? = null,
    public val labels: Map<String, String>? = null,
    public val cacheFrom: List<String>? = null,
    public val cacheTo: List<String>? = null,
    public val noCache: Boolean,
    public val pull: Boolean,
    public val network: String? = null,
    public val isolation: String? = null,
    public val shmSize: Long? = null,
    public val ulimits: Map<String, String>? = null,
    public val entitlements: List<String>? = null,
    public val privileged: Boolean,
    public val extraHosts: List<String>? = null,
    public val push: Boolean,
    public val load: Boolean,
    public val digest: String? = null,
    public val errorMessage: String? = null,
    public val output: String? = null,
    public val outputTruncated: Boolean,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val completedAt: Instant? = null,
    public val durationMs: Long? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
)
