package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.version.VersionCheck
import app.getarcane.sdk.models.version.VersionInfo

/**
 * Version endpoints. Port of Swift `VersionService`.
 *
 * `/version` and `/app-version` return their payloads *without* the standard `{success, data}`
 * envelope, so we go through [requestDecoded] and decode the body type directly.
 * `/environments/{id}/version` is wrapped normally.
 */
public class VersionService internal constructor(private val rest: RestService) {
    /**
     * Returns a simplified version check, optionally comparing against a caller-supplied [current]
     * version.
     */
    public suspend fun check(current: String? = null): VersionCheck {
        val query = buildList { current?.let { add("current" to it) } }
        return rest.transport.requestDecoded("version", method = "GET", query = query, authorized = false)
    }

    /** Returns the full application version info for the local API server. */
    public suspend fun appVersion(): VersionInfo =
        rest.transport.requestDecoded("app-version", method = "GET", authorized = false)

    /** Returns the application version info for a remote environment. */
    public suspend fun environmentVersion(envId: EnvironmentId? = null): VersionInfo =
        rest.get(rest.environmentPath(envId, "version"))
}
