package app.getarcane.sdk.models.container

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Compact history sample for container CPU and memory usage. Mirrors Swift `ContainerStatsHistorySample`. */
@Serializable
public data class ContainerStatsHistorySample(
    public val cpuTenths: Int = 0,
    public val memoryTenths: Int = 0,
    public val memoryUsageBytes: Long = 0,
)

/**
 * Container stats websocket payload. Mirrors Docker's StatsResponse (embedded) plus Arcane-added
 * history. The Docker portion is exposed as raw JSON so we stay forward-compatible with the upstream
 * schema. Mirrors Swift `ContainerStatsPayload`, including its custom `init(from:)`/`encode(to:)`
 * which capture unknown keys into [raw]; see [ContainerStatsPayloadSerializer].
 */
@Serializable(with = ContainerStatsPayloadSerializer::class)
public data class ContainerStatsPayload(
    /** Raw Docker stats fields (anything not under `statsHistory` / `currentHistorySample`). */
    public val raw: Map<String, JsonValue> = emptyMap(),
    public val statsHistory: List<ContainerStatsHistorySample>? = null,
    public val currentHistorySample: ContainerStatsHistorySample = ContainerStatsHistorySample(),
)

/**
 * Custom serializer reproducing Swift `ContainerStatsPayload.init(from:)`/`encode(to:)`: the payload
 * is a single JSON object; `statsHistory` and `currentHistorySample` are pulled out and decoded, and
 * every other key is captured into [ContainerStatsPayload.raw]. Encoding merges [raw] back with the
 * two known keys.
 */
public object ContainerStatsPayloadSerializer : KSerializer<ContainerStatsPayload> {
    private val knownKeys = setOf("statsHistory", "currentHistorySample")
    private val elementSerializer = JsonElement.serializer()

    override val descriptor: SerialDescriptor = elementSerializer.descriptor

    override fun deserialize(decoder: Decoder): ContainerStatsPayload {
        require(decoder is JsonDecoder) { "ContainerStatsPayload can only be deserialized with kotlinx Json" }
        val json = decoder.json
        val obj = decoder.decodeJsonElement().jsonObject

        val statsHistory = obj["statsHistory"]?.let {
            json.decodeFromJsonElement(ListSerializer(ContainerStatsHistorySample.serializer()), it)
        }
        val currentHistorySample = obj["currentHistorySample"]?.let {
            json.decodeFromJsonElement(ContainerStatsHistorySample.serializer(), it)
        } ?: ContainerStatsHistorySample()

        val raw = obj
            .filterKeys { it !in knownKeys }
            .mapValues { JsonValue.from(it.value) }

        return ContainerStatsPayload(
            raw = raw,
            statsHistory = statsHistory,
            currentHistorySample = currentHistorySample,
        )
    }

    override fun serialize(encoder: Encoder, value: ContainerStatsPayload) {
        require(encoder is JsonEncoder) { "ContainerStatsPayload can only be serialized with kotlinx Json" }
        val json = encoder.json
        val merged = buildMap<String, JsonElement> {
            value.raw.forEach { (k, v) -> put(k, json.encodeToJsonElement(JsonValue.serializer(), v)) }
            value.statsHistory?.let {
                put("statsHistory", json.encodeToJsonElement(ListSerializer(ContainerStatsHistorySample.serializer()), it))
            }
            put(
                "currentHistorySample",
                json.encodeToJsonElement(ContainerStatsHistorySample.serializer(), value.currentHistorySample),
            )
        }
        encoder.encodeJsonElement(JsonObject(merged))
    }
}
