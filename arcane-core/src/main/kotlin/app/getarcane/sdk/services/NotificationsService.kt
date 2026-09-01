package app.getarcane.sdk.services

import app.getarcane.sdk.EnvironmentId
import app.getarcane.sdk.http.RestService
import app.getarcane.sdk.http.requestDecoded
import app.getarcane.sdk.models.notification.AppriseSettings
import app.getarcane.sdk.models.notification.NotificationDispatchRequest
import app.getarcane.sdk.models.notification.NotificationProvider
import app.getarcane.sdk.models.notification.NotificationSettings
import app.getarcane.sdk.models.notification.NotificationTestType
import app.getarcane.sdk.models.notification.UpdateAppriseSettings
import app.getarcane.sdk.models.notification.UpdateNotificationSettings

/**
 * Manages notification provider settings, Apprise integration, and dispatch.
 */
public class NotificationsService internal constructor(private val rest: RestService) {
    // Provider settings

    /** Get all notification settings for an environment. The endpoint returns the list directly. */
    public suspend fun listSettings(envId: EnvironmentId? = null): List<NotificationSettings> =
        rest.transport.requestDecoded(rest.environmentPath(envId, "notifications/settings"))

    /** Get notification settings for a specific provider. The endpoint returns the object directly. */
    public suspend fun getSettings(
        provider: NotificationProvider,
        envId: EnvironmentId? = null,
    ): NotificationSettings =
        rest.transport.requestDecoded(rest.environmentPath(envId, "notifications/settings/${provider.wire}"))

    /** Create or update notification settings. The endpoint returns the object directly. */
    public suspend fun upsertSettings(
        body: UpdateNotificationSettings,
        envId: EnvironmentId? = null,
    ): NotificationSettings =
        rest.transport.requestDecoded(
            rest.environmentPath(envId, "notifications/settings"),
            method = "POST",
            body = body,
        )

    /** Delete notification settings for a provider. */
    public suspend fun deleteSettings(provider: NotificationProvider, envId: EnvironmentId? = null) {
        rest.deleteVoid(rest.environmentPath(envId, "notifications/settings/${provider.wire}"))
    }

    /** Send a test notification through the configured provider. */
    public suspend fun test(
        provider: NotificationProvider,
        type: NotificationTestType = NotificationTestType.SIMPLE,
        envId: EnvironmentId? = null,
    ) {
        val path = rest.environmentPath(envId, "notifications/test/${provider.wire}")
        rest.postVoid(path, query = listOf("type" to type.wire))
    }

    // Apprise

    /** Get the Apprise integration settings. */
    public suspend fun getAppriseSettings(envId: EnvironmentId? = null): AppriseSettings =
        rest.get(rest.environmentPath(envId, "notifications/apprise"))

    /** Create or update Apprise integration settings. */
    public suspend fun upsertAppriseSettings(
        body: UpdateAppriseSettings,
        envId: EnvironmentId? = null,
    ): AppriseSettings =
        rest.post(rest.environmentPath(envId, "notifications/apprise"), body = body)

    /** Send a test notification through Apprise. */
    public suspend fun testApprise(
        type: NotificationTestType = NotificationTestType.SIMPLE,
        envId: EnvironmentId? = null,
    ) {
        val path = rest.environmentPath(envId, "notifications/apprise/test")
        rest.postVoid(path, query = listOf("type" to type.wire))
    }

    // Dispatch (manager-facing endpoint)

    /** Dispatch a notification from a remote agent to the manager. */
    public suspend fun dispatch(body: NotificationDispatchRequest) {
        rest.postVoid("notifications/dispatch", body = body)
    }
}
