package app.getarcane.sdk.pagination

import app.getarcane.sdk.models.base.PaginationResponse
import kotlinx.serialization.Serializable

/**
 * A page of results plus its pagination metadata, `{ "success", "data": [T], "pagination" }`.
 * Mirrors Swift `PaginatedResponse` / `PaginatedAPIResponse`. (The streaming [ArcanePaginator]
 * that walks pages as a `Flow` is added in the streaming phase.)
 */
@Serializable
public data class PaginatedResponse<T>(
    public val success: Boolean,
    public val data: List<T>,
    public val pagination: PaginationResponse,
)
