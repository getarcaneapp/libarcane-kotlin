package app.getarcane.sdk.models.system

import kotlinx.serialization.Serializable

/** Health-check response. `status` is "UP" when healthy. Mirrors Swift `HealthResponse`. */
@Serializable
public data class HealthResponse(
    public val status: String,
)
