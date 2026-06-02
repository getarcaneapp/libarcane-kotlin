package app.getarcane.sdk

import app.getarcane.sdk.errors.ArcaneError
import app.getarcane.sdk.errors.fromResponse
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Error mapping + EnvironmentID literal. */
class ErrorMappingTest {
    @Test
    fun environmentIdLiteral() {
        assertEquals("3", EnvironmentId("3").rawValue)
        assertEquals("0", EnvironmentId.LOCAL_DOCKER.rawValue)
    }

    @Test
    fun conflictMapping() {
        val err = ArcaneError.fromResponse(409, """{"code":"CONFLICT","message":"already exists"}""")
        assertEquals(ArcaneError.Conflict("already exists"), err)
    }

    @Test
    fun huma422ValidationMapping() {
        val json = """
            {
              "${'$'}schema": "https://example.com/schemas/ErrorModel.json",
              "title": "Unprocessable Entity",
              "status": 422,
              "detail": "validation failed",
              "errors": [
                { "message": "expected string", "location": "body.username", "value": null },
                { "message": "must be at least 8 characters", "location": "body.password", "value": null }
              ]
            }
        """.trimIndent()
        val err = assertIs<ArcaneError.Validation>(ArcaneError.fromResponse(422, json))
        assertEquals(listOf("expected string"), err.fields["username"])
        assertEquals(listOf("must be at least 8 characters"), err.fields["password"])
    }

    @Test
    fun huma422WithoutErrorsFallsBackToServer() {
        val json =
            """{ "${'$'}schema":"x", "title":"Unprocessable Entity", "status":422, "detail":"bad input" }"""
        val err = assertIs<ArcaneError.Server>(ArcaneError.fromResponse(422, json))
        assertEquals("bad input", err.serverMessage)
    }

    @Test
    fun rateLimitedReadsRetryAfter() {
        val err = ArcaneError.fromResponse(429, "{}", headersOf("Retry-After", "5"))
        assertEquals(ArcaneError.RateLimited(5.0), err)
    }
}
