package app.getarcane.sdk.models.apikey

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** An API key. Mirrors Swift `APIKey` (Models/APIKey/APIKey.swift). */
@Serializable
public data class APIKey(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val keyPrefix: String,
    public val userId: String? = null,
    public val isStatic: Boolean = false,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastUsedAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
)

/** An API key returned once at creation time, including the raw key. Mirrors Swift `APIKeyCreated`. */
@Serializable
public data class APIKeyCreated(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val keyPrefix: String,
    public val userId: String? = null,
    public val isStatic: Boolean = false,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastUsedAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val updatedAt: Instant? = null,
    public val key: String,
)

/** Mirrors Swift `CreateAPIKey`. */
@Serializable
public data class CreateAPIKey(
    public val name: String,
    public val description: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant? = null,
)

/** Mirrors Swift `UpdateAPIKey`. */
@Serializable
public data class UpdateAPIKey(
    public val name: String? = null,
    public val description: String? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val expiresAt: Instant? = null,
)
