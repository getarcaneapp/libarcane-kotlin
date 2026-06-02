package app.getarcane.sdk.models.container

import kotlinx.serialization.Serializable

/** A single probe result from the healthcheck log. */
@Serializable
public data class ContainerHealthLogEntry(
    public val start: String? = null,
    public val end: String? = null,
    public val exitCode: Int,
    public val output: String? = null,
)

/** Current healthcheck state of a container. */
@Serializable
public data class ContainerHealth(
    public val status: String,
    public val failingStreak: Int,
    public val log: List<ContainerHealthLogEntry>? = null,
)

/** Container healthcheck configuration. Duration values are expressed in nanoseconds. */
@Serializable
public data class ContainerHealthcheck(
    public val test: List<String>? = null,
    public val interval: Long? = null,
    public val timeout: Long? = null,
    public val startPeriod: Long? = null,
    public val startInterval: Long? = null,
    public val retries: Int? = null,
)

/** State of a container. */
@Serializable
public data class ContainerState(
    public val status: String,
    public val running: Boolean,
    public val exitCode: Int? = null,
    public val startedAt: String? = null,
    public val finishedAt: String? = null,
    public val health: ContainerHealth? = null,
)

/** Configuration details for a container. */
@Serializable
public data class ContainerConfig(
    public val env: List<String>? = null,
    public val cmd: List<String>? = null,
    public val entrypoint: List<String>? = null,
    public val workingDir: String? = null,
    public val user: String? = null,
    public val healthcheck: ContainerHealthcheck? = null,
)

/** Docker Compose project information extracted from container labels. */
@Serializable
public data class ContainerComposeInfo(
    public val projectName: String,
    public val serviceName: String,
    public val workingDir: String? = null,
    public val configFiles: String? = null,
)
