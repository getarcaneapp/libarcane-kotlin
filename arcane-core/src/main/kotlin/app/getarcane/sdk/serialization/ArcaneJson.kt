package app.getarcane.sdk.serialization

import kotlinx.serialization.json.Json

/**
 * The single JSON codec for the SDK, mirroring Swift's `ArcaneJSON` (ArcaneClient.swift).
 *
 * - [Json.ignoreUnknownKeys] = true — tolerate extra server fields (e.g. Huma's `$schema`,
 *   `type`, `instance`), matching Swift `JSONDecoder`'s default leniency.
 * - [Json.explicitNulls] = false — omit null properties on encode and treat missing keys as
 *   null on decode, matching Swift's `encodeIfPresent` / `decodeIfPresent`.
 * - [Json.encodeDefaults] = true — emit non-null fields that hold their default value, matching
 *   Swift structs that always encode their stored (non-optional) properties.
 * - [Json.coerceInputValues] = true — fall back to a property's default when the wire value is
 *   null or an unknown enum constant, matching Swift's `decodeIfPresent(...) ?? default`.
 */
public object ArcaneJson {
    public val default: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }
}
