package app.getarcane.sdk.android.passkey

import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class AndroidPasskeyCredentialManagerTest {
    @Test
    fun preservesArcaneWebAuthnOptionsForCredentialManager() {
        val options = buildJsonObject {
            put("rpId", "arcane.example")
            put("challenge", "sensitive-challenge")
        }

        assertEquals(
            """{"rpId":"arcane.example","challenge":"sensitive-challenge"}""",
            credentialManagerRequestJson(options),
        )
    }

    @Test
    fun mapsCreateProviderFailuresToStableTypedErrors() {
        val configuration = mapCreateCredentialException(
            CreateCredentialProviderConfigurationException("raw provider detail"),
        )
        assertIs<PasskeyProviderConfigurationException>(configuration)
        assertFalse(configuration.toString().contains("raw provider detail"))
        assertIs<PasskeyProviderUnsupportedException>(
            mapCreateCredentialException(CreateCredentialUnsupportedException()),
        )
        assertIs<PasskeyProviderInterruptedException>(
            mapCreateCredentialException(CreateCredentialInterruptedException()),
        )
    }

    @Test
    fun mapsGetProviderFailuresToStableTypedErrors() {
        val configuration = mapGetCredentialException(
            GetCredentialProviderConfigurationException("raw provider detail"),
        )
        assertIs<PasskeyProviderConfigurationException>(configuration)
        assertFalse(configuration.toString().contains("raw provider detail"))
        assertIs<PasskeyProviderUnsupportedException>(
            mapGetCredentialException(GetCredentialUnsupportedException()),
        )
        assertIs<PasskeyNotFoundException>(mapGetCredentialException(NoCredentialException()))
        assertIs<PasskeyProviderInterruptedException>(
            mapGetCredentialException(GetCredentialInterruptedException()),
        )
    }
}
