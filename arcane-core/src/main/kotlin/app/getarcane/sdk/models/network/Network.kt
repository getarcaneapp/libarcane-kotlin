package app.getarcane.sdk.models.network

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.models.base.PaginationResponse
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** List/summary representation of a Docker network. Mirrors Swift `NetworkSummary`. */
@Serializable
public data class NetworkSummary(
    public val id: String,
    public val name: String,
    public val driver: String,
    public val scope: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val created: Instant,
    public val options: Map<String, String> = emptyMap(),
    public val labels: Map<String, String> = emptyMap(),
    public val inUse: Boolean = false,
    public val isDefault: Boolean = false,
)

/** Counts of networks by usage status. Mirrors Swift `NetworkUsageCounts`. */
@Serializable
public data class NetworkUsageCounts(
    public val inuse: Int = 0,
    public val unused: Int = 0,
    public val total: Int = 0,
)

/** A single subnet's IPAM configuration. Mirrors Swift `IPAMConfig`. */
@Serializable
public data class IPAMConfig(
    public val subnet: String? = null,
    public val gateway: String? = null,
    public val ipRange: String? = null,
    public val auxAddress: Map<String, String>? = null,
)

/** The IP Address Management configuration block. Mirrors Swift `IPAM`. */
@Serializable
public data class IPAM(
    public val driver: String? = null,
    public val options: Map<String, String>? = null,
    public val config: List<IPAMConfig>? = null,
)

/** One container attached to a network. Mirrors Swift `NetworkContainerEndpoint`. */
@Serializable
public data class NetworkContainerEndpoint(
    public val id: String,
    public val name: String,
    public val endpointId: String,
    public val ipv4Address: String = "",
    public val ipv6Address: String = "",
    public val macAddress: String = "",
)

/** The detailed inspect view of a Docker network. Mirrors Swift `NetworkInspect`. */
@Serializable
public data class NetworkInspect(
    public val id: String,
    public val name: String,
    public val driver: String,
    public val scope: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val created: Instant,
    public val enableIPv4: Boolean = false,
    public val enableIPv6: Boolean = false,
    public val ipam: IPAM,
    public val `internal`: Boolean = false,
    public val attachable: Boolean = false,
    public val ingress: Boolean = false,
    public val configFrom: Map<String, JsonValue>? = null,
    public val configOnly: Boolean = false,
    public val containers: Map<String, JsonValue> = emptyMap(),
    public val options: Map<String, String> = emptyMap(),
    public val labels: Map<String, String> = emptyMap(),
    public val peers: List<JsonValue>? = null,
    public val services: Map<String, JsonValue>? = null,
    public val containersList: List<NetworkContainerEndpoint> = emptyList(),
)

/** Configures a network create request. Mirrors Swift `NetworkCreateOptions`. */
@Serializable
public data class NetworkCreateOptions(
    public val driver: String? = null,
    public val checkDuplicate: Boolean? = null,
    public val `internal`: Boolean? = null,
    public val attachable: Boolean? = null,
    public val ingress: Boolean? = null,
    public val ipam: IPAM? = null,
    public val enableIPv6: Boolean? = null,
    public val options: Map<String, String>? = null,
    public val labels: Map<String, String>? = null,
)

/** Body for `POST environments/{id}/networks`. Mirrors Swift `NetworkCreateRequest`. */
@Serializable
public data class NetworkCreateRequest(
    public val name: String,
    public val options: NetworkCreateOptions = NetworkCreateOptions(),
)

/** Response of a network create request. Mirrors Swift `NetworkCreateResponse`. */
@Serializable
public data class NetworkCreateResponse(
    public val id: String,
    public val warning: String? = null,
)

/** Result of a network prune operation. Mirrors Swift `NetworkPruneReport`. */
@Serializable
public data class NetworkPruneReport(
    public val networksDeleted: List<String> = emptyList(),
    public val spaceReclaimed: ULong = 0u,
)

/** The page envelope returned by `GET /environments/{id}/networks`. Mirrors Swift `NetworkListPage`. */
@Serializable
public data class NetworkListPage(
    public val success: Boolean,
    public val data: List<NetworkSummary>,
    public val counts: NetworkUsageCounts,
    public val pagination: PaginationResponse,
)
