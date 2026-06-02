package app.getarcane.sdk.pagination

import app.getarcane.sdk.models.base.PaginationResponse
import kotlinx.serialization.Serializable

/**
 * A page of results plus its pagination metadata, `{ "success", "data": [T], "pagination" }`. The
 * streaming [ArcanePaginator] walks pages as a `Flow`.
 */
@Serializable
public data class PaginatedResponse<T>(
    public val success: Boolean,
    public val data: List<T>,
    public val pagination: PaginationResponse,
)
