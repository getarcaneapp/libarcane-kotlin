package app.getarcane.sdk.models.search

import app.getarcane.sdk.models.category.SettingCategory
import kotlinx.serialization.Serializable

/** Request body for searching settings. */
@Serializable
public data class SettingsSearchRequest(
    public val query: String,
)

/** Response for a settings search. */
@Serializable
public data class SettingsSearchResponse(
    public val results: List<SettingCategory>,
    public val query: String,
    public val count: Int,
)
