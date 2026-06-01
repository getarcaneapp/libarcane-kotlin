package app.getarcane.sdk

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Port of Tests/ArcaneIntegrationTests/ArcaneIntegrationTests.swift. Hits a live backend's `/health`
 * endpoint only when `ARCANE_TEST_URL` is set; otherwise the test is skipped.
 */
class IntegrationTest {
    @Test
    fun backendHealthWhenConfigured() = runBlocking {
        val url = System.getenv("ARCANE_TEST_URL")
        Assumptions.assumeTrue(url != null && url.isNotBlank(), "Set ARCANE_TEST_URL to run integration tests")
        ArcaneClient(ArcaneConfiguration(baseUrl = url!!)).use { client ->
            val body = client.transport.rawRequestText("health", authorized = false)
            assertTrue(body.isNotEmpty(), "Expected a non-empty /health response")
        }
    }
}
