package app.getarcane.sdk.models.search

import app.getarcane.sdk.models.category.SettingCategory
import kotlinx.serialization.Serializable

/** Mirrors Swift `SettingsSearchRequest` (Models/Search/Search.swift). */
@Serializable
public data class SettingsSearchRequest(
    public val query: String,
)

/** Mirrors Swift `SettingsSearchResponse`. */
@Serializable
public data class SettingsSearchResponse(
    public val results: List<SettingCategory>,
    public val query: String,
    public val count: Int,
)
