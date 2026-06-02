package app.getarcane.sdk.errors

import app.getarcane.sdk.models.base.JsonValue
import app.getarcane.sdk.serialization.ArcaneJson
import io.ktor.http.Headers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Typed errors surfaced by the SDK. Each case carries its own associated data.
 */
public sealed class ArcaneError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** HTTP 401. */
    public data object Unauthorized : ArcaneError("Unauthorized")

    /** HTTP 403. */
    public data object Forbidden : ArcaneError("Forbidden")

    /** HTTP 404. */
    public data object NotFound : ArcaneError("Not found")

    /** HTTP 409. */
    public data class Conflict(public val detail: String?) : ArcaneError(detail)

    /** HTTP 400 `VALIDATION_ERROR` or Huma 422 with field errors. Keyed by field name. */
    public data class Validation(public val fields: Map<String, List<String>>) :
        ArcaneError("Validation failed")

    /** HTTP 429. [retryAfter] is the `Retry-After` header in seconds, if present and numeric. */
    public data class RateLimited(public val retryAfter: Double?) : ArcaneError("Rate limited")

    /** HTTP 5xx and other server-shaped errors. */
    public data class Server(public val code: String, public val serverMessage: String) :
        ArcaneError(serverMessage)

    /** Network / IO failure. */
    public data class Transport(public val detail: String) : ArcaneError(detail)

    /** Response body could not be decoded into the expected type. */
    public data class Decoding(public val detail: String) : ArcaneError(detail)

    /** Unrecognized non-2xx response shape. */
    public data class Unknown(public val statusCode: Int, public val body: String) :
        ArcaneError("HTTP $statusCode")

    public companion object
}

// --- Error response DTOs (Arcane envelope + Huma/RFC-7807) ---

@Serializable
internal data class ApiErrorResponse(
    val success: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    val code: String? = null,
    val details: JsonValue? = null,
)

@Serializable
internal data class HumaErrorResponse(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val errors: List<HumaErrorDetail>? = null,
)

@Serializable
internal data class HumaErrorDetail(
    val message: String? = null,
    val location: String? = null,
    val value: JsonValue? = null,
)

/**
 * Maps an HTTP status + body + headers to an [ArcaneError]: prefer the Arcane envelope; fall back to
 * Huma RFC-7807 only when the Arcane envelope has no `error`/`message`/`code`.
 */
public fun ArcaneError.Companion.fromResponse(
    statusCode: Int,
    body: String,
    headers: Headers = Headers.Empty,
    json: Json = ArcaneJson.default,
): ArcaneError {
    val arcane = runCatching { json.decodeFromString<ApiErrorResponse>(body) }.getOrNull()
    val arcaneHasFields = arcane?.error != null || arcane?.message != null || arcane?.code != null
    val huma = if (arcaneHasFields) {
        null
    } else {
        runCatching { json.decodeFromString<HumaErrorResponse>(body) }.getOrNull()
    }
    val humaMessage = huma?.detail ?: huma?.title
    val message = arcane?.error ?: arcane?.message ?: humaMessage ?: body
    val code = arcane?.code ?: httpCode(statusCode)

    return when {
        statusCode == 401 -> ArcaneError.Unauthorized
        statusCode == 403 -> ArcaneError.Forbidden
        statusCode == 404 -> ArcaneError.NotFound
        statusCode == 409 -> ArcaneError.Conflict(message.ifEmpty { null })
        statusCode == 400 && code == "VALIDATION_ERROR" ->
            ArcaneError.Validation(validationFields(arcane?.details))
        statusCode == 422 -> {
            val errs = huma?.errors
            if (!errs.isNullOrEmpty()) {
                ArcaneError.Validation(validationFieldsFromHuma(errs))
            } else {
                ArcaneError.Server(code, humaMessage ?: message)
            }
        }
        statusCode == 429 -> ArcaneError.RateLimited(headers["Retry-After"]?.toDoubleOrNull())
        statusCode in 500..599 -> ArcaneError.Server(code, message)
        else -> when {
            arcane?.code != null -> ArcaneError.Server(code, message)
            !humaMessage.isNullOrEmpty() -> ArcaneError.Server(code, humaMessage)
            else -> ArcaneError.Unknown(statusCode, body)
        }
    }
}

private fun httpCode(statusCode: Int): String = when (statusCode) {
    400 -> "BAD_REQUEST"
    401 -> "UNAUTHORIZED"
    403 -> "FORBIDDEN"
    404 -> "NOT_FOUND"
    409 -> "CONFLICT"
    504 -> "TIMEOUT"
    else -> "HTTP_$statusCode"
}

private fun validationFields(details: JsonValue?): Map<String, List<String>> {
    val obj = (details as? JsonValue.Obj)?.value ?: return emptyMap()
    val fields = LinkedHashMap<String, List<String>>()
    for ((key, value) in obj) {
        when (value) {
            is JsonValue.Str -> fields[key] = listOf(value.value)
            is JsonValue.Arr -> fields[key] = value.value.mapNotNull { (it as? JsonValue.Str)?.value }
            else -> Unit
        }
    }
    return fields
}

private val LOCATION_PREFIXES = listOf("body.", "query.", "path.", "header.", "cookie.")

private fun validationFieldsFromHuma(errors: List<HumaErrorDetail>): Map<String, List<String>> {
    val fields = LinkedHashMap<String, MutableList<String>>()
    for (detail in errors) {
        val name = stripLocationPrefix(detail.location ?: "request")
        val message = detail.message ?: "Invalid value"
        fields.getOrPut(name) { mutableListOf() }.add(message)
    }
    return fields
}

private fun stripLocationPrefix(location: String): String =
    LOCATION_PREFIXES.firstOrNull { location.startsWith(it) }?.let { location.removePrefix(it) } ?: location
