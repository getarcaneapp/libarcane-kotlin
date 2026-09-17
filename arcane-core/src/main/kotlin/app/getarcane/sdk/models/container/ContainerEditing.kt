package app.getarcane.sdk.models.container

import app.getarcane.sdk.models.containerregistry.ContainerRegistryCredential
import kotlinx.serialization.Serializable

/** Host edits: null preserves a field; empty values replace it. */
@Serializable
public data class HostConfigEdit(
    public val binds: List<String>? = null,
    public val mounts: List<ContainerMountCreate>? = null,
    public val portBindings: Map<String, List<PortBindingCreate>>? = null,
    public val restartPolicy: ContainerRestartPolicyCreate? = null,
    public val privileged: Boolean? = null,
    public val capAdd: List<String>? = null,
    public val capDrop: List<String>? = null,
    public val autoRemove: Boolean? = null,
    public val readonlyRootfs: Boolean? = null,
    public val memory: Long? = null,
    public val memorySwap: Long? = null,
    public val nanoCpus: Long? = null,
    public val cpuShares: Long? = null,
)

/** Configuration changes applied by recreating a standalone container. */
@Serializable
public data class ContainerEdit(
    public val name: String? = null,
    public val image: String? = null,
    public val workingDir: String? = null,
    public val user: String? = null,
    public val command: List<String>? = null,
    public val entrypoint: List<String>? = null,
    public val environment: List<String>? = null,
    public val labels: Map<String, String>? = null,
    public val healthcheck: ContainerHealthcheckCreate? = null,
    public val clearHealthcheck: Boolean? = null,
    public val hostConfig: HostConfigEdit? = null,
    public val networkingConfig: NetworkingConfigCreate? = null,
    public val credentials: List<ContainerRegistryCredential>? = null,
)

/** Editable snapshot, including Compose ownership and edit restrictions. */
@Serializable
public data class ContainerEditConfig(
    public val id: String,
    public val name: String,
    public val image: String,
    public val hostConfig: HostConfigCreate = HostConfigCreate(),
    public val running: Boolean = false,
    public val command: List<String>? = null,
    public val entrypoint: List<String>? = null,
    public val workingDir: String? = null,
    public val user: String? = null,
    public val environment: List<String>? = null,
    public val labels: Map<String, String>? = null,
    public val healthcheck: ContainerHealthcheckCreate? = null,
    public val networks: Map<String, EndpointSettingsCreate>? = null,
    public val isCompose: Boolean? = null,
    public val composeProject: String? = null,
    public val editDisabled: Boolean? = null,
)

/** Options for creating an image from a container filesystem. */
@Serializable
public data class ContainerCommitRequest(
    public val repository: String? = null,
    public val tag: String? = null,
    public val comment: String? = null,
    public val author: String? = null,
    public val changes: List<String>? = null,
    public val noPause: Boolean? = null,
)

@Serializable
public data class ContainerCommitResult(public val id: String)

@Serializable
public data class ContainerGenerateComposeRequest(public val containerIds: List<String>)

@Serializable
public data class ContainerGenerateComposeResponse(public val composeContent: String)
