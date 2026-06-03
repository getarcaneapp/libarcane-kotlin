package app.getarcane.sdk.models.container

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.Serializable

/** A port binding for a container. */
@Serializable
public data class ContainerPort(
    public val ip: String? = null,
    public val privatePort: Int,
    public val publicPort: Int? = null,
    public val type: String,
)

/** A volume mount for a container. */
@Serializable
public data class ContainerMount(
    public val type: String,
    public val name: String? = null,
    public val source: String? = null,
    public val destination: String,
    public val driver: String? = null,
    public val mode: String? = null,
    public val rw: Boolean? = null,
    public val propagation: String? = null,
)

/** Network endpoint settings for a container. */
@Serializable
public data class ContainerNetworkEndpoint(
    public val ipamConfig: JsonValue? = null,
    public val links: List<String>? = null,
    public val aliases: List<String>? = null,
    public val macAddress: String? = null,
    public val driverOpts: Map<String, String>? = null,
    public val gwPriority: Int? = null,
    public val networkId: String? = null,
    public val endpointId: String? = null,
    public val gateway: String? = null,
    public val ipAddress: String? = null,
    public val ipPrefixLen: Int? = null,
    public val ipv6Gateway: String? = null,
    public val globalIPv6Address: String? = null,
    public val globalIPv6PrefixLen: Int? = null,
    public val dnsNames: List<String>? = null,
)

/** Network configuration for a container. */
@Serializable
public data class ContainerNetworkSettings(
    public val networks: Map<String, ContainerNetworkEndpoint> = emptyMap(),
)

/** Host configuration for a container. */
@Serializable
public data class ContainerHostConfig(
    public val networkMode: String? = null,
    public val restartPolicy: String? = null,
    public val privileged: Boolean? = null,
    public val autoRemove: Boolean? = null,
    public val nanoCpus: Long? = null,
    public val memory: Long? = null,
)
