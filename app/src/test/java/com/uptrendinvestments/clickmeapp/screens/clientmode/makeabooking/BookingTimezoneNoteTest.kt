package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class BookingTimezoneNoteTest {
    private val toronto = ZoneId.of("America/Toronto")

    @Test
    fun differentZonesInDaylightTime() {
        assertEquals(
            TimezoneNote.Different("EDT", "Los Angeles", "PDT"),
            timezoneNote(toronto, "America/Los_Angeles", LocalDate.of(2026, 10, 22), Locale.US),
        )
    }

    @Test
    fun differentZonesInStandardTime() {
        assertEquals(
            TimezoneNote.Different("EST", "Los Angeles", "PST"),
            timezoneNote(toronto, "America/Los_Angeles", LocalDate.of(2026, 11, 10), Locale.US),
        )
    }

    @Test
    fun sameZoneGivesSingleZoneNote() {
        assertEquals(
            TimezoneNote.Same("EDT"),
            timezoneNote(toronto, "America/Toronto", LocalDate.of(2026, 10, 22), Locale.US),
        )
    }

    @Test
    fun aliasOfTheClientZoneGivesSingleZoneNote() {
        val day = LocalDate.of(2026, 10, 22)
        assertTrue(timezoneNote(ZoneId.of("Asia/Kolkata"), "Asia/Calcutta", day, Locale.US) is TimezoneNote.Same)
        assertEquals(
            TimezoneNote.Same("EDT"),
            timezoneNote(ZoneId.of("America/New_York"), "US/Eastern", day, Locale.US),
        )
        assertTrue(timezoneNote(ZoneId.of("UTC"), "Etc/UTC", day, Locale.US) is TimezoneNote.Same)
    }

    @Test
    fun zonesSharingOnlyTodaysOffsetStayDifferent() {
        val note = timezoneNote(toronto, "America/New_York", LocalDate.of(2026, 10, 22), Locale.US)
        assertEquals(TimezoneNote.Different("EDT", "New York", "EDT"), note)
    }

    @Test
    fun missingOrInvalidExpertZoneFallsBackToSingleZoneNote() {
        val day = LocalDate.of(2026, 10, 22)
        val expected = TimezoneNote.Same("EDT")
        assertEquals(expected, timezoneNote(toronto, null, day, Locale.US))
        assertEquals(expected, timezoneNote(toronto, "", day, Locale.US))
        assertEquals(expected, timezoneNote(toronto, "Not/A_Zone", day, Locale.US))
    }

    @Test
    fun abbreviationFollowsSelectedDate() {
        val newYork = ZoneId.of("America/New_York")
        assertEquals("EDT", zoneAbbreviation(newYork, LocalDate.of(2026, 10, 31), Locale.US))
        assertEquals("EST", zoneAbbreviation(newYork, LocalDate.of(2026, 11, 1), Locale.US))
    }

    @Test
    fun zoneCityUsesLastSegmentWithSpaces() {
        assertEquals("Toronto", zoneCity("America/Toronto"))
        assertEquals("Buenos Aires", zoneCity("America/Argentina/Buenos_Aires"))
        assertEquals("UTC", zoneCity("UTC"))
    }

    @Test
    fun rawIanaIdIsNeverShown() {
        val note = timezoneNote(toronto, "Asia/Kolkata", LocalDate.of(2026, 10, 22), Locale.US)
        assertTrue(note is TimezoneNote.Different)
        note as TimezoneNote.Different
        assertEquals("Kolkata", note.expertCity)
        assertNotEquals("Asia/Kolkata", note.expertAbbr)
        assertNotEquals("Asia/Kolkata", note.clientAbbr)
    }
}
