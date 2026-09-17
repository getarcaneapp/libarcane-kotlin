package app.getarcane.sdk

import app.getarcane.sdk.models.role.OidcRoleMapping
import app.getarcane.sdk.models.role.OidcRoleMappingSource
import app.getarcane.sdk.models.role.AccessSurfaceAccessMode
import app.getarcane.sdk.models.role.AccessSurfaceKind
import app.getarcane.sdk.models.role.AccessSurfaceScopeMode
import app.getarcane.sdk.models.role.Permission
import app.getarcane.sdk.models.role.PermissionResourceScope
import app.getarcane.sdk.models.role.PermissionsManifest
import app.getarcane.sdk.models.role.Role
import app.getarcane.sdk.models.role.RoleAssignment
import app.getarcane.sdk.models.role.RoleAssignmentSource
import app.getarcane.sdk.models.role.SetUserAssignments
import app.getarcane.sdk.models.role.UserAssignmentInput
import app.getarcane.sdk.models.user.User
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Decoding/encoding of the role and permission models. */
class RoleModelsTest {
    private val json = ArcaneJson.default

    @Test
    fun decodeRole() {
        val role = json.decodeFromString<Role>(
            """
            {"id":"role_admin","name":"Admin","description":"Full administrative access",
             "permissions":["*"],"builtIn":true,"assignedUserCount":2,"createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertEquals(Role.BuiltIn.ADMIN, role.id)
        assertEquals("Admin", role.name)
        assertEquals(listOf("*"), role.permissions)
        assertTrue(role.builtIn)
        assertEquals(2, role.assignedUserCount)
        assertNull(role.updatedAt)
    }

    @Test
    fun roleDefaultsMissingFields() {
        val role = json.decodeFromString<Role>(
            """{"id":"role_custom","name":"Custom","createdAt":"2026-01-01T00:00:00Z"}""",
        )
        assertEquals(emptyList(), role.permissions)
        assertFalse(role.builtIn)
        assertEquals(0, role.assignedUserCount)
    }

    @Test
    fun decodeRoleAssignment() {
        val a = json.decodeFromString<RoleAssignment>(
            """
            {"id":"a1","userId":"u1","roleId":"role_deployer","environmentId":"3",
             "source":"manual","createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertEquals("3", a.environmentId)
        assertEquals(RoleAssignmentSource.MANUAL, a.sourceKind)
    }

    @Test
    fun decodeGlobalRoleAssignment() {
        val a = json.decodeFromString<RoleAssignment>(
            """{"id":"a2","userId":"u1","roleId":"role_admin","source":"oidc","createdAt":"2026-01-01T00:00:00Z"}""",
        )
        assertNull(a.environmentId)
        assertEquals(RoleAssignmentSource.OIDC, a.sourceKind)
    }

    @Test
    fun decodeOidcRoleMapping() {
        val m = json.decodeFromString<OidcRoleMapping>(
            """
            {"id":"m1","claimValue":"docker-admins","roleId":"role_admin","source":"manual",
             "createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertEquals("docker-admins", m.claimValue)
        assertNull(m.environmentId)
        assertEquals(OidcRoleMappingSource.MANUAL, m.sourceKind)
    }

    @Test
    fun decodeEnvDeclaredOidcMapping() {
        val m = json.decodeFromString<OidcRoleMapping>(
            """
            {"id":"m2","claimValue":"viewers","roleId":"role_viewer","environmentId":"3","source":"env",
             "createdAt":"2026-01-01T00:00:00Z"}
            """.trimIndent(),
        )
        assertEquals(OidcRoleMappingSource.ENV, m.sourceKind)
        assertEquals("3", m.environmentId)
    }

    @Test
    fun decodePermissionsManifest() {
        val manifest = json.decodeFromString<PermissionsManifest>(
            """
            {"resources":[
              {"key":"containers","label":"Containers","scope":"env","actions":[
                {"key":"start","permission":"containers:start","label":"Start","requires":["containers:inspect"]},
                {"key":"stop","permission":"containers:stop","label":"Stop","description":"Stop a container"}]},
              {"key":"users","label":"Users","scope":"global","actions":[
                {"key":"list","permission":"users:list","label":"List"}]}],
             "presets":[{"key":"viewer","label":"Viewer","permissions":["containers:list"]}],
             "accessSurfaces":[
               {"id":"route.containers","kind":"route","label":"Containers","accessMode":"permissions",
                "matchMode":"any-of","scopeMode":"selected-env-plus-global","permissions":["containers:list"]},
               {"id":"route.future","kind":"future-kind","label":"Future","accessMode":"future-mode",
                "matchMode":"all-of","scopeMode":"future-scope"}]}
            """.trimIndent(),
        )
        assertEquals(2, manifest.resources.size)
        assertEquals(PermissionResourceScope.ENV, manifest.resources[0].scopeKind)
        assertEquals(PermissionResourceScope.GLOBAL, manifest.resources[1].scopeKind)
        assertEquals("Stop a container", manifest.resources[0].actions[1].description)
        assertEquals(listOf("containers:inspect"), manifest.resources[0].actions[0].requires)
        assertEquals("viewer", manifest.presets.single().key)
        assertEquals(AccessSurfaceKind.ROUTE, manifest.accessSurfaces[0].kind)
        assertEquals("future-kind", manifest.accessSurfaces[1].kind.wire)
        assertEquals("future-mode", manifest.accessSurfaces[1].accessMode.wire)
    }

    @Test
    fun missingManifestAdditionsDefaultForOlderServers() {
        val manifest = json.decodeFromString<PermissionsManifest>("""{"resources":[]}""")
        assertTrue(manifest.presets.isEmpty())
        assertTrue(manifest.accessSurfaces.isEmpty())
    }

    @Test
    fun permissionSelectionIncludesTransitiveRequirements() {
        val manifest = json.decodeFromString<PermissionsManifest>(
            """
            {"resources":[{"key":"containers","label":"Containers","scope":"env","actions":[
              {"key":"edit","permission":"containers:edit","label":"Edit","requires":["containers:inspect"]},
              {"key":"inspect","permission":"containers:inspect","label":"Inspect","requires":["containers:list"]}
            ]}]}
            """.trimIndent(),
        )
        assertEquals(
            listOf("containers:edit", "images:list", "containers:inspect", "containers:list"),
            manifest.normalizePermissionSelection(listOf("containers:edit", "images:list", "containers:edit")),
        )
    }

    @Test
    fun accessSurfaceEvaluationHonorsModesScopesUnknownsAndCycles() {
        val manifest = json.decodeFromString<PermissionsManifest>(
            """
            {"accessSurfaces":[
              {"id":"selected","kind":"route","accessMode":"permissions","matchMode":"all-of",
               "scopeMode":"selected-env-plus-global","permissions":["containers:list","containers:inspect"]},
              {"id":"any-env","kind":"route","accessMode":"permissions","matchMode":"any-of",
               "scopeMode":"any-effective-scope","permissions":["containers:start"]},
              {"id":"parent","kind":"landing","accessMode":"any-child","matchMode":"any-of",
               "scopeMode":"global-only","children":["selected","cycle-a"]},
              {"id":"cycle-a","kind":"landing","accessMode":"any-child","matchMode":"any-of",
               "scopeMode":"global-only","children":["cycle-b"]},
              {"id":"cycle-b","kind":"landing","accessMode":"any-child","matchMode":"any-of",
               "scopeMode":"global-only","children":["cycle-a"]},
              {"id":"unknown","kind":"route","accessMode":"future","matchMode":"any-of",
               "scopeMode":"global-only","permissions":["*"]}
            ]}
            """.trimIndent(),
        )
        val user = User(
            id = "u1",
            username = "restricted",
            permissionsByEnv = mapOf(
                "global" to listOf("containers:list"),
                "env-a" to listOf("containers:inspect"),
                "env-b" to listOf("containers:start"),
            ),
        )
        assertTrue(manifest.canAccessSurface("selected", user, "env-a"))
        assertFalse(manifest.canAccessSurface("selected", user, "env-b"))
        assertTrue(manifest.canAccessSurface("any-env", user, "env-a"))
        assertTrue(manifest.canAccessSurface("parent", user, "env-a"))
        assertFalse(manifest.canAccessSurface("cycle-a", user, "env-a"))
        assertFalse(manifest.canAccessSurface("unknown", user, "env-a"))
        assertFalse(manifest.canAccessSurface("missing", user, "env-a"))
        assertEquals(AccessSurfaceAccessMode.PERMISSIONS, manifest.accessSurfaces[0].accessMode)
        assertEquals(
            AccessSurfaceScopeMode.SELECTED_ENVIRONMENT_PLUS_GLOBAL,
            manifest.accessSurfaces[0].scopeMode,
        )
    }

    @Test
    fun setUserAssignmentsEncoding() {
        val body = SetUserAssignments(
            assignments = listOf(
                UserAssignmentInput(roleId = Role.BuiltIn.ADMIN),
                UserAssignmentInput(roleId = Role.BuiltIn.DEPLOYER, environmentId = "3"),
            ),
        )
        val decoded = json.decodeFromString<SetUserAssignments>(json.encodeToString(body))
        assertEquals(2, decoded.assignments.size)
        assertNull(decoded.assignments[0].environmentId)
        assertEquals("3", decoded.assignments[1].environmentId)
    }

    @Test
    fun permissionConstants() {
        assertEquals("containers:start", Permission.Containers.START)
        assertEquals("roles:list", Permission.Roles.LIST)
        assertEquals("git-repositories:sync", Permission.GitRepositories.SYNC)
        assertEquals("*", Permission.SUDO)
        assertEquals("role_admin", Role.BuiltIn.ADMIN)
    }
}
