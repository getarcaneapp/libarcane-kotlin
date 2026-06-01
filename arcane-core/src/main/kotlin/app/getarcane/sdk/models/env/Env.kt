package app.getarcane.sdk.models.env

import kotlinx.serialization.Serializable

/** A generic `KEY=value` template variable. Mirrors Swift `EnvVariable` (Models/Env/Env.swift). */
@Serializable
public data class EnvVariable(
    public val key: String,
    public val value: String,
)

/**
 * Wraps a collection of variables for endpoints that accept a `variables` array. Mirrors Swift
 * `EnvVariables`.
 */
@Serializable
public data class EnvVariables(
    public val variables: List<EnvVariable> = emptyList(),
)
