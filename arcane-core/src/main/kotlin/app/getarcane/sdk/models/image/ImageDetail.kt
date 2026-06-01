package app.getarcane.sdk.models.image

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.Serializable

/** The `config` block on an `ImageDetailSummary`. Mirrors Swift `ImageDetailConfig` (Models/image/ImageDetail.swift). */
@Serializable
public data class ImageDetailConfig(
    public val exposedPorts: Map<String, JsonValue>? = null,
    public val env: List<String>? = null,
    public val cmd: List<String>? = null,
    public val entrypoint: List<String>? = null,
    public val user: String? = null,
    public val volumes: Map<String, JsonValue>? = null,
    public val workingDir: String? = null,
    public val argsEscaped: Boolean? = null,
    public val labels: Map<String, String>? = null,
)

/** The `graphDriver` block on an `ImageDetailSummary`. Mirrors Swift `ImageDetailGraphDriver`. */
@Serializable
public data class ImageDetailGraphDriver(
    public val data: JsonValue? = null,
    public val name: String,
)

/** The `rootFs` block on an `ImageDetailSummary`. Mirrors Swift `ImageDetailRootFs`. */
@Serializable
public data class ImageDetailRootFs(
    public val type: String,
    public val layers: List<String>,
)

/** The `metadata` block on an `ImageDetailSummary`. Mirrors Swift `ImageDetailMetadata`. */
@Serializable
public data class ImageDetailMetadata(
    public val lastTagTime: String,
)

/** The `descriptor` block on an `ImageDetailSummary`. Mirrors Swift `ImageDetailDescriptor`. */
@Serializable
public data class ImageDetailDescriptor(
    public val mediaType: String,
    public val digest: String,
    public val size: Long,
)

/** Detailed information about a single image (the `inspect` response shape). Mirrors Swift `ImageDetailSummary`. */
@Serializable
public data class ImageDetailSummary(
    public val id: String,
    public val repoTags: List<String>,
    public val repoDigests: List<String>,
    public val comment: String,
    public val created: String,
    public val author: String,
    public val config: ImageDetailConfig,
    public val architecture: String,
    public val os: String,
    public val size: Long,
    public val graphDriver: ImageDetailGraphDriver,
    public val rootFs: ImageDetailRootFs,
    public val metadata: ImageDetailMetadata,
    public val descriptor: ImageDetailDescriptor,
)
