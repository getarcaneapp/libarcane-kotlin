package app.getarcane.sdk.http

import kotlinx.serialization.Serializable

/**
 * Standard Arcane success envelope `{ "success": true, "data": <T> }`. The transport strips the
 * envelope and returns `data` to callers.
 */
@Serializable
public data class ApiResponse<T>(
    public val success: Boolean,
    public val data: T,
)
