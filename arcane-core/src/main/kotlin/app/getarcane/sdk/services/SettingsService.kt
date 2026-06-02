package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.category.SettingCategory
import app.getarcane.sdk.models.search.SettingsSearchRequest
import app.getarcane.sdk.models.search.SettingsSearchResponse
import app.getarcane.sdk.models.settings.PublicSetting
import app.getarcane.sdk.models.settings.SettingDto
import app.getarcane.sdk.models.settings.UpdateSettings

/** Manages application settings, search, and category metadata. */
public class SettingsService internal constructor(private val rest: RestService) {
    // Per-environment settings

    /**
     * Get all public settings for an environment (no auth required).
     *
     * The endpoint returns the list directly without an `ApiResponse` envelope.
     */
    public suspend fun getPublicSettings(envId: EnvironmentId? = null): List<PublicSetting> =
        rest.transport.requestDecoded(
            rest.environmentPath(envId, "settings/public"),
            authorized = false,
        )

    /**
     * Get all settings visible to the current user for an environment.
     *
     * The endpoint returns the list directly without an `ApiResponse` envelope.
     */
    public suspend fun getSettings(envId: EnvironmentId? = null): List<PublicSetting> =
        rest.transport.requestDecoded(rest.environmentPath(envId, "settings"))

    /** Update settings for an environment. */
    public suspend fun updateSettings(body: UpdateSettings, envId: EnvironmentId? = null): List<SettingDto> =
        rest.put(rest.environmentPath(envId, "settings"), body = body)

    // Top-level settings (search & categories)

    /**
     * Search settings categories and individual settings by query.
     *
     * The endpoint returns the response directly without an `ApiResponse` envelope.
     */
    public suspend fun search(query: String): SettingsSearchResponse =
        rest.transport.requestDecoded(
            "settings/search",
            method = "POST",
            body = SettingsSearchRequest(query = query),
        )

    /**
     * Get all available settings categories with metadata.
     *
     * The endpoint returns the list directly without an `ApiResponse` envelope.
     */
    public suspend fun categories(): List<SettingCategory> =
        rest.transport.requestDecoded("settings/categories")
}
