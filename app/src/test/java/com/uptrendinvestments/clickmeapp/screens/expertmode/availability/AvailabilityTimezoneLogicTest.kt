package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.constants.Timezones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvailabilityTimezoneLogicTest {
    private val logic = AvailabilityTimezoneLogic
    private val toronto = TimezoneEntry("America/Toronto", "(GMT-05:00) Toronto")
    private val utcEntry = TimezoneEntry("UTC", "(GMT+00:00) UTC")

    @Test
    fun isUtc_recognisesUtcSpellings() {
        listOf("UTC", "utc", "Etc/UTC", "etc/utc").forEach { assertTrue(it, logic.isUtc(it)) }
        listOf("Europe/London", "UTC+01:00").forEach { assertFalse(it, logic.isUtc(it)) }
        assertFalse(logic.isUtc(null))
    }

    @Test
    fun autoFillZone_onlyForUtcServerAndNonUtcDevice() {
        assertEquals("America/Toronto", logic.autoFillZone("UTC", toronto))
        assertEquals("America/Toronto", logic.autoFillZone("etc/utc", toronto))
        assertNull(logic.autoFillZone("Etc/UTC", utcEntry))
        assertNull(logic.autoFillZone("America/Chicago", toronto))
    }

    @Test
    fun label_bundledWins() {
        assertEquals("(GMT-05:00) Eastern Time", logic.label("America/New_York", "America/New_York", "server"))
    }

    @Test
    fun label_serverDisplayNameForUnbundledServerZone() {
        assertEquals(
            "(GMT-05:00) America/Toronto",
            logic.label("America/Toronto", "America/Toronto", "(GMT-05:00) America/Toronto"),
        )
    }

    @Test
    fun label_runtimeLabelWhenNotServerZone() {
        val l = logic.label("America/Toronto", "UTC", "(GMT+00:00) UTC")
        assertTrue(l, l.startsWith("(GMT"))
        assertTrue(l, l.endsWith("Toronto"))
    }

    @Test
    fun label_blankDisplayNameFallsBackToRuntime() {
        val l = logic.label("America/Toronto", "America/Toronto", "  ")
        assertTrue(l, l.startsWith("(GMT"))
        assertTrue(l, l.endsWith("Toronto"))
    }

    @Test
    fun pickerEntries_unbundledSelectedFirst() {
        val e = logic.pickerEntries("America/Toronto", "America/Toronto", "(GMT-05:00) America/Toronto")
        assertEquals(TimezoneEntry("America/Toronto", "(GMT-05:00) America/Toronto"), e.first())
        assertEquals(Timezones.all, e.drop(1))
    }

    @Test
    fun pickerEntries_bundledSelectedIsExactlyAll() {
        assertEquals(Timezones.all, logic.pickerEntries("UTC", "UTC", "(GMT+00:00) UTC"))
    }

    @Test
    fun pickerEntries_serverZoneListedAfterSelected() {
        val e = logic.pickerEntries("America/Toronto", "Etc/UTC", "(GMT+00:00) Etc/UTC")
        assertEquals("America/Toronto", e[0].id)
        assertEquals("Etc/UTC", e[1].id)
        assertEquals("(GMT+00:00) Etc/UTC", e[1].label)
        assertEquals(Timezones.all, e.drop(2))
        assertEquals(e.size, e.map { it.id }.distinct().size)
    }
}
