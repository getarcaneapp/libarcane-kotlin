package app.getarcane.sdk.models.system

import kotlinx.serialization.Serializable

/** Health-check response. `status` is "UP" when healthy. */
@Serializable
public data class HealthResponse(
    public val status: String,
)
