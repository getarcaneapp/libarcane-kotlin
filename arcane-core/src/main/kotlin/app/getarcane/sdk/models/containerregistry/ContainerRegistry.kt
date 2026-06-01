package app.getarcane.sdk.models.containerregistry

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Mirrors Swift `ContainerRegistry` (Models/containerregistry/ContainerRegistry.swift). */
@Serializable
public data class ContainerRegistry(
    public val id: String,
    public val url: String,
    public val username: String,
    public val description: String? = null,
    public val insecure: Boolean,
    public val enabled: Boolean,
    public val registryType: String,
    public val awsAccessKeyId: String? = null,
    public val awsRegion: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/** Mirrors Swift `CreateContainerRegistry`. */
@Serializable
public data class CreateContainerRegistry(
    public val url: String,
    public val username: String? = null,
    public val token: String? = null,
    public val description: String? = null,
    public val insecure: Boolean? = null,
    public val enabled: Boolean? = null,
    public val registryType: String? = null,
    public val awsAccessKeyId: String? = null,
    public val awsSecretAccessKey: String? = null,
    public val awsRegion: String? = null,
)

/** Mirrors Swift `UpdateContainerRegistry`. */
@Serializable
public data class UpdateContainerRegistry(
    public val url: String? = null,
    public val username: String? = null,
    public val token: String? = null,
    public val description: String? = null,
    public val insecure: Boolean? = null,
    public val enabled: Boolean? = null,
    public val registryType: String? = null,
    public val awsAccessKeyId: String? = null,
    public val awsSecretAccessKey: String? = null,
    public val awsRegion: String? = null,
)

/** Mirrors Swift `ContainerRegistryPullUsageResponse`. */
@Serializable
public data class ContainerRegistryPullUsageResponse(
    public val registries: List<ContainerRegistryPullUsage>,
)

/** Mirrors Swift `ContainerRegistryPullUsage`. */
@Serializable
public data class ContainerRegistryPullUsage(
    public val registryId: String,
    public val provider: String,
    public val registry: String,
    public val displayName: String,
    public val repository: String? = null,
    public val limit: Int? = null,
    public val remaining: Int? = null,
    public val used: Int? = null,
    public val windowSeconds: Int? = null,
    public val observedPulls: Long,
    public val authMethod: String,
    public val authUsername: String? = null,
    public val source: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val checkedAt: Instant,
    public val error: String? = null,
)

/** Mirrors Swift `ContainerRegistrySync`. */
@Serializable
public data class ContainerRegistrySync(
    public val id: String,
    public val url: String,
    public val username: String,
    public val token: String,
    public val description: String? = null,
    public val insecure: Boolean,
    public val enabled: Boolean,
    public val registryType: String,
    public val awsAccessKeyId: String? = null,
    public val awsSecretAccessKey: String? = null,
    public val awsRegion: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
)

/** Mirrors Swift `ContainerRegistrySyncRequest`. */
@Serializable
public data class ContainerRegistrySyncRequest(
    public val registries: List<ContainerRegistrySync>,
)

/** Mirrors Swift `ContainerRegistryCredential`. */
@Serializable
public data class ContainerRegistryCredential(
    public val url: String,
    public val username: String,
    public val token: String,
    public val enabled: Boolean,
)
