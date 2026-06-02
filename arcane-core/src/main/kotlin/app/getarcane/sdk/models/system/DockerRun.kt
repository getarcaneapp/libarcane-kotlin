package app.getarcane.sdk.models.system

import kotlinx.serialization.Serializable

/** Parsed representation of a `docker run` command. */
@Serializable
public data class DockerRunCommand(
    public val image: String,
    public val name: String? = null,
    public val ports: List<String>? = null,
    public val volumes: List<String>? = null,
    public val environment: List<String>? = null,
    public val networks: List<String>? = null,
    public val restart: String? = null,
    public val workdir: String? = null,
    public val user: String? = null,
    public val entrypoint: String? = null,
    public val command: String? = null,
    public val detached: Boolean? = null,
    public val interactive: Boolean? = null,
    public val tty: Boolean? = null,
    public val remove: Boolean? = null,
    public val privileged: Boolean? = null,
    public val labels: List<String>? = null,
    public val healthCheck: String? = null,
    public val memoryLimit: String? = null,
    public val cpuLimit: String? = null,
)

/** Body for `POST /environments/{id}/system/convert`. */
@Serializable
public data class ConvertDockerRunRequest(
    public val dockerRunCommand: String,
)

/** Result of converting a `docker run` command to compose form. */
@Serializable
public data class ConvertDockerRunResponse(
    public val success: Boolean,
    public val dockerCompose: String,
    public val envVars: String,
    public val serviceName: String,
)
