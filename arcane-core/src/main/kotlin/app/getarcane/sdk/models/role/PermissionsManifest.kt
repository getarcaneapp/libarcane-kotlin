package app.getarcane.sdk.models.role

import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.models.user.hasPermission
import app.getarcane.sdk.models.user.isGlobalAdmin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
    public val resources: List<PermissionResource> = emptyList(),
    public val presets: List<PermissionPreset> = emptyList(),
    public val accessSurfaces: List<AccessSurface> = emptyList(),
) {
    /** Adds transitive action dependencies while retaining caller order. */
    public fun normalizePermissionSelection(selected: List<String>): List<String> {
        val requirements = resources.flatMap { it.actions }.associate { it.permission to it.requires }
        val seen = linkedSetOf<String>()
        val normalized = selected.filterTo(mutableListOf()) { seen.add(it) }
        var index = 0
        while (index < normalized.size) {
            requirements[normalized[index]].orEmpty().forEach { required ->
                if (seen.add(required)) normalized += required
            }
            index += 1
        }
        return normalized
    }

    /** Evaluates backend-owned access metadata for advisory client reachability. */
    public fun canAccessSurface(id: String, user: User, selectedEnvironmentId: String? = null): Boolean {
        val surfaces = accessSurfaces.associateBy(AccessSurface::id)
        fun has(permission: String, scopeMode: AccessSurfaceScopeMode): Boolean = when (scopeMode) {
            AccessSurfaceScopeMode.GLOBAL_ONLY -> user.hasPermission(permission)
            AccessSurfaceScopeMode.SELECTED_ENVIRONMENT_PLUS_GLOBAL ->
                user.hasPermission(permission, selectedEnvironmentId)
            AccessSurfaceScopeMode.ANY_EFFECTIVE_SCOPE ->
                user.permissionsByEnv?.values?.any { Permission.SUDO in it || permission in it }
                    ?: user.isGlobalAdmin
            else -> false
        }
        fun evaluate(surfaceId: String, visiting: MutableSet<String>): Boolean {
            val surface = surfaces[surfaceId] ?: return false
            if (!visiting.add(surfaceId)) return false
            return try {
                when (surface.accessMode) {
                    AccessSurfaceAccessMode.ANY_CHILD ->
                        surface.children.any { evaluate(it, visiting) }
                    AccessSurfaceAccessMode.PERMISSIONS -> {
                        if (surface.permissions.isEmpty()) return false
                        when (surface.matchMode) {
                            AccessSurfaceMatchMode.ALL_OF ->
                                surface.permissions.all { has(it, surface.scopeMode) }
                            AccessSurfaceMatchMode.ANY_OF ->
                                surface.permissions.any { has(it, surface.scopeMode) }
                            else -> false
                        }
                    }
                    else -> false
                }
            } finally {
                visiting.remove(surfaceId)
            }
        }
        return evaluate(id, mutableSetOf())
    }
}

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
    public val requires: List<String> = emptyList(),
) {
    public val id: String get() = permission
}

/** A backend-owned role preset. */
@Serializable
public data class PermissionPreset(
    public val key: String,
    public val label: String,
    public val description: String? = null,
    public val permissions: List<String> = emptyList(),
) {
    public val id: String get() = key
}

@Serializable(with = AccessSurfaceKindSerializer::class)
public data class AccessSurfaceKind(public val wire: String) {
    public companion object {
        public val ROUTE: AccessSurfaceKind = AccessSurfaceKind("route")
        public val SETTINGS_CATEGORY: AccessSurfaceKind = AccessSurfaceKind("settings-category")
        public val CUSTOMIZE_CATEGORY: AccessSurfaceKind = AccessSurfaceKind("customize-category")
        public val LANDING: AccessSurfaceKind = AccessSurfaceKind("landing")
        public val UNKNOWN: AccessSurfaceKind = AccessSurfaceKind("")
    }
}

