package app.getarcane.sdk.models.user

import app.getarcane.sdk.models.role.RoleAssignmentSummary
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * An Arcane user, including the dual v1/v2 shape:
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
    public val fontSize: Int? = null,
    public val avatarUrl: String? = null,
    public val timeFormat: UserTimeFormat? = null,
    public val lastLogin: String? = null,
    public val preferences: UserPreferences? = null,
    @SerialName("isGlobalAdmin")
    public val serverIsGlobalAdmin: Boolean? = null,
) {
    public companion object {
        /** Reserved [permissionsByEnv] key for permissions that apply across every environment. */
        public const val GLOBAL_PERMISSIONS_KEY: String = "global"
    }
}

/**
 * Custom serializer: decode the raw payload, then synthesize
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
        val fontSize: Int? = null,
        val avatarUrl: String? = null,
        val timeFormat: UserTimeFormat? = null,
        val lastLogin: String? = null,
        val preferences: UserPreferences? = null,
        @SerialName("isGlobalAdmin")
        val serverIsGlobalAdmin: Boolean? = null,
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
            fontSize = s.fontSize,
            avatarUrl = s.avatarUrl,
            timeFormat = s.timeFormat,
            lastLogin = s.lastLogin,
            preferences = s.preferences,
            serverIsGlobalAdmin = s.serverIsGlobalAdmin,
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
                fontSize = value.fontSize,
                avatarUrl = value.avatarUrl,
                timeFormat = value.timeFormat,
                lastLogin = value.lastLogin,
                preferences = value.preferences,
                serverIsGlobalAdmin = value.serverIsGlobalAdmin,
            ),
        )
    }
}

/**
 * Initial role assignments by name. On v2 servers [roles] must be null
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
    public val timeFormat: UserTimeFormat? = null,
) {
    override fun toString(): String =
        "CreateUser(username=$username, password=<redacted>, displayName=$displayName, email=$email, " +
            "roles=$roles, locale=$locale, timeFormat=$timeFormat)"
}

/** Fields for updating an existing user. */
@Serializable
public data class UpdateUser(
    public val username: String? = null,
    public val displayName: String? = null,
    public val email: String? = null,
    public val roles: List<String>? = null,
    public val locale: String? = null,
    public val password: String? = null,
    public val timeFormat: UserTimeFormat? = null,
) {
    override fun toString(): String =
        "UpdateUser(username=$username, displayName=$displayName, email=$email, roles=$roles, " +
            "locale=$locale, password=${if (password == null) "null" else "<redacted>"}, " +
            "timeFormat=$timeFormat)"
}

/**
 * Fields that a signed-in user may change on their own account. This deliberately excludes
 * administrator-only fields such as username, roles, and password.
 */
@Serializable
public data class UpdateProfile(
    public val displayName: String? = null,
    public val email: String? = null,
    public val locale: String? = null,
    public val timeFormat: UserTimeFormat? = null,
    public val fontSize: Int? = null,
    public val preferences: UserPreferences? = null,
)

/** Time-display preference accepted by current Arcane user/profile contracts. */
@Serializable(with = UserTimeFormatSerializer::class)
public enum class UserTimeFormat(public val wire: String) {
    @SerialName("auto") AUTO("auto"),
    @SerialName("12h") TWELVE_HOUR("12h"),
    @SerialName("24h") TWENTY_FOUR_HOUR("24h"),
    UNKNOWN("unknown"),
}

internal object UserTimeFormatSerializer : KSerializer<UserTimeFormat> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UserTimeFormat", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UserTimeFormat {
        val wire = decoder.decodeString()
        return UserTimeFormat.entries.firstOrNull { it.wire == wire } ?: UserTimeFormat.UNKNOWN
    }

    override fun serialize(encoder: Encoder, value: UserTimeFormat) {
        if (value == UserTimeFormat.UNKNOWN) {
            throw SerializationException("Cannot encode unknown user time format")
        }
        encoder.encodeString(value.wire)
    }
}

/** Per-user UI preferences emitted by Arcane and accepted by the self-profile route. */
@Serializable
public data class UserPreferences(
    public val themeMode: String? = null,
    public val applicationTheme: String? = null,
    public val accentColor: String? = null,
    public val iconCatalog: String? = null,
    public val oledMode: Boolean? = null,
    public val glassEffectsEnabled: Boolean? = null,
    public val animationsEnabled: Boolean? = null,
    public val sidebarHoverExpansion: Boolean? = null,
    public val keyboardShortcutsEnabled: Boolean? = null,
    public val mobileNavigationMode: String? = null,
    public val mobileNavigationShowLabels: Boolean? = null,
    public val defaultLandingPage: String? = null,
)

/** Image formats accepted by Arcane's self-avatar endpoint. */
public enum class AvatarImageFormat(
    public val mediaType: String,
    public val fileExtension: String,
) {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp"),
}
