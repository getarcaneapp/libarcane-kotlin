package app.getarcane.sdk.models.base

import kotlinx.serialization.Serializable

/** A simple response carrying a human-readable message. */
@Serializable
public data class MessageResponse(
    public val message: String,
)

/** A simple response carrying an error string. */
@Serializable
public data class ErrorResponse(
    public val error: String,
)

/** RFC-7807-style error body. */
@Serializable
public data class ErrorModel(
    public val title: String? = null,
    public val status: Int? = null,
    public val detail: String? = null,
    public val instance: String? = null,
    public val type: String? = null,
    public val errors: List<ErrorDetail>? = null,
)

/** A single field-level error detail within an [ErrorModel]. */
@Serializable
public data class ErrorDetail(
    public val message: String? = null,
    public val location: String? = null,
    public val value: JsonValue? = null,
)
