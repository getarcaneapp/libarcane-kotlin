package app.getarcane.sdk.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.getarcane.sdk.auth.TokenPair
import app.getarcane.sdk.auth.TokenStore
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.arcaneTokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "arcane_secure_tokens")

/**
 * Android [TokenStore] backed by an AES-256-GCM key in the AndroidKeyStore and ciphertext stored in
 * a Preferences DataStore. The OS-protected key never leaves the Keystore, and the serialized
 * [TokenPair] is encrypted at rest. The key has no user-authentication requirement so silent token
 * refresh works in the background.
 *
 * This is the recommended, non-deprecated secure store. See [EncryptedPrefsTokenStore] for the
 * Jetpack-Security fallback.
 */
public class AndroidSecureTokenStore @JvmOverloads constructor(
    context: Context,
    account: String = "default",
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val json: Json = ArcaneJson.default,
) : TokenStore {
    private val dataStore = context.applicationContext.arcaneTokenDataStore
    private val prefKey = stringPreferencesKey("tokens.$account")

    override suspend fun loadTokens(): TokenPair? = withContext(Dispatchers.IO) {
        val blob = dataStore.data.map { it[prefKey] }.first() ?: return@withContext null
        val plaintext = runCatching { decrypt(blob) }.getOrNull() ?: return@withContext null
        runCatching { json.decodeFromString(TokenPair.serializer(), plaintext) }.getOrNull()
    }

    override suspend fun saveTokens(tokens: TokenPair) {
        val blob = encrypt(json.encodeToString(TokenPair.serializer(), tokens))
        withContext(Dispatchers.IO) { dataStore.edit { it[prefKey] = blob } }
    }

    override suspend fun clearTokens() {
        withContext(Dispatchers.IO) { dataStore.edit { it.remove(prefKey) } }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val combined = Base64.decode(blob, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    public companion object {
        public const val DEFAULT_KEY_ALIAS: String = "app.getarcane.sdk.tokenkey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
    }
}
