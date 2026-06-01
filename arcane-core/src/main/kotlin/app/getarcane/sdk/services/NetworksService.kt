package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SearchPaginationSort
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.network.NetworkCreateRequest
import app.getarcane.sdk.models.network.NetworkCreateResponse
import app.getarcane.sdk.models.network.NetworkInspect
import app.getarcane.sdk.models.network.NetworkPruneReport
import app.getarcane.sdk.models.network.NetworkSummary
import app.getarcane.sdk.models.network.NetworkTopology
import app.getarcane.sdk.models.network.NetworkUsageCounts
import app.getarcane.sdk.pagination.PaginatedResponse

/**
 * Exposes the Docker network endpoints registered under `/environments/{id}/networks`, including
 * topology and prune. Port of Swift `NetworksService`.
 */
public class NetworksService internal constructor(private val rest: RestService) {
    /** Paginated list of networks. */
    public suspend fun list(
        envId: EnvironmentId? = null,
        query: SearchPaginationSort = SearchPaginationSort(),
        inUse: Boolean? = null,
    ): PaginatedResponse<NetworkSummary> {
        val items = buildList {
            addAll(query.nonPaginationQueryItems)
            inUse?.let { add("inUse" to it.toString()) }
        }
        return rest.transport.paginated(
            rest.environmentPath(envId, "networks"),
            query.start ?: 0,
            query.limit ?: 20,
            items,
        )
    }

    /** Get aggregate usage counts for networks. */
    public suspend fun counts(envId: EnvironmentId? = null): NetworkUsageCounts =
        rest.get(rest.environmentPath(envId, "networks/counts"))

    /** Build the network/container topology graph. */
    public suspend fun topology(envId: EnvironmentId? = null): NetworkTopology =
        rest.get(rest.environmentPath(envId, "networks/topology"))

    /** Inspect a network by ID. */
    public suspend fun inspect(
        envId: EnvironmentId? = null,
        networkId: String,
        sort: String? = null,
        order: SortOrder? = null,
    ): NetworkInspect {
        val items = buildList {
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.get(rest.environmentPath(envId, "networks/$networkId"), items)
    }

    /** Create a new Docker network. */
    public suspend fun create(
        envId: EnvironmentId? = null,
        request: NetworkCreateRequest,
    ): NetworkCreateResponse =
        rest.post(rest.environmentPath(envId, "networks"), body = request)

    /** Delete a network. */
    public suspend fun delete(envId: EnvironmentId? = null, networkId: String) {
        rest.deleteVoid(rest.environmentPath(envId, "networks/$networkId"))
    }

    /** Prune unused networks. */
    public suspend fun prune(envId: EnvironmentId? = null): NetworkPruneReport =
        rest.post(rest.environmentPath(envId, "networks/prune"))
}
