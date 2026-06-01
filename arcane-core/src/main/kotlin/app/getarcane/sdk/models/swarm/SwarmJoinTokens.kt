package app.getarcane.sdk.models.swarm

import app.getarcane.sdk.models.base.JsonValue
import kotlinx.serialization.Serializable

/** Body for `POST /environments/{id}/swarm/init`; the Docker `spec` blob is preserved as raw JSON. Mirrors Swift `SwarmInitRequest`. */
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

/** Response from `POST /environments/{id}/swarm/init`. Mirrors Swift `SwarmInitResponse`. */
@Serializable
public data class SwarmInitResponse(
    public val nodeId: String,
)

/** Body for `POST /environments/{id}/swarm/join`. Mirrors Swift `SwarmJoinRequest`. */
@Serializable
public data class SwarmJoinRequest(
    public val listenAddr: String? = null,
    public val advertiseAddr: String? = null,
    public val dataPathAddr: String? = null,
    public val remoteAddrs: List<String>,
    public val joinToken: String,
    public val availability: String? = null,
)

/** Body for `POST /environments/{id}/swarm/leave`. Mirrors Swift `SwarmLeaveRequest`. */
@Serializable
public data class SwarmLeaveRequest(
    public val force: Boolean? = null,
)

/** Body for `POST /environments/{id}/swarm/unlock`. Mirrors Swift `SwarmUnlockRequest`. */
@Serializable
public data class SwarmUnlockRequest(
    public val key: String,
)

/** Response from `GET /environments/{id}/swarm/unlock-key`. Mirrors Swift `SwarmUnlockKeyResponse`. */
@Serializable
public data class SwarmUnlockKeyResponse(
    public val unlockKey: String,
)

/** Worker / manager join tokens for the swarm. Mirrors Swift `SwarmJoinTokens`. */
@Serializable
public data class SwarmJoinTokens(
    public val worker: String,
    public val manager: String,
)

/** Body for `POST /environments/{id}/swarm/join-tokens/rotate`. Mirrors Swift `SwarmRotateJoinTokensRequest`. */
@Serializable
public data class SwarmRotateJoinTokensRequest(
    public val rotateWorkerToken: Boolean? = null,
    public val rotateManagerToken: Boolean? = null,
)

/** Body for `PUT /environments/{id}/swarm/spec`. Mirrors Swift `SwarmUpdateRequest`. */
@Serializable
public data class SwarmUpdateRequest(
    public val version: ULong? = null,
    public val spec: JsonValue,
    public val rotateWorkerToken: Boolean? = null,
    public val rotateManagerToken: Boolean? = null,
    public val rotateManagerUnlockKey: Boolean? = null,
)
