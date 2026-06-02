package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.event.CreateEvent
import app.getarcane.sdk.models.event.Event
import app.getarcane.sdk.pagination.PaginatedResponse

/** Manages system audit events. */
public class EventsService internal constructor(private val rest: RestService) {
    /** List system events with pagination and optional filters. */
    public suspend fun listPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
        severity: String? = null,
        type: String? = null,
    ): PaginatedResponse<Event> {
        val query = buildEventQuery(search, sort, order, severity, type)
        return rest.transport.paginated<Event>("events", start, limit, query)
    }

    /** List events scoped to a specific environment ID. */
    public suspend fun listByEnvironmentPaginated(
        environmentId: String,
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
        severity: String? = null,
        type: String? = null,
    ): PaginatedResponse<Event> {
        val query = buildEventQuery(search, sort, order, severity, type)
        return rest.transport.paginated<Event>("events/environment/$environmentId", start, limit, query)
    }

    /** Create a new event. */
    public suspend fun create(body: CreateEvent): Event = rest.post("events", body = body)

    /** Delete an event by ID. */
    public suspend fun delete(id: String) {
        rest.deleteVoid("events/$id")
    }

    private fun buildEventQuery(
        search: String?,
        sort: String?,
        order: SortOrder?,
        severity: String?,
        type: String?,
    ): List<Pair<String, String>> = buildList {
        search?.let { add("search" to it) }
        sort?.let { add("sort" to it) }
        order?.let { add("order" to it.wire) }
        severity?.let { add("severity" to it) }
        type?.let { add("type" to it) }
    }
}
