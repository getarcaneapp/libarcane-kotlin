package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.apikey.APIKey
import app.getarcane.sdk.models.apikey.APIKeyCreated
import app.getarcane.sdk.models.apikey.CreateAPIKey
import app.getarcane.sdk.models.apikey.UpdateAPIKey
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.pagination.PaginatedResponse

/** Manages API keys for programmatic access. */
public class APIKeysService internal constructor(private val rest: RestService) {
    /** List API keys with pagination. */
    public suspend fun listPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<APIKey> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.transport.paginated<APIKey>("api-keys", start, limit, query)
    }

    /** Get details of a specific API key. */
    public suspend fun get(id: String): APIKey = rest.get("api-keys/$id")

    /** Create a new API key. The plain-text key is returned only on creation. */
    public suspend fun create(body: CreateAPIKey): APIKeyCreated = rest.post("api-keys", body = body)

    /** Update an existing API key. */
    public suspend fun update(id: String, body: UpdateAPIKey): APIKey = rest.put("api-keys/$id", body = body)

    /** Delete an API key. */
    public suspend fun delete(id: String) {
        rest.deleteVoid("api-keys/$id")
    }
}
