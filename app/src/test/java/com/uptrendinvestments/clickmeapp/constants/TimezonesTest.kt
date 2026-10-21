package com.uptrendinvestments.clickmeapp.constants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TimezonesTest {
    private val winter = Instant.parse("2026-01-15T12:00:00Z")
    private val summer = Instant.parse("2026-07-01T12:00:00Z")

    @Test
    fun all_isTheTenIosEntriesVerbatim() {
        assertEquals(10, Timezones.all.size)
        assertEquals(
            listOf("Pacific/Midway", "America/Anchorage", "America/Los_Angeles"),
            Timezones.all.take(3).map { it.id },
        )
        assertEquals(TimezoneEntry("Asia/Kolkata", "(GMT+05:30) Mumbai"), Timezones.all.last())
        assertEquals(TimezoneEntry("UTC", "(GMT+00:00) UTC"), Timezones.all[6])
    }

    @Test
    fun detected_bundledIdMatchesExactly() {
        val e = Timezones.detected(ZoneId.of("America/New_York"), summer)
        assertEquals(TimezoneEntry("America/New_York", "(GMT-05:00) Eastern Time"), e)
    }

    @Test
    fun detected_unbundledRegionIdGetsRuntimeLabel() {
        assertEquals(
            TimezoneEntry("America/Toronto", "(GMT-04:00) Toronto"),
            Timezones.detected(ZoneId.of("America/Toronto"), summer),
        )
    }

    @Test
    fun detected_nonRegionOffsetFallsBackToSameOffsetBundledZone() {
        assertEquals("Europe/Paris", Timezones.detected(ZoneOffset.ofHours(1), winter).id)
    }

    @Test
    fun detected_noMatchingOffsetFallsBackToUtc() {
        assertEquals("UTC", Timezones.detected(ZoneOffset.ofHoursMinutes(3, 45), winter).id)
    }

    @Test
    fun pickerEntries_bundledDeviceZoneAddsNothing() {
        assertEquals(Timezones.all, Timezones.pickerEntries(ZoneId.of("America/New_York"), summer))
    }

    @Test
    fun pickerEntries_unbundledDeviceZoneIsFirst() {
        val rows = Timezones.pickerEntries(ZoneId.of("America/Toronto"), summer)
        assertEquals(11, rows.size)
        assertEquals("America/Toronto", rows.first().id)
        assertEquals(Timezones.all, rows.drop(1))
    }

    @Test
    fun label_formatsOffsetAndCity() {
        assertEquals(
            "(GMT-03:00) Buenos Aires",
            Timezones.label(ZoneId.of("America/Argentina/Buenos_Aires"), winter),
        )
        assertEquals("(GMT+05:45) Kathmandu", Timezones.label(ZoneId.of("Asia/Kathmandu"), winter))
        assertEquals("(GMT+00:00) UTC", Timezones.label(ZoneId.of("UTC"), winter))
    }

    @Test
    fun entryFor_bundledRuntimeAndUnknown() {
        val london = Timezones.all.first { it.id == "Europe/London" }
        assertSame(london, Timezones.entryFor("Europe/London", summer))
        assertEquals("(GMT-04:00) Toronto", Timezones.entryFor("America/Toronto", summer).label)
        assertEquals(TimezoneEntry("Not/AZone", "Not/AZone"), Timezones.entryFor("Not/AZone", summer))
    }
}
