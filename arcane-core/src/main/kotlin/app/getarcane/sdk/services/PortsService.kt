package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.port.PortMapping
import app.getarcane.sdk.pagination.PaginatedResponse

/** Port mappings across containers in an environment. Port of Swift `PortsService`. */
public class PortsService internal constructor(private val rest: RestService) {
    /** Paginated list of port mappings across containers in an environment. */
    public suspend fun list(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
    ): PaginatedResponse<PortMapping> =
        rest.transport.paginated<PortMapping>(
            rest.environmentPath(envId, "ports"),
            query.start ?: 0,
            query.limit ?: 20,
            query.nonPaginationQueryItems,
        )
}
