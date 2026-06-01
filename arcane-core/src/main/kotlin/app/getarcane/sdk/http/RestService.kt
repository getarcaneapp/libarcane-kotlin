package app.getarcane.sdk.http

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.pagination.PaginatedResponse

/**
 * Generic REST helpers over [ArcaneTransport], mirroring Swift `RESTService` (HTTP/RESTService.swift).
 * Typed methods are reified so services call `rest.get<ContainerDetails>(path)`; `*Void` methods
 * decode the `MessageResponse` envelope and discard it.
 */
public class RestService internal constructor(
    public val transport: ArcaneTransport,
    public val defaultEnvironmentId: EnvironmentId,
) {
    public suspend inline fun <reified T> get(path: String, query: List<Pair<String, String>> = emptyList()): T =
        transport.request(path, "GET", query)

    public suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        query: List<Pair<String, String>> = emptyList(),
    ): T = transport.request(path, "POST", query, body)

    public suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        query: List<Pair<String, String>> = emptyList(),
    ): T = transport.request(path, "PUT", query, body)

    public suspend inline fun <reified T> patch(
        path: String,
        body: Any? = null,
        query: List<Pair<String, String>> = emptyList(),
    ): T = transport.request(path, "PATCH", query, body)

    public suspend inline fun <reified T> delete(path: String, query: List<Pair<String, String>> = emptyList()): T =
        transport.request(path, "DELETE", query)

    public suspend fun deleteVoid(path: String, query: List<Pair<String, String>> = emptyList()) {
        transport.request<MessageResponse>(path, "DELETE", query)
    }

    public suspend fun postVoid(
        path: String,
        body: Any? = null,
        query: List<Pair<String, String>> = emptyList(),
    ) {
        transport.request<MessageResponse>(path, "POST", query, body)
    }

    public suspend fun putVoid(
        path: String,
        body: Any? = null,
        query: List<Pair<String, String>> = emptyList(),
    ) {
        transport.request<MessageResponse>(path, "PUT", query, body)
    }

    public suspend inline fun <reified T> paginated(
        path: String,
        start: Int,
        limit: Int,
        query: List<Pair<String, String>> = emptyList(),
    ): PaginatedResponse<T> = transport.paginated(path, start, limit, query)

    /** `environments/{id}/{suffix}`, defaulting to [defaultEnvironmentId]. Mirrors Swift `environmentPath`. */
    public fun environmentPath(envId: EnvironmentId?, suffix: String): String =
        "environments/${(envId ?: defaultEnvironmentId).rawValue}/${suffix.trim('/')}"
}
