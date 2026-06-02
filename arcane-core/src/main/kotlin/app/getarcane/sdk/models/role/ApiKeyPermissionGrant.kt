package app.getarcane.sdk.models.role

import kotlinx.serialization.Serializable

/**
 * One permission grant on an API key, optionally scoped to a single environment. API keys carry
 * independent permission sets (not inherited from the owner).
 */
@Serializable
public data class ApiKeyPermissionGrant(
    public val permission: String,
    public val environmentId: String? = null,
)
