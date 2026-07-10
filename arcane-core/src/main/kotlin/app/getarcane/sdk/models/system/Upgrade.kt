package app.getarcane.sdk.models.system

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Result of the system upgrade-availability check. */
@Serializable
public data class UpgradeCheckResult(
    public val canUpgrade: Boolean,
    public val error: Boolean,
    public val message: String,
)

/** Status of a fleet-wide update-all job. */
@Serializable
public enum class EnvironmentUpdateJobStatus(public val wire: String) {
    @SerialName("running")
    RUNNING("running"),

    @SerialName("pending_restart")
    PENDING_RESTART("pending_restart"),

    @SerialName("completed")
    COMPLETED("completed"),

    @SerialName("failed")
    FAILED("failed"),

    @SerialName("unknown")
    UNKNOWN("unknown"),
}

/** Per-environment status within a fleet-wide update-all job. */
@Serializable
public enum class EnvironmentUpdateResultStatus(public val wire: String) {
    @SerialName("pending")
    PENDING("pending"),

    @SerialName("updating")
    UPDATING("updating"),

    @SerialName("updated")
    UPDATED("updated"),

    @SerialName("triggered")
    TRIGGERED("triggered"),

    @SerialName("skipped_offline")
    SKIPPED_OFFLINE("skipped_offline"),

    @SerialName("failed")
    FAILED("failed"),

    @SerialName("unknown")
    UNKNOWN("unknown"),
}

/** Outcome of one environment inside a fleet-wide update-all job. */
@Serializable
public data class EnvironmentUpdateResult(
    public val environmentId: String,
    public val environmentName: String,
    public val status: EnvironmentUpdateResultStatus = EnvironmentUpdateResultStatus.UNKNOWN,
    public val fromVersion: String? = null,
    public val toVersion: String? = null,
    public val error: String? = null,
) {
    public val id: String get() = environmentId
}

/** A fleet-wide Arcane update-all job. */
@Serializable
public data class EnvironmentUpdateJob(
    public val id: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
    public val status: EnvironmentUpdateJobStatus = EnvironmentUpdateJobStatus.UNKNOWN,
    public val userId: String? = null,
    public val username: String? = null,
    public val managerVersionAtStart: String? = null,
    public val managerDigestAtStart: String? = null,
    public val managerTargetVersion: String? = null,
    public val results: List<EnvironmentUpdateResult>? = null,
    public val error: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val completedAt: Instant? = null,
) {
    public val managerResult: EnvironmentUpdateResult?
        get() = results?.firstOrNull { it.environmentId == "0" }

    public val isTerminal: Boolean
        get() = status == EnvironmentUpdateJobStatus.COMPLETED || status == EnvironmentUpdateJobStatus.FAILED
}

/**
 * Result of a system-level container batch action (start/stop/etc.).
 */
@Serializable
public data class SystemContainerActionResult(
    public val started: List<String>? = null,
    public val stopped: List<String>? = null,
    public val failed: List<String>? = null,
    public val success: Boolean = false,
    public val errors: List<String>? = null,
)
