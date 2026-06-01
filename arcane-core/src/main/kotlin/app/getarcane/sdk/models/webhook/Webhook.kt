package app.getarcane.sdk.models.webhook

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Summary returned in list responses; the token is masked. Mirrors Swift `Webhook`
 * (Models/Webhook/Webhook.swift).
 */
@Serializable
public data class Webhook(
    public val id: String,
    public val name: String,
    public val tokenPrefix: String,
    public val targetType: String,
    public val actionType: String,
    public val targetId: String,
    public val targetName: String? = null,
    public val environmentId: String,
    public val enabled: Boolean,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val lastTriggeredAt: Instant? = null,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
)

/** Returned once when a webhook is first created, including the raw token. Mirrors Swift `WebhookCreated`. */
@Serializable
public data class WebhookCreated(
    public val id: String,
    public val name: String,
    public val token: String,
    public val targetType: String,
    public val actionType: String,
    public val targetId: String,
    @Serializable(with = ArcaneInstantSerializer::class)
    public val createdAt: Instant,
)

/** Mirrors Swift `CreateWebhook`. */
@Serializable
public data class CreateWebhook(
    public val name: String,
    public val targetType: String,
    public val actionType: String,
    public val targetId: String,
)

/** Mirrors Swift `UpdateWebhook`. */
@Serializable
public data class UpdateWebhook(
    public val enabled: Boolean,
)
