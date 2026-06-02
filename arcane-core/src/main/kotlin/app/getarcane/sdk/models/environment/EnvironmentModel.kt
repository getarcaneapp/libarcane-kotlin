package app.getarcane.sdk.models.environment

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * EdgeMTLSCertificate is the lightweight mTLS certificate status for an edge environment surfaced
 * in environment list/detail responses.
 */
@Serializable
public data class EdgeMTLSCertificate(
    public val commonName: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant? = null,
    public val daysRemaining: Int? = null,
    public val expired: Boolean = false,
    public val expiringSoon: Boolean = false,
)

/** Environment represents one Docker environment (local or edge). */
@Serializable
public data class Environment(
    public val id: String,
    public val name: String? = null,
    public val apiUrl: String,
    public val status: String,
    public val enabled: Boolean = true,
    public val isEdge: Boolean = false,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastSeen: Instant? = null,
    public val edgeTransport: String? = null,
    public val edgeSecurityMode: String? = null,
    public val edgeSessionId: String? = null,
    public val edgeAgentInstance: String? = null,
    public val edgeCapabilities: List<String>? = null,
    public val connected: Boolean? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val connectedAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastHeartbeat: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastPollAt: Instant? = null,
    public val edgeMTLSCertificate: EdgeMTLSCertificate? = null,
    public val apiKey: String? = null,
)

/** CreateEnvironment is the body for `POST /environments`. */
@Serializable
public data class CreateEnvironment(
    public val apiUrl: String,
    public val name: String? = null,
    public val enabled: Boolean? = null,
    public val accessToken: String? = null,
    public val bootstrapToken: String? = null,
    public val useApiKey: Boolean? = null,
    public val isEdge: Boolean? = null,
)

/** UpdateEnvironment is the body for `PUT /environments/{id}`. */
@Serializable
public data class UpdateEnvironment(
    public val apiUrl: String? = null,
    public val name: String? = null,
    public val enabled: Boolean? = null,
    public val accessToken: String? = null,
    public val bootstrapToken: String? = null,
    public val regenerateApiKey: Boolean? = null,
)

/** EnvironmentTestResult is the response of the test-connection endpoint. */
@Serializable
public data class EnvironmentTestResult(
    public val status: String,
    public val message: String? = null,
)

/** TestConnectionRequest is the body for the test-connection endpoint. */
@Serializable
public data class TestConnectionRequest(
    public val apiUrl: String? = null,
)

/** AgentPairRequest is the body for the local agent pairing endpoint. */
@Serializable
public data class AgentPairRequest(
    public val rotate: Boolean? = null,
)

/** AgentPairResponse is the response for the local agent pairing endpoint. */
@Serializable
public data class AgentPairResponse(
    public val token: String,
)

/** EnvironmentVersion is the response payload for the version endpoint. */
@Serializable
public data class EnvironmentVersion(
    public val currentVersion: String,
    public val currentTag: String? = null,
    public val currentDigest: String? = null,
    public val revision: String,
    public val shortRevision: String,
    public val goVersion: String,
    public val enabledFeatures: List<String>? = null,
    public val buildTime: String? = null,
    public val displayVersion: String,
    public val isSemverVersion: Boolean,
    public val newestVersion: String? = null,
    public val newestDigest: String? = null,
    public val updateAvailable: Boolean,
    public val releaseUrl: String? = null,
    public val releaseNotes: String? = null,
    public val releasedAt: String? = null,
)
