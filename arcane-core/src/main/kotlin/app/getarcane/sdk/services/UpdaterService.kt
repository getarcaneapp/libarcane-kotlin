package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.models.updater.AutoUpdateRecord
import app.getarcane.sdk.models.updater.UpdaterOptions
import app.getarcane.sdk.models.updater.UpdaterResult
import app.getarcane.sdk.models.updater.UpdaterStatus

/**
 * Updater endpoints under `/environments/{id}/updater` and
 * `/environments/{id}/containers/{containerId}/update`.
 */
public class UpdaterService internal constructor(private val rest: RestService) {
    /** Runs the updater. When [options] is omitted the server uses its defaults. */
    public suspend fun run(options: UpdaterOptions? = null, envId: EnvironmentId? = null): UpdaterResult =
        rest.post(rest.environmentPath(envId, "updater/run"), body = options)

    /** Returns the live updater status. */
    public suspend fun status(envId: EnvironmentId? = null): UpdaterStatus =
        rest.get(rest.environmentPath(envId, "updater/status"))

    /** Returns the most recent [limit] history records. */
    public suspend fun history(limit: Int = 50, envId: EnvironmentId? = null): List<AutoUpdateRecord> =
        rest.get(rest.environmentPath(envId, "updater/history"), query = listOf("limit" to limit.toString()))

    /**
     * Updates a single container by pulling the latest image and applying the appropriate strategy
     * server-side.
     */
    public suspend fun updateContainer(containerId: String, envId: EnvironmentId? = null): UpdaterResult =
        rest.post(rest.environmentPath(envId, "containers/$containerId/update"))
}
