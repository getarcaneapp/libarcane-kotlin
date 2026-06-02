package app.getarcane.sdk.models.role

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Scope at which a permission resource applies: global or per-environment. */
@Serializable
public enum class PermissionResourceScope(public val wire: String) {
    @SerialName("global")
    GLOBAL("global"),

    @SerialName("env")
    ENV("env"),
}

/** The server's catalog of assignable permissions, grouped by resource. */
@Serializable
public data class PermissionsManifest(
    public val resources: List<PermissionResource>,
)

/** A resource in the permissions manifest, with its assignable actions. */
@Serializable
public data class PermissionResource(
    public val key: String,
    public val label: String,
    public val scope: String,
    public val actions: List<PermissionAction>,
) {
    public val id: String get() = key
    public val scopeKind: PermissionResourceScope?
        get() = PermissionResourceScope.entries.firstOrNull { it.wire == scope }
}

/** A single assignable action on a permission resource. */
@Serializable
public data class PermissionAction(
    public val key: String,
    public val permission: String,
    public val label: String,
    public val description: String? = null,
) {
    public val id: String get() = permission
}
