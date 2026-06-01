package app.getarcane.sdk

import app.getarcane.sdk.serialization.ArcaneInstantSerializer
import app.getarcane.sdk.serialization.ArcaneJson
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class DateSerializationTest {
    @Serializable
    private data class Holder(
        @Serializable(with = ArcaneInstantSerializer::class) val at: Instant,
    )

    private val json = ArcaneJson.default

    @Test
    fun parsesWithFractionalSeconds() {
        val h = json.decodeFromString<Holder>("""{"at":"2024-05-31T12:00:00.123Z"}""")
        assertEquals(Instant.parse("2024-05-31T12:00:00.123Z"), h.at)
    }

    @Test
    fun parsesWithoutFractionalSeconds() {
        val h = json.decodeFromString<Holder>("""{"at":"2024-05-31T12:00:00Z"}""")
        assertEquals(Instant.parse("2024-05-31T12:00:00Z"), h.at)
    }

    @Test
    fun roundTrips() {
        val original = Holder(Instant.parse("2024-05-31T12:00:00Z"))
        val back = json.decodeFromString<Holder>(json.encodeToString(original))
        assertEquals(original, back)
    }
}
