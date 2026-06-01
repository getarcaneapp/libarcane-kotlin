package app.getarcane.sdk.serialization

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Lenient ISO-8601 [Instant] serializer mirroring Swift's `ArcaneJSON.makeDecoder` date strategy
 * (ArcaneClient.swift): parse an internet date-time with **or** without fractional seconds, and
 * tolerate a space separator or a missing trailing `Z`. Encodes via [Instant.toString] (RFC-3339),
 * matching the Swift `.iso8601` encoding strategy.
 *
 * kotlinx-datetime's built-in `Instant` serializer already handles the common RFC-3339 forms the
 * Arcane backend emits (Go `time.RFC3339`), so plain `Instant` fields are fine for decoding server
 * payloads. Apply this serializer explicitly (`@Serializable(with = ArcaneInstantSerializer::class)`)
 * on fields that may receive looser input.
 */
public object ArcaneInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("app.getarcane.sdk.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant = parse(decoder.decodeString())

    private fun parse(raw: String): Instant {
        runCatching { return Instant.parse(raw) }
        val normalized = raw.trim().let { if (it.contains(' ')) it.replace(' ', 'T') else it }
        runCatching { return Instant.parse(normalized) }
        // No timezone designator -> assume UTC.
        runCatching { return Instant.parse("${normalized}Z") }
        throw IllegalArgumentException("Invalid ISO-8601 instant: \"$raw\"")
    }
}
