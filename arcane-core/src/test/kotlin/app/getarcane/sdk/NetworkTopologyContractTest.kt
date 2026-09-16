package app.getarcane.sdk

import app.getarcane.sdk.models.network.NetworkTopology
import app.getarcane.sdk.models.network.TopologyNodeType
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkTopologyContractTest {
    @Test
    fun unknownAndMissingNodeTypesDecodeDefensively() {
        val topology = ArcaneJson.default.decodeFromString<NetworkTopology>(
            """
            {
              "nodes": [
                {"id":"future","name":"Future","type":"service"},
                {"id":"missing","name":"Missing"},
                {"id":"network","name":"Bridge","type":"network"}
              ],
              "edges": []
            }
            """.trimIndent(),
        )

        assertEquals(TopologyNodeType.UNKNOWN, topology.nodes[0].type)
        assertEquals(TopologyNodeType.UNKNOWN, topology.nodes[1].type)
        assertEquals(TopologyNodeType.NETWORK, topology.nodes[2].type)
    }
}
