package app.getarcane.sdk

import app.getarcane.sdk.models.role.Permission
import app.getarcane.sdk.models.role.RoleAssignmentSummary
import app.getarcane.sdk.models.user.CreateUser
import app.getarcane.sdk.models.user.UpdateUser
import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.models.user.hasPermission
import app.getarcane.sdk.models.user.isAdmin
import app.getarcane.sdk.models.user.isGlobalAdmin
import app.getarcane.sdk.models.user.permissions
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Decoding of v1/v2 `User` payloads and derived capabilities. */
class UserDecodingTest {
    private val json = ArcaneJson.default

    @Test
    fun decodeV1Admin() {
        val user = json.decodeFromString<User>(
            """{"id":"u1","username":"alice","roles":["admin"],"canDelete":true,"requiresPasswordChange":false}""",
        )
        assertEquals(listOf("admin"), user.roles)
        assertNull(user.roleAssignments)
        assertNull(user.permissionsByEnv)
        assertTrue(user.isGlobalAdmin)
        assertTrue(user.isAdmin)
        assertTrue(user.hasPermission(Permission.Containers.START))
        assertTrue(user.hasPermission(Permission.Containers.START, environmentId = "3"))
        assertTrue(user.hasPermission("anything:goes"))
    }

    @Test
    fun decodeV1NonAdmin() {
        val user = json.decodeFromString<User>(
            """{"id":"u2","username":"bob","roles":["user"],"canDelete":true,"requiresPasswordChange":false}""",
        )
        assertFalse(user.isGlobalAdmin)
        assertFalse(user.hasPermission(Permission.Containers.START))
        assertFalse(user.hasPermission(Permission.Containers.START, environmentId = "3"))
        assertTrue(user.permissions(null).isEmpty())
    }

