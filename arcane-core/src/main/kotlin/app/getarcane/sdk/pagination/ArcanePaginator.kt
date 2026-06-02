package app.getarcane.sdk.pagination

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A cold [Flow] that walks a paginated endpoint, fetching successive `limit`-sized pages until
 * `pagination.totalItems` is exhausted (or a page comes back empty).
 */
public fun <T> arcanePaginator(
    limit: Int = 50,
    fetch: suspend (start: Int, limit: Int) -> PaginatedResponse<T>,
): Flow<T> = flow {
    var start = 0
    val pageLimit = maxOf(1, limit)
    while (true) {
        val page = fetch(start, pageLimit)
        if (page.data.isEmpty()) break
        page.data.forEach { emit(it) }
        start += page.data.size
        if (start.toLong() >= page.pagination.totalItems) break
    }
}
