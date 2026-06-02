package app.getarcane.sdk.models.port

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A port mapping between a container and its host. */
@Serializable
public data class PortMapping(
    public val id: String,
    public val containerId: String,
    public val containerName: String,
    public val hostIp: String? = null,
    public val hostPort: Int? = null,
    public val containerPort: Int,
    @SerialName("protocol")
    public val protocolName: String,
    public val isPublished: Boolean,
)
