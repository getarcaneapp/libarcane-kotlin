package app.getarcane.sdk.models.base

import kotlinx.serialization.Serializable

/** Mirrors Swift `MessageResponse` (Models/Base/Message.swift). */
@Serializable
public data class MessageResponse(
    public val message: String,
)

/** Mirrors Swift `ErrorResponse`. */
@Serializable
public data class ErrorResponse(
    public val error: String,
)

/** RFC-7807-style error body. Mirrors Swift `ErrorModel`. */
@Serializable
public data class ErrorModel(
    public val title: String? = null,
    public val status: Int? = null,
    public val detail: String? = null,
    public val instance: String? = null,
    public val type: String? = null,
    public val errors: List<ErrorDetail>? = null,
)

/** Mirrors Swift `ErrorDetail`. */
@Serializable
public data class ErrorDetail(
    public val message: String? = null,
    public val location: String? = null,
    public val value: JsonValue? = null,
)
