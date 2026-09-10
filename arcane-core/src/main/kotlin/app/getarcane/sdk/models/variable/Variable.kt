package app.getarcane.sdk.models.variable

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** A manager-owned variable materialized into all or selected environments. */
@Serializable
public data class GlobalVariable(
    public val id: String,
    public val key: String,
    /** Empty for secret variables returned by Arcane. */
    public val value: String = "",
    public val isSecret: Boolean = false,
    public val allEnvironments: Boolean = false,
    public val environmentIds: List<String> = emptyList(),
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
) {
    override fun toString(): String =
        "GlobalVariable(" +
            "id=$id, key=$key, value=${if (isSecret) "<redacted>" else value}, " +
            "isSecret=$isSecret, allEnvironments=$allEnvironments, " +
            "environmentIds=$environmentIds, createdAt=$createdAt, updatedAt=$updatedAt)"
}

/** Body for `POST /variables`. */
@Serializable
public data class CreateGlobalVariableRequest(
    public val key: String,
    public val value: String,
    public val isSecret: Boolean = false,
    public val allEnvironments: Boolean = false,
    public val environmentIds: List<String> = emptyList(),
) {
    /** Variable values are deliberately excluded from diagnostic string output. */
    override fun toString(): String =
        "CreateGlobalVariableRequest(" +
            "key=$key, value=<redacted>, isSecret=$isSecret, " +
            "allEnvironments=$allEnvironments, environmentIds=$environmentIds)"
}

/**
 * Partial body for `PUT /variables/{id}`. Null fields preserve the stored value. In particular,
 * [value] must remain null when editing a secret without replacing its write-only value; an empty
 * [environmentIds] list is distinct from null and explicitly clears selected-environment scope.
 */
@Serializable
public data class UpdateGlobalVariableRequest(
    public val key: String? = null,
    public val value: String? = null,
    public val isSecret: Boolean? = null,
    public val allEnvironments: Boolean? = null,
    public val environmentIds: List<String>? = null,
) {
    /** Variable values are deliberately excluded from diagnostic string output. */
    override fun toString(): String =
        "UpdateGlobalVariableRequest(" +
            "key=$key, value=${if (value == null) "null" else "<redacted>"}, " +
            "isSecret=$isSecret, allEnvironments=$allEnvironments, environmentIds=$environmentIds)"
}

/** Current materialization state for global variables in one environment. */
@Serializable(with = VariableSyncStateSerializer::class)
public enum class VariableSyncState(public val wire: String) {
    @SerialName("synced")
    SYNCED("synced"),

    @SerialName("pending")
    PENDING("pending"),

    @SerialName("error")
    ERROR("error"),

    /** A state introduced by a newer Arcane server. */
    UNKNOWN("unknown"),
}

internal object VariableSyncStateSerializer : KSerializer<VariableSyncState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("VariableSyncState", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): VariableSyncState {
        val wire = decoder.decodeString()
        return VariableSyncState.entries.firstOrNull {
            it != VariableSyncState.UNKNOWN && it.wire == wire
        } ?: VariableSyncState.UNKNOWN
    }

    override fun serialize(encoder: Encoder, value: VariableSyncState) {
        encoder.encodeString(value.wire)
    }
}

/** Last-known global-variable synchronization outcome for one environment. */
@Serializable
public data class EnvironmentSyncStatus(
    public val environmentId: String,
    public val environmentName: String? = null,
    public val status: VariableSyncState = VariableSyncState.UNKNOWN,
    public val error: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastSyncedAt: Instant? = null,
) {
    public val id: String get() = environmentId
}

/** Variable mutation result plus the background materialization outcomes it scheduled. */
@Serializable
public data class GlobalVariableMutationResponse(
    public val variable: GlobalVariable? = null,
    public val syncResults: List<EnvironmentSyncStatus> = emptyList(),
)
