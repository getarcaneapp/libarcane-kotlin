package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.role.CreateRole
import app.getarcane.sdk.models.role.PermissionsManifest
import app.getarcane.sdk.models.role.Role
import app.getarcane.sdk.models.role.UpdateRole
import app.getarcane.sdk.pagination.PaginatedResponse

/**
 * Manages roles and role-related metadata in v2 RBAC servers.
 * Calls fail with `ArcaneError.NotFound` on v1 servers; gate UI on `ArcaneClient.serverCapabilities()`
 * to avoid invoking against v1.
 */
public class RolesService internal constructor(private val rest: RestService) {
    /** List roles (built-in + custom) with pagination. */
    public suspend fun listPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<Role> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.transport.paginated<Role>("roles", start, limit, query)
    }

    /** Get a single role by ID. */
    public suspend fun get(id: String): Role = rest.get("roles/$id")

    /** Create a custom role. Reserved for global admins server-side. */
    public suspend fun create(body: CreateRole): Role = rest.post("roles", body = body)

    /** Update a custom role. Built-in roles return 403. */
    public suspend fun update(id: String, body: UpdateRole): Role = rest.put("roles/$id", body = body)

    /**
     * Delete a custom role. May throw `.Conflict` if removal would leave the system with zero global
     * admins.
     */
    public suspend fun delete(id: String) {
        rest.deleteVoid("roles/$id")
    }

    /**
     * Returns the server's permission manifest — every recognized permission grouped by resource.
     * Used to render the permission picker.
     */
    public suspend fun availablePermissions(): PermissionsManifest = rest.get("roles/available-permissions")
}
