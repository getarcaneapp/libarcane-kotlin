package app.getarcane.sdk.http

import kotlinx.serialization.Serializable

/**
 * Marker for endpoints that take no request body. In practice the transport sends no body for
 * void/no-body requests; this exists for API symmetry.
 */
@Serializable
public data object EmptyBody
