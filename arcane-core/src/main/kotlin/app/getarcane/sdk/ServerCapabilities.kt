package app.getarcane.sdk

import app.getarcane.sdk.models.user.User

/**
 * Snapshot of which Arcane API shape the server speaks, detected from the first authenticated
 * [User] payload the SDK decodes. Surfaced via [ArcaneClient.serverCapabilities] so callers can
 * gate v2-only screens.
 */
public data class ServerCapabilities(
    public val mode: Mode,
) {
    public enum class Mode {
        /** Detection has not happened yet (no user payload decoded). */
        UNKNOWN,

        /** Legacy string-roles backend; `User.roles` is the source of truth. */
        LEGACY_ROLES,

        /** RBAC backend with role assignments, per-env permission sets, and the v2 endpoints. */
        RBAC,
    }

    /** True iff the server exposes the v2 RBAC endpoints. */
    public val supportsRoleManagement: Boolean get() = mode == Mode.RBAC

    /** True iff the server exposes the v2 background activity endpoints. */
    public val supportsActivities: Boolean get() = mode == Mode.RBAC

    /** True iff payloads include `permissionsByEnv` for per-permission queries. */
    public val supportsPermissionQueries: Boolean get() = mode == Mode.RBAC

    public companion object {
        public val UNKNOWN: ServerCapabilities = ServerCapabilities(Mode.UNKNOWN)

        /** Infers the server mode from a freshly decoded [User]. */
        public fun detect(user: User): Mode =
            if (user.permissionsByEnv != null || user.roleAssignments != null) {
                Mode.RBAC
            } else {
                Mode.LEGACY_ROLES
            }
    }
}
