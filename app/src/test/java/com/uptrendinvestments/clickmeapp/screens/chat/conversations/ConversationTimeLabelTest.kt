package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ConversationTimeLabelTest {
    private val now = Instant.parse("2026-10-27T15:00:00Z")

    private fun label(
        at: Instant?,
        zone: ZoneId = ZoneOffset.UTC,
        locale: Locale = Locale.US,
    ) = relativeTimeLabel(at, now, zone, locale, SimpleDateFormat("h:mm a", Locale.US), "Yesterday")

    @Test
    fun nullIsEmpty() {
        assertEquals("", label(null))
    }

    @Test
    fun sameDayShowsTheDeviceTime() {
        assertEquals("9:30 AM", label(Instant.parse("2026-10-27T09:30:00Z")))
    }

    @Test
    fun theDayIsJudgedInTheGivenZone() {
        // 02:00Z is still Oct 26 in Toronto (EDT), where "now" is Oct 27 11:00.
        val toronto = ZoneId.of("America/Toronto")
        assertEquals("Yesterday", label(Instant.parse("2026-10-27T02:00:00Z"), zone = toronto))
        assertEquals("9:30 AM", label(Instant.parse("2026-10-27T13:30:00Z"), zone = toronto))
    }

    @Test
    fun previousDayShowsYesterday() {
        assertEquals("Yesterday", label(Instant.parse("2026-10-26T23:59:00Z")))
    }

    @Test
    fun olderShowsMonthAndDay() {
        assertEquals("Oct 24", label(Instant.parse("2026-10-24T12:00:00Z")))
    }

    @Test
    fun monthAndDayFollowTheLocale() {
        val french = label(Instant.parse("2026-10-24T12:00:00Z"), locale = Locale.CANADA_FRENCH)
        assertTrue(french, french.contains("24") && french.contains("oct", ignoreCase = true))
    }

    @Test
    fun theTimeFormatIsNotMutated() {
        val format = SimpleDateFormat("h:mm a", Locale.US)
        val before = format.timeZone
        val at = Instant.parse("2026-10-27T09:30:00Z")
        relativeTimeLabel(at, now, ZoneId.of("Asia/Tokyo"), Locale.US, format, "Yesterday")
        assertEquals(before, format.timeZone)
    }
}
