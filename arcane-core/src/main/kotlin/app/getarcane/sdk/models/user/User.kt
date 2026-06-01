package app.getarcane.sdk.models.user

import app.getarcane.sdk.models.role.RoleAssignmentSummary
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * An Arcane user. Mirrors Swift `User` (Models/User/User.swift), including the dual v1/v2 shape:
 * on a v1 server [roles] is authoritative; on a v2 server [roleAssignments]/[permissionsByEnv] are
 * present and [roles] is synthesized as the deduped list of assigned role IDs so legacy call sites
 * keep working. The synthesis is performed by [UserSerializer].
 */
@Serializable(with = UserSerializer::class)
public data class User(
    public val id: String,
    public val username: String,
    public val displayName: String? = null,
    public val email: String? = null,
    public val roles: List<String> = emptyList(),
    public val canDelete: Boolean = false,
    public val oidcSubjectId: String? = null,
    public val locale: String? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
    public val requiresPasswordChange: Boolean = false,
    public val roleAssignments: List<RoleAssignmentSummary>? = null,
    public val permissionsByEnv: Map<String, List<String>>? = null,
) {
    public companion object {
        /** Reserved [permissionsByEnv] key for permissions that apply across every environment. */
        public const val GLOBAL_PERMISSIONS_KEY: String = "global"
    }
}

/**
 * Custom serializer reproducing Swift `User.init(from:)`: decode the raw payload, then synthesize
 * [User.roles] from [User.roleAssignments] (deduped, order-preserving) when `roles` is absent.
 */
public object UserSerializer : KSerializer<User> {
    @Serializable
    private data class Surrogate(
        val id: String,
        val username: String,
        val displayName: String? = null,
        val email: String? = null,
        val roles: List<String>? = null,
        val canDelete: Boolean = false,
        val oidcSubjectId: String? = null,
        val locale: String? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
        val requiresPasswordChange: Boolean = false,
        val roleAssignments: List<RoleAssignmentSummary>? = null,
        val permissionsByEnv: Map<String, List<String>>? = null,
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): User {
        val s = decoder.decodeSerializableValue(Surrogate.serializer())
        val roles = when {
            s.roles != null -> s.roles
            s.roleAssignments != null -> {
                val seen = LinkedHashSet<String>()
                s.roleAssignments.mapNotNull { if (seen.add(it.roleId)) it.roleId else null }
            }
            else -> emptyList()
        }
        return User(
            id = s.id,
            username = s.username,
            displayName = s.displayName,
            email = s.email,
            roles = roles,
            canDelete = s.canDelete,
            oidcSubjectId = s.oidcSubjectId,
            locale = s.locale,
            createdAt = s.createdAt,
            updatedAt = s.updatedAt,
            requiresPasswordChange = s.requiresPasswordChange,
            roleAssignments = s.roleAssignments,
            permissionsByEnv = s.permissionsByEnv,
        )
    }

    override fun serialize(encoder: Encoder, value: User) {
        encoder.encodeSerializableValue(
            Surrogate.serializer(),
            Surrogate(
                id = value.id,
                username = value.username,
                displayName = value.displayName,
                email = value.email,
                roles = value.roles,
                canDelete = value.canDelete,
                oidcSubjectId = value.oidcSubjectId,
                locale = value.locale,
                createdAt = value.createdAt,
                updatedAt = value.updatedAt,
                requiresPasswordChange = value.requiresPasswordChange,
                roleAssignments = value.roleAssignments,
                permissionsByEnv = value.permissionsByEnv,
            ),
        )
    }
}

/**
 * Initial role assignments by name. Mirrors Swift `CreateUser`. On v2 servers [roles] must be null
 * (use role assignments after creation); on v1 servers it is applied.
 */
@Serializable
public data class CreateUser(
    public val username: String,
    public val password: String,
    public val displayName: String? = null,
    public val email: String? = null,
    public val roles: List<String>? = null,
    public val locale: String? = null,
)

/** Mirrors Swift `UpdateUser`. */
@Serializable
public data class UpdateUser(
    public val username: String? = null,
    public val displayName: String? = null,
    public val email: String? = null,
    public val roles: List<String>? = null,
    public val locale: String? = null,
    public val password: String? = null,
)