@Serializable(with = AccessSurfaceAccessModeSerializer::class)
public data class AccessSurfaceAccessMode(public val wire: String) {
    public companion object {
        public val PERMISSIONS: AccessSurfaceAccessMode = AccessSurfaceAccessMode("permissions")
        public val ANY_CHILD: AccessSurfaceAccessMode = AccessSurfaceAccessMode("any-child")
        public val UNKNOWN: AccessSurfaceAccessMode = AccessSurfaceAccessMode("")
    }
}

@Serializable(with = AccessSurfaceMatchModeSerializer::class)
public data class AccessSurfaceMatchMode(public val wire: String) {
    public companion object {
        public val ANY_OF: AccessSurfaceMatchMode = AccessSurfaceMatchMode("any-of")
        public val ALL_OF: AccessSurfaceMatchMode = AccessSurfaceMatchMode("all-of")
        public val UNKNOWN: AccessSurfaceMatchMode = AccessSurfaceMatchMode("")
    }
}

@Serializable(with = AccessSurfaceScopeModeSerializer::class)
public data class AccessSurfaceScopeMode(public val wire: String) {
    public companion object {
        public val GLOBAL_ONLY: AccessSurfaceScopeMode = AccessSurfaceScopeMode("global-only")
        public val SELECTED_ENVIRONMENT_PLUS_GLOBAL: AccessSurfaceScopeMode =
            AccessSurfaceScopeMode("selected-env-plus-global")
        public val ANY_EFFECTIVE_SCOPE: AccessSurfaceScopeMode = AccessSurfaceScopeMode("any-effective-scope")
        public val UNKNOWN: AccessSurfaceScopeMode = AccessSurfaceScopeMode("")
    }
}

/** Backend-owned metadata describing whether a client surface should be reachable. */
@Serializable
public data class AccessSurface(
    public val id: String,
    public val kind: AccessSurfaceKind = AccessSurfaceKind.UNKNOWN,
    public val url: String? = null,
    public val label: String = "",
    public val accessMode: AccessSurfaceAccessMode = AccessSurfaceAccessMode.UNKNOWN,
    public val matchMode: AccessSurfaceMatchMode = AccessSurfaceMatchMode.UNKNOWN,
    public val scopeMode: AccessSurfaceScopeMode = AccessSurfaceScopeMode.UNKNOWN,
    public val permissions: List<String> = emptyList(),
    public val children: List<String> = emptyList(),
    public val fallbackOrder: Int? = null,
)

private abstract class WireValueSerializer<T>(name: String) : KSerializer<T> {
    final override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)
    protected abstract fun fromWire(wire: String): T
    protected abstract fun wire(value: T): String
    final override fun deserialize(decoder: Decoder): T = fromWire(decoder.decodeString())
    final override fun serialize(encoder: Encoder, value: T): Unit = encoder.encodeString(wire(value))
}

private object AccessSurfaceKindSerializer : WireValueSerializer<AccessSurfaceKind>("AccessSurfaceKind") {
    override fun fromWire(wire: String): AccessSurfaceKind = AccessSurfaceKind(wire)
    override fun wire(value: AccessSurfaceKind): String = value.wire
}

private object AccessSurfaceAccessModeSerializer :
    WireValueSerializer<AccessSurfaceAccessMode>("AccessSurfaceAccessMode") {
    override fun fromWire(wire: String): AccessSurfaceAccessMode = AccessSurfaceAccessMode(wire)
    override fun wire(value: AccessSurfaceAccessMode): String = value.wire
}

private object AccessSurfaceMatchModeSerializer :
    WireValueSerializer<AccessSurfaceMatchMode>("AccessSurfaceMatchMode") {
    override fun fromWire(wire: String): AccessSurfaceMatchMode = AccessSurfaceMatchMode(wire)
    override fun wire(value: AccessSurfaceMatchMode): String = value.wire
}

private object AccessSurfaceScopeModeSerializer :
    WireValueSerializer<AccessSurfaceScopeMode>("AccessSurfaceScopeMode") {
    override fun fromWire(wire: String): AccessSurfaceScopeMode = AccessSurfaceScopeMode(wire)
    override fun wire(value: AccessSurfaceScopeMode): String = value.wire
}
