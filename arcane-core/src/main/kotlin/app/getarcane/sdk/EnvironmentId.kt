package app.getarcane.sdk

import kotlinx.serialization.Serializable

/**
 * Identifier of an Arcane environment, serialized as a bare string. Mirrors Swift `EnvironmentID`
 * (Client/Environment.swift). `"0"` ([LOCAL_DOCKER]) is the default local Docker environment.
 */
@JvmInline
@Serializable
public value class EnvironmentId(public val rawValue: String) {
    override fun toString(): String = rawValue

    public companion object {
        public val LOCAL_DOCKER: EnvironmentId = EnvironmentId("0")
    }
}
