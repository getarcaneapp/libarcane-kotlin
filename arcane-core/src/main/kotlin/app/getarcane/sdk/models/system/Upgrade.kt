package app.getarcane.sdk.models.system

import kotlinx.serialization.Serializable

/** Result of the system upgrade-availability check. */
@Serializable
public data class UpgradeCheckResult(
    public val canUpgrade: Boolean,
    public val error: Boolean,
    public val message: String,
)

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
