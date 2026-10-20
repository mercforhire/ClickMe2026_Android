package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.JsonDataException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class InstantAdapterTest {
    private val adapter = MoshiProvider.moshi.adapter(Instant::class.java)

    private fun decode(raw: String): Instant = adapter.fromJson("\"$raw\"")!!

    @Test
    fun decodesMicrosecondFractionWithNumericOffset() {
        val instant = decode("2026-10-20T16:17:09.983286+00:00")
        assertEquals(983_286_000, instant.nano)
        assertEquals(Instant.parse("2026-10-20T16:17:09.983286Z"), instant)
    }

    @Test
    fun decodesMillisecondFractionWithZ() {
        assertEquals(983_000_000, decode("2026-10-20T16:17:09.983Z").nano)
    }

    @Test
    fun decodesNoFractionWithZ() {
        val instant = decode("2026-10-20T16:17:09Z")
        assertEquals(0, instant.nano)
        assertEquals(Instant.parse("2026-10-20T16:17:09Z"), instant)
    }

    @Test
    fun decodesFourDigitFraction() {
        assertEquals(983_200_000, decode("2026-10-20T16:17:09.9832Z").nano)
    }

    @Test
    fun decodesNineDigitFractionWithNegativeOffset() {
        val instant = decode("2026-10-20T16:17:09.123456789-04:00")
        assertEquals(123_456_789, instant.nano)
        assertEquals(Instant.parse("2026-10-20T20:17:09.123456789Z"), instant)
    }

    @Test
    fun rejectsSpaceSeparatedTimestamp() {
        assertThrows(Exception::class.java) { decode("2026-10-20 16:17:09.983286") }
    }

    @Test
    fun rejectsTimestampWithoutOffset() {
        assertThrows(Exception::class.java) { decode("2026-10-20T16:17:09") }
    }

    @Test
    fun encodeThenDecodeRoundTrips() {
        val original = Instant.parse("2026-10-20T16:17:09.983286Z")
        val json = adapter.toJson(original)
        assertEquals(original, adapter.fromJson(json))
    }

    @Test
    fun nonStringTokenFails() {
        assertThrows(JsonDataException::class.java) { adapter.fromJson("123") }
    }
}
