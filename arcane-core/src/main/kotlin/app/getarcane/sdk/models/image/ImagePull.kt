package app.getarcane.sdk.models.image

import app.getarcane.sdk.models.containerregistry.ContainerRegistryCredential
import kotlinx.serialization.Serializable

/** Request body for the image pull endpoint. */
@Serializable
public data class ImagePullOptions(
    public val imageName: String,
    public val tag: String? = null,
    public val auth: ContainerRegistryCredential? = null,
    public val credentials: List<ContainerRegistryCredential>? = null,
)