    @Test
    fun decodeV2AdminGlobal() {
        val user = json.decodeFromString<User>(
            """
            {"id":"u3","username":"root",
             "roleAssignments":[{"roleId":"role_admin","source":"manual"}],
             "permissionsByEnv":{"global":["*"]},
             "canDelete":false,"requiresPasswordChange":false,
             "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertEquals(listOf("role_admin"), user.roles) // synthesized from assignments
        assertEquals(1, user.roleAssignments?.size)
        assertEquals(listOf("*"), user.permissionsByEnv?.get("global"))
        assertTrue(user.isGlobalAdmin)
        assertTrue(user.hasPermission(Permission.Containers.START))
        assertTrue(user.hasPermission(Permission.Containers.START, environmentId = "any-env"))
        assertTrue(user.hasPermission("custom:perm"))
    }

    @Test
    fun decodeV2EnvScopedDeployer() {
        val user = json.decodeFromString<User>(
            """
            {"id":"u4","username":"deploy",
             "roleAssignments":[{"roleId":"role_deployer","environmentId":"3","source":"manual"}],
             "permissionsByEnv":{"global":["dashboard:read"],"3":["containers:start","containers:stop"]},
             "canDelete":true,"requiresPasswordChange":false,
             "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertFalse(user.isGlobalAdmin)
        assertTrue(user.hasPermission(Permission.Containers.START, environmentId = "3"))
        assertTrue(user.hasPermission(Permission.Containers.STOP, environmentId = "3"))
        assertFalse(user.hasPermission(Permission.Containers.START, environmentId = "4"))
        assertTrue(user.hasPermission(Permission.Dashboard.READ))
        assertTrue(user.hasPermission(Permission.Dashboard.READ, environmentId = "3"))
        assertFalse(user.hasPermission(Permission.Containers.START)) // global query, only env-scoped
        assertEquals(listOf("role_deployer"), user.roles)
    }

    @Test
    fun decodeV2NoPerms() {
        val user = json.decodeFromString<User>(
            """
            {"id":"u5","username":"stranger","roleAssignments":[],"permissionsByEnv":{},
             "canDelete":true,"requiresPasswordChange":false,"createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertFalse(user.isGlobalAdmin)
        assertFalse(user.hasPermission(Permission.Containers.START))
        assertEquals(emptyList(), user.roles)
    }

    @Test
    fun v2SudoWildcardEnvScoped() {
        val user = json.decodeFromString<User>(
            """
            {"id":"u6","username":"envadmin",
             "roleAssignments":[{"roleId":"role_admin","environmentId":"3","source":"manual"}],
             "permissionsByEnv":{"3":["*"]},
             "canDelete":true,"requiresPasswordChange":false,"createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertFalse(user.isGlobalAdmin) // sudo is env-scoped, not global
        assertTrue(user.hasPermission("anything:goes", environmentId = "3"))
        assertFalse(user.hasPermission(Permission.Containers.START, environmentId = "4"))
        assertFalse(user.hasPermission(Permission.Containers.START)) // global query
    }

    @Test
    fun v2DedupesRolesAcrossEnvs() {
        val user = json.decodeFromString<User>(
            """
            {"id":"u7","username":"multi",
             "roleAssignments":[
               {"roleId":"role_viewer","environmentId":"3","source":"manual"},
               {"roleId":"role_viewer","environmentId":"4","source":"manual"},
               {"roleId":"role_deployer","environmentId":"5","source":"oidc"}],
             "permissionsByEnv":{},"canDelete":true,"requiresPasswordChange":false,
             "createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertEquals(listOf("role_viewer", "role_deployer"), user.roles) // deduped, order preserved
    }

    @Test
    fun capabilitiesDetectV1() {
        val user = User(id = "u", username = "a", roles = listOf("admin"))
        assertEquals(ServerCapabilities.Mode.LEGACY_ROLES, ServerCapabilities.detect(user))
    }

    @Test
    fun capabilitiesDetectV2WithAssignments() {
        val user = User(id = "u", username = "a", roleAssignments = listOf(RoleAssignmentSummary(roleId = "role_admin")))
        assertEquals(ServerCapabilities.Mode.RBAC, ServerCapabilities.detect(user))
    }

    @Test
    fun capabilitiesDetectV2WithPermissionsOnly() {
        val user = User(id = "u", username = "a", permissionsByEnv = mapOf("global" to listOf("*")))
        assertEquals(ServerCapabilities.Mode.RBAC, ServerCapabilities.detect(user))
    }

    @Test
    fun createUserOmitsNilRoles() {
        val out = json.encodeToString(CreateUser(username = "alice", password = "12345678", roles = null))
        assertFalse(out.contains("\"roles\""), "Expected no roles key when null; got: $out")
        assertTrue(out.contains("\"username\""))
        assertTrue(out.contains("\"password\""))
    }

    @Test
    fun createUserIncludesRolesWhenSet() {
        val out = json.encodeToString(CreateUser(username = "alice", password = "12345678", roles = listOf("admin")))
        assertTrue(out.contains("\"roles\""))
        assertTrue(out.contains("\"admin\""))
    }

    @Test
    fun updateUserOmitsNilRoles() {
        val out = json.encodeToString(UpdateUser(displayName = "Alice", roles = null))
        assertFalse(out.contains("\"roles\""))
    }

    @Test
    fun roundTripV2User() {
        val original = User(
            id = "u8",
            username = "rt",
            email = "rt@example.com",
            roles = listOf("role_editor"),
            canDelete = true,
            requiresPasswordChange = false,
            roleAssignments = listOf(RoleAssignmentSummary(roleId = "role_editor", environmentId = "3", source = "manual")),
            permissionsByEnv = mapOf("global" to listOf("dashboard:read"), "3" to listOf("containers:start")),
        )
        val decoded = json.decodeFromString<User>(json.encodeToString(original))
        assertEquals(original.id, decoded.id)
        assertEquals(1, decoded.roleAssignments?.size)
        assertEquals(listOf("dashboard:read"), decoded.permissionsByEnv?.get("global"))
        assertEquals(listOf("containers:start"), decoded.permissionsByEnv?.get("3"))
        assertTrue(decoded.hasPermission(Permission.Containers.START, environmentId = "3"))
    }
}
