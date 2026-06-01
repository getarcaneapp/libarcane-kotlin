package app.getarcane.sdk.models.jobschedule

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** JobScheduleConfig represents the configured intervals (in minutes) for background jobs. Mirrors Swift `JobScheduleConfig`. */
@Serializable
public data class JobScheduleConfig(
    public val environmentHealthInterval: String,
    public val eventCleanupInterval: String,
    public val expiredSessionsCleanupInterval: String,
    public val autoUpdateInterval: String,
    public val dockerClientRefreshInterval: String,
    public val pollingInterval: String,
    public val scheduledPruneInterval: String,
    public val gitopsSyncInterval: String,
    public val vulnerabilityScanInterval: String,
    public val autoHealInterval: String,
)

/**
 * UpdateJobScheduleConfig updates job schedule intervals (in minutes). Null fields are ignored.
 * Mirrors Swift `UpdateJobScheduleConfig`.
 */
@Serializable
public data class UpdateJobScheduleConfig(
    public val environmentHealthInterval: String? = null,
    public val eventCleanupInterval: String? = null,
    public val expiredSessionsCleanupInterval: String? = null,
    public val autoUpdateInterval: String? = null,
    public val dockerClientRefreshInterval: String? = null,
    public val pollingInterval: String? = null,
    public val scheduledPruneInterval: String? = null,
    public val gitopsSyncInterval: String? = null,
    public val vulnerabilityScanInterval: String? = null,
    public val autoHealInterval: String? = null,
)

/** Status and metadata for a background job. Mirrors Swift `JobStatus`. */
@Serializable
public data class JobStatus(
    public val id: String,
    public val name: String,
    public val description: String,
    public val category: String,
    public val schedule: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val nextRun: Instant? = null,
    public val enabled: Boolean,
    public val managerOnly: Boolean,
    public val isContinuous: Boolean,
    public val canRunManually: Boolean,
    public val prerequisites: List<JobPrerequisite> = emptyList(),
    public val settingsKey: String? = null,
)

/** Mirrors Swift `JobPrerequisite`. */
@Serializable
public data class JobPrerequisite(
    public val settingKey: String,
    public val label: String,
    public val isMet: Boolean,
    public val settingsUrl: String? = null,
)

/** Mirrors Swift `JobListResponse`. */
@Serializable
public data class JobListResponse(
    public val jobs: List<JobStatus>,
    public val isAgent: Boolean,
)

/** Mirrors Swift `JobRunResponse`. */
@Serializable
public data class JobRunResponse(
    public val success: Boolean,
    public val message: String,
)
