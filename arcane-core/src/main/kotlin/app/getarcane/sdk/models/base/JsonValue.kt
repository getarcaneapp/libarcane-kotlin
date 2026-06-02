package app.getarcane.sdk.models.base

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * A dynamically-typed JSON value. Used for open/polymorphic fields the SDK does not model strictly
 * (labels, `exposedPorts`, Docker `info`, validation `details`, …).
 */
@Serializable(with = JsonValueSerializer::class)
public sealed interface JsonValue {
    public data object Null : JsonValue
    public data class Bool(public val value: Boolean) : JsonValue
    public data class Number(public val value: Double) : JsonValue
    public data class Str(public val value: String) : JsonValue
    public data class Arr(public val value: List<JsonValue>) : JsonValue
    public data class Obj(public val value: Map<String, JsonValue>) : JsonValue

    public companion object {
        public fun from(element: JsonElement): JsonValue = element.toJsonValue()
    }
}

// Accessor helpers for reading typed values out of a JsonValue, used by DockerInfo etc.
public val JsonValue.stringValue: String? get() = (this as? JsonValue.Str)?.value
public val JsonValue.boolValue: Boolean? get() = (this as? JsonValue.Bool)?.value
public val JsonValue.doubleValue: Double? get() = (this as? JsonValue.Number)?.value
public val JsonValue.intValue: Int? get() = (this as? JsonValue.Number)?.value?.toInt()
public val JsonValue.int64Value: Long? get() = (this as? JsonValue.Number)?.value?.toLong()
public val JsonValue.arrayValue: List<JsonValue>? get() = (this as? JsonValue.Arr)?.value
public val JsonValue.objectValue: Map<String, JsonValue>? get() = (this as? JsonValue.Obj)?.value

internal fun JsonElement.toJsonValue(): JsonValue = when (this) {
    is JsonNull -> JsonValue.Null
    is JsonPrimitive -> when {
        isString -> JsonValue.Str(content)
        booleanOrNull != null -> JsonValue.Bool(booleanOrNull!!)
        else -> JsonValue.Number(doubleOrNull ?: content.toDouble())
    }
    is JsonArray -> JsonValue.Arr(map { it.toJsonValue() })
    is JsonObject -> JsonValue.Obj(mapValues { it.value.toJsonValue() })
}

internal fun JsonValue.toJsonElement(): JsonElement = when (this) {
    JsonValue.Null -> JsonNull
    is JsonValue.Bool -> JsonPrimitive(value)
    is JsonValue.Number -> JsonPrimitive(value)
    is JsonValue.Str -> JsonPrimitive(value)
    is JsonValue.Arr -> JsonArray(value.map { it.toJsonElement() })
    is JsonValue.Obj -> JsonObject(value.mapValues { it.value.toJsonElement() })
}

public object JsonValueSerializer : KSerializer<JsonValue> {
    private val delegate = JsonElement.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: JsonValue) {
        require(encoder is JsonEncoder) { "JsonValue can only be serialized with kotlinx Json" }
        encoder.encodeJsonElement(value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): JsonValue {
        require(decoder is JsonDecoder) { "JsonValue can only be deserialized with kotlinx Json" }
        return decoder.decodeJsonElement().toJsonValue()
    }
}
