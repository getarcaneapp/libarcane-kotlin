package app.getarcane.sdk.models.role

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors Swift `RoleAssignmentSource`. */
@Serializable
public enum class RoleAssignmentSource(public val wire: String) {
    @SerialName("manual")
    MANUAL("manual"),

    @SerialName("oidc")
    OIDC("oidc"),
}

/** A role granted to a user, optionally scoped to an environment. Mirrors Swift `RoleAssignment`. */
@Serializable
public data class RoleAssignment(
    public val id: String,
    public val userId: String,
    public val roleId: String,
    public val environmentId: String? = null,
    public val source: String = "manual",
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
) {
    public val sourceKind: RoleAssignmentSource?
        get() = RoleAssignmentSource.entries.firstOrNull { it.wire == source }
}

/** Compact role assignment carried on a v2 `User` payload. Mirrors Swift `RoleAssignmentSummary`. */
@Serializable
public data class RoleAssignmentSummary(
    public val roleId: String,
    public val environmentId: String? = null,
    public val source: String = "manual",
) {
    public val sourceKind: RoleAssignmentSource?
        get() = RoleAssignmentSource.entries.firstOrNull { it.wire == source }
}

/** A single desired assignment when setting a user's roles. Mirrors Swift `UserAssignmentInput`. */
@Serializable
public data class UserAssignmentInput(
    public val roleId: String,
    public val environmentId: String? = null,
)

/** Request body to replace a user's role assignments. Mirrors Swift `SetUserAssignments`. */
@Serializable
public data class SetUserAssignments(
    public val assignments: List<UserAssignmentInput>,
)
