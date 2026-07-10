package app.getarcane.sdk.models.dashboard

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Event kinds emitted by the aggregated dashboard stream. */
@Serializable
public enum class DashboardStreamEventType(public val wire: String) {
    @SerialName("snapshot")
    SNAPSHOT("snapshot"),

    @SerialName("pending")
    PENDING("pending"),

    @SerialName("heartbeat")
    HEARTBEAT("heartbeat"),

    @SerialName("error")
    ERROR("error"),

    @SerialName("unknown")
    UNKNOWN("unknown"),
}

/** Classified failure reasons carried by dashboard stream error events. */
@Serializable(with = DashboardStreamErrorCodeSerializer::class)
public enum class DashboardStreamErrorCode(public val wire: String) {
    @SerialName("agent_incompatible")
    AGENT_INCOMPATIBLE("agent_incompatible"),

    @SerialName("unreachable")
    UNREACHABLE("unreachable"),

    @SerialName("unknown")
    UNKNOWN("unknown"),
}

internal object DashboardStreamErrorCodeSerializer : KSerializer<DashboardStreamErrorCode> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DashboardStreamErrorCode", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DashboardStreamErrorCode {
        val raw = decoder.decodeString()
        return DashboardStreamErrorCode.entries.firstOrNull { it.wire == raw }
            ?: DashboardStreamErrorCode.UNKNOWN
    }

    override fun serialize(encoder: Encoder, value: DashboardStreamErrorCode) {
        encoder.encodeString(value.wire)
    }
}

/** One line from the aggregated dashboard NDJSON stream. */
@Serializable
public data class DashboardStreamEvent(
    public val type: DashboardStreamEventType = DashboardStreamEventType.UNKNOWN,
    public val environmentId: String? = null,
    public val snapshot: DashboardSnapshot? = null,
    public val error: String? = null,
    public val errorCode: DashboardStreamErrorCode? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val timestamp: Instant,
) {
    /** Environment the event applies to; empty IDs normalize to the local Docker environment. */
    public val resolvedEnvironmentId: EnvironmentId
        get() = environmentId?.takeIf { it.isNotEmpty() }?.let(::EnvironmentId) ?: EnvironmentId.LOCAL_DOCKER
}
