package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.models.dashboard.ActionItems
import app.getarcane.sdk.models.dashboard.DashboardEnvironmentsOverview
import app.getarcane.sdk.models.dashboard.DashboardSnapshot
import app.getarcane.sdk.models.dashboard.DashboardStreamEvent
import app.getarcane.sdk.streaming.ndjsonFlow
import kotlinx.coroutines.flow.Flow

/** Dashboard endpoints. */
public class DashboardService internal constructor(private val rest: RestService) {
    /** Returns the per-environment dashboard first-paint snapshot. */
    public suspend fun snapshot(envId: EnvironmentId? = null, debugAllGood: Boolean = false): DashboardSnapshot =
        rest.get(rest.environmentPath(envId, "dashboard"), debugQuery(debugAllGood))

    /** Returns just the dashboard action items that currently need attention. */
    public suspend fun actionItems(envId: EnvironmentId? = null, debugAllGood: Boolean = false): ActionItems =
        rest.get(rest.environmentPath(envId, "dashboard/action-items"), debugQuery(debugAllGood))

    /** Returns the aggregate dashboard overview across every visible environment. */
    public suspend fun environmentsOverview(debugAllGood: Boolean = false): DashboardEnvironmentsOverview =
        rest.get("dashboard/environments", debugQuery(debugAllGood))

    /** Stream dashboard snapshots for every visible environment over one NDJSON connection. */
    public fun stream(debugAllGood: Boolean = false): Flow<DashboardStreamEvent> =
        rest.transport.ndjsonFlow(
            path = "dashboard/stream",
            deserializer = DashboardStreamEvent.serializer(),
            method = "GET",
            query = debugQuery(debugAllGood),
        )

    private fun debugQuery(debugAllGood: Boolean): List<Pair<String, String>> =
        if (debugAllGood) listOf("debugAllGood" to "true") else emptyList()
}
