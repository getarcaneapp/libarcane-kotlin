package app.getarcane.sdk.models.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Distinguishes network nodes from container nodes. Mirrors Swift `TopologyNodeType`. */
@Serializable
public enum class TopologyNodeType(public val wire: String) {
    @SerialName("network")
    NETWORK("network"),

    @SerialName("container")
    CONTAINER("container"),
}

/** Additional context shown by the UI. Mirrors Swift `TopologyNodeMetadata`. */
@Serializable
public data class TopologyNodeMetadata(
    public val driver: String? = null,
    public val scope: String? = null,
    public val status: String? = null,
    public val image: String? = null,
    public val isDefault: Boolean? = null,
)

/** A single node (network or container) in the topology graph. Mirrors Swift `TopologyNode`. */
@Serializable
public data class TopologyNode(
    public val id: String,
    public val name: String,
    public val type: TopologyNodeType,
    public val metadata: TopologyNodeMetadata = TopologyNodeMetadata(),
)

/** A directed edge from a network node to a container node. Mirrors Swift `TopologyEdge`. */
@Serializable
public data class TopologyEdge(
    public val id: String,
    public val source: String,
    public val target: String,
    public val ipv4Address: String? = null,
    public val ipv6Address: String? = null,
)

/** The full topology graph for a Docker environment. Mirrors Swift `NetworkTopology`. */
@Serializable
public data class NetworkTopology(
    public val nodes: List<TopologyNode> = emptyList(),
    public val edges: List<TopologyEdge> = emptyList(),
)
