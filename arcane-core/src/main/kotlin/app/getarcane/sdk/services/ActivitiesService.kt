package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.activity.Activity
import app.getarcane.sdk.models.activity.ActivityDetail
import app.getarcane.sdk.models.activity.ActivityStatus
import app.getarcane.sdk.models.activity.ActivityStreamEvent
import app.getarcane.sdk.models.activity.ActivityType
import app.getarcane.sdk.models.activity.ClearActivityHistoryResult
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.pagination.PaginatedResponse
import app.getarcane.sdk.streaming.ndjsonFlow
import kotlinx.coroutines.flow.Flow

/** Lists, inspects, streams, cancels, and clears v2 background activities for an environment. */
public class ActivitiesService internal constructor(private val rest: RestService) {
    /** List current and recent background activities for an environment. */
    public suspend fun listPaginated(
        envId: EnvironmentId? = null,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 50,
        status: ActivityStatus? = null,
        type: ActivityType? = null,
        resourceType: String? = null,
    ): PaginatedResponse<Activity> =
        rest.transport.paginated<Activity>(
            rest.environmentPath(envId, "activities"),
            start,
            limit,
            buildQuery(search, sort, order, status, type, resourceType),
        )

    /** Get a background activity with its recent output messages. */
    public suspend fun detail(
        envId: EnvironmentId? = null,
        activityId: String,
        limit: Int = 500,
    ): ActivityDetail =
        rest.get(
            rest.environmentPath(envId, "activities/$activityId"),
            listOf("limit" to limit.toString()),
        )

    /** Stream activity snapshots and updates as a cold NDJSON [Flow]. */
    public fun stream(
        envId: EnvironmentId? = null,
        limit: Int = 50,
    ): Flow<ActivityStreamEvent> =
        rest.transport.ndjsonFlow(
            rest.environmentPath(envId, "activities/stream"),
            ActivityStreamEvent.serializer(),
            method = "GET",
            query = listOf("limit" to limit.toString()),
        )

    /** Request cancellation of a running or queued activity. */
    public suspend fun cancel(
        envId: EnvironmentId? = null,
        activityId: String,
        requestedBy: String? = null,
    ): Activity {
        val query = buildList {
            requestedBy?.takeIf { it.isNotEmpty() }?.let { add("requestedBy" to it) }
        }
        return rest.post(rest.environmentPath(envId, "activities/$activityId/cancel"), query = query)
    }

    /** Clear completed activity history for an environment. */
    public suspend fun clearHistory(envId: EnvironmentId? = null): ClearActivityHistoryResult =
        rest.delete(rest.environmentPath(envId, "activities/history"))

    private fun buildQuery(
        search: String?,
        sort: String?,
        order: SortOrder?,
        status: ActivityStatus?,
        type: ActivityType?,
        resourceType: String?,
    ): List<Pair<String, String>> = buildList {
        search?.takeIf { it.isNotEmpty() }?.let { add("search" to it) }
        sort?.takeIf { it.isNotEmpty() }?.let { add("sort" to it) }
        order?.let { add("order" to it.wire) }
        status?.let { add("status" to it.wire) }
        type?.let { add("type" to it.wire) }
        resourceType?.takeIf { it.isNotEmpty() }?.let { add("resourceType" to it) }
    }
}
