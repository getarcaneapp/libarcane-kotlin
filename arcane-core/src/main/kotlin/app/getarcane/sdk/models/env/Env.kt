package app.getarcane.sdk.models.env

import kotlinx.serialization.Serializable

/** A generic `KEY=value` template variable. */
@Serializable
public data class EnvVariable(
    public val key: String,
    public val value: String,
)

/**
 * Wraps a collection of variables for endpoints that accept a `variables` array.
 */
@Serializable
public data class EnvVariables(
    public val variables: List<EnvVariable> = emptyList(),
)
