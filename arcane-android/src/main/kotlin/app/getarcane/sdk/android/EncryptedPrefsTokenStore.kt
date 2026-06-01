package app.getarcane.sdk.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.getarcane.sdk.auth.TokenPair
import app.getarcane.sdk.auth.TokenStore
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * [TokenStore] backed by Jetpack Security's `EncryptedSharedPreferences`.
 *
 * Provided for teams already standardized on Jetpack Security. The `androidx.security:security-crypto`
 * library is deprecated, so new integrations should prefer [AndroidSecureTokenStore]. To migrate,
 * read once from this store and write to [AndroidSecureTokenStore].
 */
@Deprecated(
    message = "androidx.security:security-crypto is deprecated; prefer AndroidSecureTokenStore.",
    replaceWith = ReplaceWith("AndroidSecureTokenStore(context, account)"),
)
public class EncryptedPrefsTokenStore @JvmOverloads constructor(
    context: Context,
    account: String = "default",
    private val json: Json = ArcaneJson.default,
) : TokenStore {
    private val prefKey = "tokens.$account"
    private val prefs = run {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "arcane_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun loadTokens(): TokenPair? = withContext(Dispatchers.IO) {
        prefs.getString(prefKey, null)?.let {
            runCatching { json.decodeFromString(TokenPair.serializer(), it) }.getOrNull()
        }
    }

    override suspend fun saveTokens(tokens: TokenPair) {
        withContext(Dispatchers.IO) {
            prefs.edit().putString(prefKey, json.encodeToString(TokenPair.serializer(), tokens)).apply()
        }
    }

    override suspend fun clearTokens() {
        withContext(Dispatchers.IO) { prefs.edit().remove(prefKey).apply() }
    }
}
