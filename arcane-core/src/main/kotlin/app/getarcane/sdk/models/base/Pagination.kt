package app.getarcane.sdk.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors Swift `PaginationResponse` (Models/Base/Pagination.swift). */
@Serializable
public data class PaginationResponse(
    public val totalPages: Long,
    public val totalItems: Long,
    public val currentPage: Int,
    public val itemsPerPage: Int,
    public val grandTotalItems: Long? = null,
)

/** Mirrors Swift `SortOrder`. The [wire] value is interpolated into query strings. */
@Serializable
public enum class SortOrder(public val wire: String) {
    @SerialName("asc")
    ASCENDING("asc"),

    @SerialName("desc")
    DESCENDING("desc"),
}

/**
 * Common query parameters for Arcane list endpoints that support search, sort, and
 * offset-based pagination. Mirrors Swift `SearchPaginationSort`. Produces plain
 * name/value pairs so the model layer stays free of Ktor types.
 */
public data class SearchPaginationSort(
    public val search: String? = null,
    public val start: Int? = null,
    public val limit: Int? = null,
    public val sortBy: String? = null,
    public val sortOrder: SortOrder? = null,
) {
    public val queryItems: List<Pair<String, String>>
        get() = buildList {
            search?.let { add("search" to it) }
            start?.let { add("start" to it.toString()) }
            limit?.let { add("limit" to it.toString()) }
            sortBy?.let { add("sort" to it) }
            sortOrder?.let { add("order" to it.wire) }
        }

    /** [queryItems] without `start`/`limit`, for when those are passed explicitly to `paginated(...)`. */
    public val nonPaginationQueryItems: List<Pair<String, String>>
        get() = buildList {
            search?.let { add("search" to it) }
            sortBy?.let { add("sort" to it) }
            sortOrder?.let { add("order" to it.wire) }
        }
}
