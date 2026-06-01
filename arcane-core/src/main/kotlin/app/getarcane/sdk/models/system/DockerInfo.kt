package app.getarcane.sdk.models.system

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.models.base.boolValue
import app.getarcane.sdk.models.base.int64Value
import app.getarcane.sdk.models.base.intValue
import app.getarcane.sdk.models.base.stringValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Docker daemon version + system info as returned by `/system/docker/info`. Mirrors Swift
 * `DockerInfo` (Models/System/DockerInfo.swift).
 *
 * The fields below the top-level metadata mirror the Docker SDK's `system.Info` struct, which the
 * Go API embeds verbatim. Rather than re-modeling every Docker SDK field, the merged object is
 * exposed as a dictionary of [JsonValue] via [info]; use the strongly-typed top-level fields for
 * the values Arcane massages itself. Decoding/encoding follows Swift's catch-all single-value
 * container via [DockerInfoSerializer].
 */
@Serializable(with = DockerInfoSerializer::class)
public data class DockerInfo(
    public val success: Boolean = false,
    public val apiVersion: String = "",
    public val gitCommit: String = "",
    public val goVersion: String = "",
    public val os: String = "",
    public val arch: String = "",
    public val buildTime: String = "",
    /**
     * Raw Docker SDK info payload. Null when the API did not embed the info document (older
     * deployments) — generally populated.
     */
    public val info: Map<String, JsonValue>? = null,
) {
    /** Convenience access to commonly-used fields embedded from `system.Info`. */
    public val ncpu: Int? get() = info?.get("NCPU")?.intValue

    public val memTotal: Long? get() = info?.get("MemTotal")?.int64Value

    public val name: String? get() = info?.get("Name")?.stringValue

    public val serverVersion: String? get() = info?.get("ServerVersion")?.stringValue

    public val operatingSystem: String? get() = info?.get("OperatingSystem")?.stringValue

    public companion object {
        internal val TOP_LEVEL_KEYS: Set<String> =
            setOf("success", "apiVersion", "gitCommit", "goVersion", "os", "arch", "buildTime")
    }
}

/**
 * Reproduces Swift `DockerInfo.init(from:)`/`encode(to:)`: decode the whole payload as a
 * `[String: JsonValue]` map, pull the known top-level fields, and stuff the remainder into
 * [DockerInfo.info]. On encode, the known fields plus [DockerInfo.info] are merged back into a flat
 * object.
 */
public object DockerInfoSerializer : KSerializer<DockerInfo> {
    private val mapSer = MapSerializer(String.serializer(), JsonValue.serializer())

    override val descriptor: SerialDescriptor = mapSer.descriptor

    override fun deserialize(decoder: Decoder): DockerInfo {
        val raw = decoder.decodeSerializableValue(mapSer)
        val rest = raw.filterKeys { it !in DockerInfo.TOP_LEVEL_KEYS }
        return DockerInfo(
            success = raw["success"]?.boolValue ?: false,
            apiVersion = raw["apiVersion"]?.stringValue ?: "",
            gitCommit = raw["gitCommit"]?.stringValue ?: "",
            goVersion = raw["goVersion"]?.stringValue ?: "",
            os = raw["os"]?.stringValue ?: "",
            arch = raw["arch"]?.stringValue ?: "",
            buildTime = raw["buildTime"]?.stringValue ?: "",
            info = rest.ifEmpty { null },
        )
    }

    override fun serialize(encoder: Encoder, value: DockerInfo) {
        val out = LinkedHashMap<String, JsonValue>()
        out["success"] = JsonValue.Bool(value.success)
        out["apiVersion"] = JsonValue.Str(value.apiVersion)
        out["gitCommit"] = JsonValue.Str(value.gitCommit)
        out["goVersion"] = JsonValue.Str(value.goVersion)
        out["os"] = JsonValue.Str(value.os)
        out["arch"] = JsonValue.Str(value.arch)
        out["buildTime"] = JsonValue.Str(value.buildTime)
        value.info?.forEach { (key, v) ->
            if (key !in DockerInfo.TOP_LEVEL_KEYS) out[key] = v
        }
        encoder.encodeSerializableValue(mapSer, out)
    }
}
