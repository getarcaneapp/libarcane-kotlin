package app.getarcane.sdk.models.category

import kotlinx.serialization.Serializable

/** Category metadata used by the settings UI. Mirrors Swift `SettingCategory` (Models/Category/Category.swift). */
@Serializable
public data class SettingCategory(
    public val id: String,
    public val title: String,
    public val description: String,
    public val icon: String,
    public val url: String,
    public val keywords: List<String> = emptyList(),
    public val settings: List<SettingMetadata> = emptyList(),
    public val matchingSettings: List<SettingMetadata>? = null,
    public val relevanceScore: Int? = null,
)

/** Setting-level metadata used by the settings UI. Mirrors Swift `SettingMetadata`. */
@Serializable
public data class SettingMetadata(
    public val key: String,
    public val label: String,
    public val type: String,
    public val keywords: List<String>? = null,
    public val description: String? = null,
)
