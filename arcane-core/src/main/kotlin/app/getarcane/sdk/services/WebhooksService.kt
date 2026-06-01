package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.models.base.MessageResponse
import app.getarcane.sdk.models.webhook.CreateWebhook
import app.getarcane.sdk.models.webhook.UpdateWebhook
import app.getarcane.sdk.models.webhook.Webhook
import app.getarcane.sdk.models.webhook.WebhookCreated

/** Manages webhook resources scoped to an environment. Port of Swift `WebhooksService`. */
public class WebhooksService internal constructor(private val rest: RestService) {
    /** List all webhooks configured for an environment. Tokens are masked. */
    public suspend fun list(envId: EnvironmentId? = null): List<Webhook> =
        rest.get(rest.environmentPath(envId, "webhooks"))

    /** Create a new webhook. The raw token is returned only on creation. */
    public suspend fun create(body: CreateWebhook, envId: EnvironmentId? = null): WebhookCreated =
        rest.post(rest.environmentPath(envId, "webhooks"), body = body)

    /** Update a webhook's enabled state. */
    public suspend fun update(id: String, body: UpdateWebhook, envId: EnvironmentId? = null) {
        rest.patch<MessageResponse>(rest.environmentPath(envId, "webhooks/$id"), body = body)
    }

    /** Delete a webhook. */
    public suspend fun delete(id: String, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "webhooks/$id"))
    }
}
