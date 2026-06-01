package app.getarcane.sdk.models.event

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * An audit/activity event. Mirrors Swift `Event` (Models/Event/Event.swift). The Swift `CodingKeys`
 * are all identity (camelCase), so no [kotlinx.serialization.SerialName] overrides are needed.
 */
@Serializable
public data class Event(
    public val id: String,
    public val type: String,
    public val severity: String,
    public val title: String,
    public val description: String? = null,
    public val resourceType: String? = null,
    public val resourceId: String? = null,
    public val resourceName: String? = null,
    public val userId: String? = null,
    public val username: String? = null,
    public val environmentId: String? = null,
    public val metadata: Map<String, JsonValue>? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val timestamp: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
)

/** Mirrors Swift `CreateEvent`. */
@Serializable
public data class CreateEvent(
    public val type: String,
    public val severity: String? = null,
    public val title: String,
    public val description: String? = null,
    public val resourceType: String? = null,
    public val resourceId: String? = null,
    public val resourceName: String? = null,
    public val userId: String? = null,
    public val username: String? = null,
    public val environmentId: String? = null,
    public val metadata: Map<String, JsonValue>? = null,
)
