package app.getarcane.sdk.models.container

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.models.containerregistry.ContainerRegistryCredential
import kotlinx.serialization.Serializable

/** Restart policy options for container creation. */
@Serializable
public data class ContainerRestartPolicyCreate(
    public val name: String? = null,
    public val maximumRetryCount: Int? = null,
)

/** Host port binding for container creation. */
@Serializable
public data class PortBindingCreate(
    public val hostIp: String? = null,
    public val hostPort: String? = null,
)

/** Host configuration for container creation. */
@Serializable
public data class HostConfigCreate(
    public val binds: List<String>? = null,
    public val portBindings: Map<String, List<PortBindingCreate>>? = null,
    public val restartPolicy: ContainerRestartPolicyCreate? = null,
    public val networkMode: String? = null,
    public val privileged: Boolean? = null,
    public val autoRemove: Boolean? = null,
    public val memory: Long? = null,
    public val memorySwap: Long? = null,
    public val nanoCpus: Long? = null,
    public val cpuShares: Long? = null,
    public val readonlyRootfs: Boolean? = null,
    public val publishAllPorts: Boolean? = null,
)

/** Network endpoint settings for container creation. */
@Serializable
public data class EndpointSettingsCreate(
    public val aliases: List<String>? = null,
)

/** Network configuration for container creation. */
@Serializable
public data class NetworkingConfigCreate(
    public val endpointsConfig: Map<String, EndpointSettingsCreate>? = null,
)

/** Container create request body. */
@Serializable
public data class ContainerCreate(
    public val name: String,
    public val image: String,
    public val command: List<String>? = null,
    public val cmd: List<String>? = null,
    public val entrypoint: List<String>? = null,
    public val workingDir: String? = null,
    public val user: String? = null,
    public val environment: List<String>? = null,
    public val env: List<String>? = null,
    public val labels: Map<String, String>? = null,
    public val exposedPorts: Map<String, JsonValue>? = null,
    public val hostConfig: HostConfigCreate? = null,
    public val networkingConfig: NetworkingConfigCreate? = null,
    public val hostname: String? = null,
    public val domainname: String? = null,
    public val attachStdout: Boolean? = null,
    public val attachStderr: Boolean? = null,
    public val attachStdin: Boolean? = null,
    public val tty: Boolean? = null,
    public val openStdin: Boolean? = null,
    public val stdinOnce: Boolean? = null,
    public val networkDisabled: Boolean? = null,
    public val ports: Map<String, String>? = null,
    public val volumes: List<String>? = null,
    public val networks: List<String>? = null,
    public val restartPolicy: String? = null,
    public val privileged: Boolean? = null,
    public val autoRemove: Boolean? = null,
    public val memory: Long? = null,
    public val cpus: Double? = null,
    public val credentials: List<ContainerRegistryCredential>? = null,
)
