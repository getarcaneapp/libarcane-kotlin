package app.getarcane.sdk.streaming

import kotlinx.serialization.Serializable

/** A single log line from a container/project/service log stream. Mirrors Swift `LogLine`. */
@Serializable
public data class LogLine(
    public val text: String,
    public val seq: ULong? = null,
    public val level: String? = null,
    public val service: String? = null,
    public val timestamp: String? = null,
)

/**
 * A [LogLine] paired with a stable monotonic id assigned at receive time, suitable as a list item
 * (a raw [LogLine] has no unique identity). Mirrors Swift `IdentifiedLogLine`.
 */
public data class IdentifiedLogLine(
    public val id: ULong,
    public val line: LogLine,
)

/** Wire shape of a JSON log frame. Mirrors Swift's private `LogLineMessage`. */
@Serializable
internal data class LogLineMessage(
    val seq: ULong? = null,
    val level: String? = null,
    val message: String,
    val service: String? = null,
    val timestamp: String? = null,
)
