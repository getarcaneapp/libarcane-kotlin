package app.getarcane.sdk.models.role

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A v2 RBAC role. Mirrors Swift `Role` (Models/Role/Role.swift). Optional fields default to match
 * Swift's `decodeIfPresent(...) ?? default`; `createdAt` is required.
 */
@Serializable
public data class Role(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val permissions: List<String> = emptyList(),
    public val builtIn: Boolean = false,
    public val assignedUserCount: Int = 0,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
) {
    /** Stable built-in role IDs seeded by the v2 backend migration. Mirrors Swift `Role.BuiltIn`. */
    public object BuiltIn {
        public const val ADMIN: String = "role_admin"
        public const val EDITOR: String = "role_editor"
        public const val NO_SHELL_EDITOR: String = "role_no_shell_editor"
        public const val DEPLOYER: String = "role_deployer"
        public const val MONITOR: String = "role_monitor"
        public const val VIEWER: String = "role_viewer"
    }
}

/** Mirrors Swift `CreateRole`. */
@Serializable
public data class CreateRole(
    public val name: String,
    public val description: String? = null,
    public val permissions: List<String>,
)

/** Mirrors Swift `UpdateRole`. */
@Serializable
public data class UpdateRole(
    public val name: String,
    public val description: String? = null,
    public val permissions: List<String>,
)
