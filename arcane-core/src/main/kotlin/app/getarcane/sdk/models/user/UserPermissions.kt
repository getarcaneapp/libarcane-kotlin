package app.getarcane.sdk.models.user

import app.getarcane.sdk.models.role.Permission
import app.getarcane.sdk.models.role.Role

/**
 * Permission helpers on [User]. They unify the v1 (string roles) and v2 (role assignments +
 * per-env permission sets) shapes.
 */

/**
 * True iff the user holds a global `role_admin` assignment (v2), the sudo wildcard `"*"` in their
 * global permissions (v2), or `"admin"` in the legacy [User.roles] (v1).
 */
public val User.isGlobalAdmin: Boolean
    get() {
        roleAssignments?.let { assignments ->
            if (assignments.any { it.roleId == Role.BuiltIn.ADMIN && it.environmentId == null }) {
                return true
            }
        }
        permissionsByEnv?.get(User.GLOBAL_PERMISSIONS_KEY)?.let { perms ->
            if (Permission.SUDO in perms) return true
        }
        return "admin" in roles
    }

/** Backward-compatible alias for [isGlobalAdmin]. */
public val User.isAdmin: Boolean
    get() = isGlobalAdmin

/**
 * All permissions the user effectively holds for [environmentId] (pass null for global/org-level
 * only). v2: union of the global bucket and the environment bucket. v1: admins get `["*"]`,
 * non-admins get an empty set.
 */
public fun User.permissions(environmentId: String?): Set<String> {
    val perms = permissionsByEnv
    if (perms != null) {
        val out = (perms[User.GLOBAL_PERMISSIONS_KEY] ?: emptyList()).toMutableSet()
        if (environmentId != null) {
            perms[environmentId]?.let { out.addAll(it) }
        }
        return out
    }
    return if (isGlobalAdmin) setOf(Permission.SUDO) else emptySet()
}

/**
 * Whether the user holds [perm] for [environmentId] (null = global bucket only). Returns true
 * unconditionally if any in-scope bucket holds the sudo wildcard `"*"`.
 */
public fun User.hasPermission(perm: String, environmentId: String? = null): Boolean {
    val bucket = permissions(environmentId)
    if (Permission.SUDO in bucket) return true
    return perm in bucket
}

/** Whether the user holds any of [perms] for [environmentId]. */
public fun User.hasAnyPermission(perms: List<String>, environmentId: String? = null): Boolean {
    val bucket = permissions(environmentId)
    if (Permission.SUDO in bucket) return true
    return perms.any { it in bucket }
}
