package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.paginated
import app.getarcane.sdk.models.auth.PasswordChange
import app.getarcane.sdk.models.base.SortOrder
import app.getarcane.sdk.models.role.RoleAssignment
import app.getarcane.sdk.models.role.SetUserAssignments
import app.getarcane.sdk.models.role.UserAssignmentInput
import app.getarcane.sdk.models.user.CreateUser
import app.getarcane.sdk.models.user.UpdateUser
import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.pagination.PaginatedResponse

/** User account management. Port of Swift `UsersService`. */
public class UsersService internal constructor(private val rest: RestService) {
    public suspend fun listPaginated(
        search: String? = null,
        sort: String? = null,
        order: SortOrder? = null,
        start: Int = 0,
        limit: Int = 20,
    ): PaginatedResponse<User> {
        val query = buildList {
            search?.let { add("search" to it) }
            sort?.let { add("sort" to it) }
            order?.let { add("order" to it.wire) }
        }
        return rest.transport.paginated("users", start, limit, query)
    }

    public suspend fun get(id: String): User = rest.get("users/$id")

    public suspend fun create(body: CreateUser): User = rest.post("users", body = body)

    public suspend fun update(id: String, body: UpdateUser): User = rest.put("users/$id", body = body)

    public suspend fun delete(id: String) {
        rest.deleteVoid("users/$id")
    }

    public suspend fun changePassword(body: PasswordChange) {
        rest.postVoid("auth/password", body = body)
    }

    /** A user's role assignments (v2 only). */
    public suspend fun getRoleAssignments(userId: String): List<RoleAssignment> =
        rest.get("users/$userId/role-assignments")

    /** Replace a user's manual role assignments (v2 only). */
    public suspend fun setRoleAssignments(
        userId: String,
        assignments: List<UserAssignmentInput>,
    ): List<RoleAssignment> =
        rest.put("users/$userId/role-assignments", body = SetUserAssignments(assignments))
}
