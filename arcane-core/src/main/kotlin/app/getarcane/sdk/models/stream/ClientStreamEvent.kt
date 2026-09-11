package app.getarcane.sdk.models.stream

import app.getarcane.sdk.models.activity.ActivityStreamEvent
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** Envelope emitted by Arcane's multiplexed `/stream` endpoint. */
@Serializable
public data class ClientStreamEvent(
    public val channel: String? = null,
    public val type: String? = null,
    public val activity: ActivityStreamEvent? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val timestamp: Instant,
)
