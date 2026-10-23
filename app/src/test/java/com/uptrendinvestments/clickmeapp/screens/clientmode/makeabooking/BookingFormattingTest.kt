package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class BookingFormattingTest {
    private val toronto = ZoneId.of("America/Toronto")

    @Test
    fun capNotesLeavesShortTextAlone() {
        assertEquals("abc", capNotes("abc"))
        assertEquals("a".repeat(500), capNotes("a".repeat(500)))
    }

    @Test
    fun capNotesCutsTo500() {
        assertEquals(500, capNotes("a".repeat(501)).length)
    }

    @Test
    fun capNotesDropsSplitSurrogate() {
        val capped = capNotes("a".repeat(499) + "😀")
        assertEquals(499, capped.length)
        assertFalse(Character.isHighSurrogate(capped.last()))
    }

    @Test
    fun formatSlotTimeUses12HourFormatInZone() {
        val format = SimpleDateFormat("h:mm a", Locale.US)
        assertEquals("2:00 PM", formatSlotTime(Instant.parse("2026-10-24T18:00:00Z"), toronto, format))
    }

    @Test
    fun formatSlotTimeUses24HourFormatInZone() {
        val format = SimpleDateFormat("HH:mm", Locale.US)
        assertEquals("14:00", formatSlotTime(Instant.parse("2026-10-24T18:00:00Z"), toronto, format))
    }

    @Test
    fun formatSlotTimeDoesNotMutateTheFormat() {
        val format = SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        formatSlotTime(Instant.parse("2026-10-24T18:00:00Z"), toronto, format)
        assertEquals(TimeZone.getTimeZone("UTC"), format.timeZone)
    }

    @Test
    fun formatSlotTimeCrossesToPreviousLocalEvening() {
        val format = SimpleDateFormat("h:mm a", Locale.US)
        assertEquals("11:30 PM", formatSlotTime(Instant.parse("2026-11-01T03:30:00Z"), toronto, format))
    }

    @Test
    fun formatSessionWhenUses12HourFormat() {
        val format = SimpleDateFormat("h:mm a", Locale.US)
        assertEquals(
            "Oct 24, 2:00 PM",
            formatSessionWhen(Instant.parse("2026-10-24T18:00:00Z"), toronto, Locale.US, format),
        )
    }

    @Test
    fun formatSessionWhenUses24HourFormat() {
        val format = SimpleDateFormat("HH:mm", Locale.US)
        assertEquals(
            "Oct 24, 14:00",
            formatSessionWhen(Instant.parse("2026-10-24T18:00:00Z"), toronto, Locale.US, format),
        )
    }

    @Test
    fun formatSessionWhenTakesTheDateInTheViewersZone() {
        val format = SimpleDateFormat("h:mm a", Locale.US)
        assertEquals(
            "Oct 22, 10:30 PM",
            formatSessionWhen(Instant.parse("2026-10-23T02:30:00Z"), toronto, Locale.US, format),
        )
    }

    @Test
    fun formatSlotDateLine() {
        assertEquals("Thursday, October 22", formatSlotDateLine(LocalDate.of(2026, 10, 22), Locale.US))
    }

    @Test
    fun formatShortDate() {
        assertEquals("Oct 24, 2026", formatShortDate(LocalDate.of(2026, 10, 24), Locale.US))
    }

    @Test
    fun formatMonthYear() {
        assertEquals("October 2026", formatMonthYear(YearMonth.of(2026, 10), Locale.US))
    }

    @Test
    fun formatDayLabel() {
        assertEquals("Thursday, October 22, 2026", formatDayLabel(LocalDate.of(2026, 10, 22), Locale.US))
    }
}
