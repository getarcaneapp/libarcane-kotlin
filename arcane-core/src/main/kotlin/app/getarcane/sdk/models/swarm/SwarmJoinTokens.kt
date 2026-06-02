package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.Serializable

/** Body for `POST /environments/{id}/swarm/init`; the Docker `spec` blob is preserved as raw JSON. */
@Serializable
public data class SwarmInitRequest(
    public val listenAddr: String? = null,
    public val advertiseAddr: String? = null,
    public val dataPathAddr: String? = null,
    public val dataPathPort: UInt? = null,
    public val forceNewCluster: Boolean? = null,
    public val spec: JsonValue,
    public val autoLockManagers: Boolean? = null,
    public val availability: String? = null,
    public val defaultAddrPool: List<String>? = null,
    public val subnetSize: UInt? = null,
)

/** Response from `POST /environments/{id}/swarm/init`. */
@Serializable
public data class SwarmInitResponse(
    public val nodeId: String,
)

/** Body for `POST /environments/{id}/swarm/join`. */
@Serializable
public data class SwarmJoinRequest(
    public val listenAddr: String? = null,
    public val advertiseAddr: String? = null,
    public val dataPathAddr: String? = null,
    public val remoteAddrs: List<String>,
    public val joinToken: String,
    public val availability: String? = null,
)

/** Body for `POST /environments/{id}/swarm/leave`. */
@Serializable
public data class SwarmLeaveRequest(
    public val force: Boolean? = null,
)

/** Body for `POST /environments/{id}/swarm/unlock`. */
@Serializable
public data class SwarmUnlockRequest(
    public val key: String,
)

/** Response from `GET /environments/{id}/swarm/unlock-key`. */
@Serializable
public data class SwarmUnlockKeyResponse(
    public val unlockKey: String,
)

/** Worker / manager join tokens for the swarm. */
@Serializable
public data class SwarmJoinTokens(
    public val worker: String,
    public val manager: String,
)

/** Body for `POST /environments/{id}/swarm/join-tokens/rotate`. */
@Serializable
public data class SwarmRotateJoinTokensRequest(
    public val rotateWorkerToken: Boolean? = null,
    public val rotateManagerToken: Boolean? = null,
)

/** Body for `PUT /environments/{id}/swarm/spec`. */
@Serializable
public data class SwarmUpdateRequest(
    public val version: ULong? = null,
    public val spec: JsonValue,
    public val rotateWorkerToken: Boolean? = null,
    public val rotateManagerToken: Boolean? = null,
    public val rotateManagerUnlockKey: Boolean? = null,
)
