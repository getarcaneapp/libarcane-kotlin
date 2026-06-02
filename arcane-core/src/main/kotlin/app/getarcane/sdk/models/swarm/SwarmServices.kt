package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A published port mapping for a swarm service endpoint. */
@Serializable
public data class SwarmServicePort(
    @SerialName("protocol")
    public val protocolName: String,
    public val targetPort: UInt,
    public val publishedPort: UInt? = null,
    public val publishMode: String? = null,
)

/** A mount configured on a swarm service task. */
@Serializable
public data class SwarmServiceMount(
    public val type: String,
    public val source: String? = null,
    public val target: String,
    public val readOnly: Boolean? = null,
    public val volumeDriver: String? = null,
    public val volumeOptions: Map<String, String>? = null,
    public val devicePath: String? = null,
)

/**
 * Lightweight summary of a swarm service used in list views. Named `SwarmServiceSummary` to avoid
 * collision with the `SwarmService` facade.
 */
@Serializable
public data class SwarmServiceSummary(
    public val id: String,
    public val name: String,
    public val image: String,
    public val mode: String,
    public val replicas: ULong,
    public val runningReplicas: ULong,
    public val ports: List<SwarmServicePort> = emptyList(),
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
    public val labels: Map<String, String>? = null,
    public val stackName: String? = null,
    public val nodes: List<String> = emptyList(),
    public val networks: List<String> = emptyList(),
    public val mounts: List<SwarmServiceMount> = emptyList(),
)

/** A single IPAM configuration entry for a service-attached network. */
@Serializable
public data class SwarmServiceNetworkIPAMConfig(
    public val subnet: String? = null,
    public val gateway: String? = null,
    public val ipRange: String? = null,
)

/** Details about a config-only network referenced by `configFrom`. */
@Serializable
public data class SwarmServiceNetworkConfigDetail(
    public val name: String,
    public val driver: String,
    public val scope: String,
    public val enableIPv4: Boolean,
    public val enableIPv6: Boolean,
    public val options: Map<String, String>? = null,
    public val ipv4Configs: List<SwarmServiceNetworkIPAMConfig>? = null,
    public val ipv6Configs: List<SwarmServiceNetworkIPAMConfig>? = null,
)

/** Enriched network info for a service's attached network. */
@Serializable
public data class SwarmServiceNetworkDetail(
    public val id: String,
    public val name: String,
    public val driver: String,
    public val scope: String,
    @SerialName("internal")
    public val internalNetwork: Boolean,
    public val attachable: Boolean,
    public val ingress: Boolean,
    public val enableIPv4: Boolean,
    public val enableIPv6: Boolean,
    public val configFrom: String? = null,
    public val configOnly: Boolean,
    public val options: Map<String, String>? = null,
    public val ipamConfigs: List<SwarmServiceNetworkIPAMConfig>? = null,
    public val configNetwork: SwarmServiceNetworkConfigDetail? = null,
)

/**
 * Full inspect payload for a swarm service. Docker SDK sub-blobs (spec, endpoint, version,
 * updateStatus) are preserved verbatim as JSON.
 */
@Serializable
public data class SwarmServiceInspect(
    public val id: String,
    public val version: JsonValue,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant,
    public val spec: JsonValue,
    public val endpoint: JsonValue,
    public val updateStatus: JsonValue? = null,
    public val nodes: List<String>? = null,
    public val networkDetails: Map<String, SwarmServiceNetworkDetail>? = null,
    public val mounts: List<SwarmServiceMount>? = null,
)

/** Extra options used when creating a swarm service. */
@Serializable
public data class SwarmServiceCreateOptions(
    public val encodedRegistryAuth: String? = null,
    public val queryRegistry: Boolean? = null,
)

/** Extra options used when updating a swarm service. */
@Serializable
public data class SwarmServiceUpdateOptions(
    public val encodedRegistryAuth: String? = null,
    public val registryAuthFrom: String? = null,
    public val rollback: String? = null,
    public val queryRegistry: Boolean? = null,
)

/** Body for creating a swarm service; [spec] is the raw Docker ServiceSpec JSON. */
@Serializable
public data class SwarmServiceCreateRequest(
    public val spec: JsonValue,
    public val options: SwarmServiceCreateOptions? = null,
)

/** Body for updating a swarm service. */
@Serializable
public data class SwarmServiceUpdateRequest(
    public val version: ULong,
    public val spec: JsonValue,
    public val options: SwarmServiceUpdateOptions? = null,
)

/** Response for swarm service create. */
@Serializable
public data class SwarmServiceCreateResponse(
    public val id: String,
    public val warnings: List<String>? = null,
)

/** Response for swarm service update / rollback / scale. */
@Serializable
public data class SwarmServiceUpdateResponse(
    public val warnings: List<String>? = null,
)

/** Body for scaling a replicated swarm service. */
@Serializable
public data class SwarmServiceScaleRequest(
    public val replicas: ULong,
)
