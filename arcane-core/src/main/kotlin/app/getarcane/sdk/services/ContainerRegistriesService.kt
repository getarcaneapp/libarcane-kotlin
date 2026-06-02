package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.containerregistry.ContainerRegistry
import app.getarcane.sdk.models.containerregistry.ContainerRegistryPullUsageResponse
import app.getarcane.sdk.models.containerregistry.ContainerRegistrySyncRequest
import app.getarcane.sdk.models.containerregistry.CreateContainerRegistry
import app.getarcane.sdk.models.containerregistry.UpdateContainerRegistry
import app.getarcane.sdk.pagination.PaginatedResponse

/** Manages container image registry configurations. */
public class ContainerRegistriesService internal constructor(private val rest: RestService) {
    /** List container registries with pagination. */
    public suspend fun listPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<ContainerRegistry> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.transport.paginated<ContainerRegistry>("container-registries", start, limit, query)
    }

    /** Get a container registry by ID. */
    public suspend fun get(id: String): ContainerRegistry = rest.get("container-registries/$id")

    /** Create a new container registry. */
    public suspend fun create(body: CreateContainerRegistry): ContainerRegistry =
        rest.post("container-registries", body = body)

    /** Update an existing container registry. */
    public suspend fun update(id: String, body: UpdateContainerRegistry): ContainerRegistry =
        rest.put("container-registries/$id", body = body)

    /** Delete a container registry. */
    public suspend fun delete(id: String) {
        rest.deleteVoid("container-registries/$id")
    }

    /** Test connectivity and authentication for a container registry. */
    public suspend fun test(id: String) {
        rest.postVoid("container-registries/$id/test")
    }

    /** Sync container registries from a remote source (manager to agent). */
    public suspend fun sync(body: ContainerRegistrySyncRequest) {
        rest.postVoid("container-registries/sync", body = body)
    }

    /** Get pull-usage and rate-limit visibility for configured registries. */
    public suspend fun pullUsage(): ContainerRegistryPullUsageResponse =
        rest.get("container-registries/pull-usage")
}
