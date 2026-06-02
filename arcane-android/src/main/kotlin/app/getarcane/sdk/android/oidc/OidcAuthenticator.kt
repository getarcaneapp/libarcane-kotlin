package app.getarcane.sdk.android.oidc

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import app.getarcane.sdk.ArcaneClient
import app.getarcane.sdk.models.auth.OidcCallbackResponse
import app.getarcane.sdk.models.auth.OidcDeviceAuthResponse
import app.getarcane.sdk.models.auth.OidcDeviceTokenResponse

/**
 * Drives the OIDC browser sign-in flow on Android using Custom Tabs + an app-handled redirect:
 *
 * 1. [startSignIn] fetches the provider authorization URL and opens it in a Custom Tab.
 * 2. The provider redirects to your app's deep link (`redirectUri`); your Activity receives the [Uri].
 * 3. [completeSignIn] exchanges the `code`/`state` with the backend and the [ArcaneClient] persists
 *    the resulting tokens.
 *
 * For headless/TV-style sign-in, use the device-code flow ([beginDeviceFlow] + [pollDeviceToken]),
 * which needs no redirect handling.
 */
public class OidcAuthenticator(private val client: ArcaneClient) {
    public class SignInError(message: String) : Exception(message)

    /** Fetches the provider authorization URL for [redirectUri] (a deep link your app handles). */
    public suspend fun authorizationUrl(redirectUri: String): String =
        client.auth.oidcAuthUrl(mobileRedirectUri = redirectUri).authUrl

    /** Opens [authorizationUrl] in a Custom Tab (the system browser). */
    public fun launchInCustomTab(context: Context, authorizationUrl: String) {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorizationUrl))
    }

    /** Convenience: fetch the auth URL and open it in a Custom Tab. Returns [redirectUri] to listen for. */
    public suspend fun startSignIn(context: Context, redirectUri: String): String {
        launchInCustomTab(context, authorizationUrl(redirectUri))
        return redirectUri
    }

    /**
     * Completes sign-in from the [callbackUri] your Activity received on the redirect. Exchanges the
     * `code`/`state` with the backend; the [ArcaneClient] persists the returned tokens.
     */
    public suspend fun completeSignIn(callbackUri: Uri, redirectUri: String): OidcCallbackResponse {
        val code = callbackUri.getQueryParameter("code") ?: throw SignInError("Missing 'code' in callback URL")
        val state = callbackUri.getQueryParameter("state") ?: throw SignInError("Missing 'state' in callback URL")
        return client.auth.oidcCallback(code = code, state = state, mobileRedirectUri = redirectUri)
    }

    // --- Device-code flow (no redirect handling required) ---

    /** Begins the device authorization flow. Show the returned user code + verification URL. */
    public suspend fun beginDeviceFlow(): OidcDeviceAuthResponse = client.auth.oidcDeviceCode()

    /** Polls for the device token once the user has authorized. Persists tokens on success. */
    public suspend fun pollDeviceToken(deviceCode: String): OidcDeviceTokenResponse =
        client.auth.oidcDeviceToken(deviceCode)
}
