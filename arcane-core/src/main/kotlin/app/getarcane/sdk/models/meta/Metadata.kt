package app.getarcane.sdk.models.meta

import kotlinx.serialization.Serializable

/** Metadata describing a configuration / setting field. Mirrors Swift `ConfigMetadata`. */
@Serializable
public data class ConfigMetadata(
    public val key: String,
    public val label: String,
    public val type: String,
    public val keywords: List<String>? = null,
    public val description: String? = null,
)

/** Metadata associated with a template (URLs, tags, author etc.). Mirrors Swift `TemplateMetadata`. */
@Serializable
public data class TemplateMetadata(
    public val version: String? = null,
    public val author: String? = null,
    public val tags: List<String>? = null,
    public val remoteUrl: String? = null,
    public val envUrl: String? = null,
    public val documentationUrl: String? = null,
    public val iconUrl: String? = null,
    public val updatedAt: String? = null,
)
