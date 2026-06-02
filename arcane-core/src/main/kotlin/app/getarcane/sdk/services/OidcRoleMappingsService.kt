package app.getarcane.sdk.services

import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.models.role.CreateOidcRoleMapping
import app.getarcane.sdk.models.role.OidcRoleMapping
import app.getarcane.sdk.models.role.UpdateOidcRoleMapping

/**
 * Manages OIDC claim-value → role mappings.
 * Mappings with `source == "env"` are declared via the `OIDC_ROLE_MAPPINGS` env var and are
 * read-only at runtime (update/delete against them return 403). Available only on v2 RBAC servers.
 */
public class OidcRoleMappingsService internal constructor(private val rest: RestService) {
    /** List every OIDC role mapping. Not paginated. */
    public suspend fun list(): List<OidcRoleMapping> = rest.get("oidc/role-mappings")

    public suspend fun create(body: CreateOidcRoleMapping): OidcRoleMapping =
        rest.post("oidc/role-mappings", body = body)

    public suspend fun update(id: String, body: UpdateOidcRoleMapping): OidcRoleMapping =
        rest.put("oidc/role-mappings/$id", body = body)

    public suspend fun delete(id: String) {
        rest.deleteVoid("oidc/role-mappings/$id")
    }
}
