package app.getarcane.sdk.serialization

import kotlinx.serialization.json.Json

/**
 * The single JSON codec for the SDK.
 *
 * - [Json.ignoreUnknownKeys] = true — tolerate extra server fields (e.g. Huma's `$schema`,
 *   `type`, `instance`).
 * - [Json.explicitNulls] = false — omit null properties on encode and treat missing keys as
 *   null on decode.
 * - [Json.encodeDefaults] = true — emit non-null fields that hold their default value.
 * - [Json.coerceInputValues] = true — fall back to a property's default when the wire value is
 *   null or an unknown enum constant.
 */
public object ArcaneJson {
    public val default: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }
}
