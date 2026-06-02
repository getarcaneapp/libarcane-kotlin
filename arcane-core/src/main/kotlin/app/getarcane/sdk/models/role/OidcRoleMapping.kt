package app.getarcane.sdk.models.role

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Origin of an OIDC role mapping: created manually or derived from environment configuration. */
@Serializable
public enum class OidcRoleMappingSource(public val wire: String) {
    @SerialName("manual")
    MANUAL("manual"),

    @SerialName("env")
    ENV("env"),
}

/** Maps an OIDC claim value to a role. */
@Serializable
public data class OidcRoleMapping(
    public val id: String,
    public val claimValue: String,
    public val roleId: String,
    public val environmentId: String? = null,
    public val source: String = "manual",
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
) {
    public val sourceKind: OidcRoleMappingSource?
        get() = OidcRoleMappingSource.entries.firstOrNull { it.wire == source }
}

/** Request body to create an OIDC role mapping. */
@Serializable
public data class CreateOidcRoleMapping(
    public val claimValue: String,
    public val roleId: String,
    public val environmentId: String? = null,
)

/** Request body to update an existing OIDC role mapping. */
@Serializable
public data class UpdateOidcRoleMapping(
    public val claimValue: String,
    public val roleId: String,
    public val environmentId: String? = null,
)
