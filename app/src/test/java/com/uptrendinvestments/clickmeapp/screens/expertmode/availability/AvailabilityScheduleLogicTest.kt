package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Locale
import java.util.TimeZone

class AvailabilityScheduleLogicTest {
    private val logic = AvailabilityScheduleLogic

    private fun day(
        d: DayOfWeek,
        vararg slots: TimeSlot,
        enabled: Boolean = true,
    ) = DaySchedule(d, enabled, slots.toList())

    private fun week(vararg overrides: DaySchedule): List<DaySchedule> =
        logic.WEEK.map { d ->
            overrides.firstOrNull { it.day == d } ?: DaySchedule(d, false, emptyList())
        }

    // --- defaults ---

    @Test
    fun defaultSchedule_isEveryDay1700To2100() {
        val s = logic.defaultSchedule()
        assertEquals(logic.WEEK, s.map { it.day })
        assertEquals(DayOfWeek.MONDAY, s.first().day)
        assertEquals(DayOfWeek.SUNDAY, s.last().day)
        s.forEach {
            assertTrue(it.enabled)
            assertEquals(listOf(TimeSlot(1020, 1260)), it.slots)
        }
    }

    // --- parse / format ---

    @Test
    fun parseHhmm_validValues() {
        assertEquals(540, logic.parseHhmm("09:00"))
        assertEquals(0, logic.parseHhmm("00:00"))
        assertEquals(1440, logic.parseHhmm("24:00"))
        assertEquals(1425, logic.parseHhmm("23:45"))
    }

    @Test
    fun parseHhmm_invalidValues() {
        listOf("9:00", "24:30", "25:00", "12:60", "ab:cd", "", " 09:00", "09:00:00").forEach {
            assertNull("'$it'", logic.parseHhmm(it))
        }
        assertNull(logic.parseHhmm(null))
    }

    @Test
    fun minutesToHhmm_formatsTwoDigits() {
        assertEquals("00:00", logic.minutesToHhmm(0))
        assertEquals("09:05", logic.minutesToHhmm(545))
        assertEquals("24:00", logic.minutesToHhmm(1440))
    }

    @Test
    fun minutesToHhmm_usesAsciiDigitsUnderArabicLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale("ar"))
            assertEquals("09:05", logic.minutesToHhmm(545))
            assertEquals("24:00", logic.minutesToHhmm(1440))
        } finally {
            Locale.setDefault(saved)
        }
    }

    // --- isLossless (WR-04) ---

    @Test
    fun isLossless_trueWhenEveryServerRangeHydrates() {
        assertTrue(logic.isLossless(emptyMap()))
        assertTrue(
            logic.isLossless(
                mapOf(
                    DayOfWeek.MONDAY to listOf("09:00" to "17:00"),
                    DayOfWeek.TUESDAY to listOf("22:00" to "24:00"),
                    DayOfWeek.WEDNESDAY to emptyList(),
                ),
            ),
        )
    }

    @Test
    fun isLossless_falseWhenAnyRangeIsMalformedOrInverted() {
        val bad =
            listOf(
                "9:30" to "10:00",
                "12:00" to "11:00",
                null to "10:00",
                "25:00" to "26:00",
            )
        for (range in bad) {
            assertFalse(
                "$range",
                logic.isLossless(
                    mapOf(
                        DayOfWeek.MONDAY to listOf("09:00" to "17:00"),
                        DayOfWeek.FRIDAY to listOf("08:00" to "09:00", range),
                    ),
                ),
            )
        }
    }

    // --- hydrate ---

    @Test
    fun hydrate_dropsMalformedAndInvertedSlots() {
        val result =
            logic.hydrate(
                mapOf(
                    DayOfWeek.MONDAY to
                        listOf(
                            "09:00" to "17:00",
                            "bad" to "10:00",
                            "12:00" to "11:00",
                            "13:00" to "13:00",
                            null to "10:00",
                        ),
                    DayOfWeek.TUESDAY to emptyList(),
                    DayOfWeek.WEDNESDAY to listOf("22:00" to "24:00"),
                ),
            )
        assertEquals(logic.WEEK, result.map { it.day })
        val monday = result[0]
        assertTrue(monday.enabled)
        assertEquals(listOf(TimeSlot(540, 1020)), monday.slots)
        assertFalse(result[1].enabled)
        assertTrue(result[1].slots.isEmpty())
        assertEquals(listOf(TimeSlot(1320, 1440)), result[2].slots)
        assertTrue(result[2].enabled)
        // absent days
        for (i in 3..6) {
            assertFalse(result[i].enabled)
            assertTrue(result[i].slots.isEmpty())
        }
    }

    @Test
    fun hydrate_dayWithOnlyInvalidSlotsIsDisabled() {
        val result = logic.hydrate(mapOf(DayOfWeek.FRIDAY to listOf("12:00" to "11:00")))
        assertFalse(result[4].enabled)
        assertTrue(result[4].slots.isEmpty())
    }

    // --- overlap ---

    @Test
    fun overlap_touchingSlotsAreValid() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 600), TimeSlot(600, 660)))
        assertTrue(logic.overlappingDays(s).isEmpty())
        assertFalse(logic.dayOverlaps(s[0]))
    }

    @Test
    fun overlap_overlappingSlotsFlagTheDay() {
        val s = week(day(DayOfWeek.TUESDAY, TimeSlot(540, 720), TimeSlot(660, 780)))
        assertEquals(listOf(DayOfWeek.TUESDAY), logic.overlappingDays(s))
        assertTrue(logic.dayOverlaps(s[1]))
    }

    @Test
    fun overlap_longSlotContainingTwoShortOnesIsOverlap() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1020), TimeSlot(600, 660), TimeSlot(720, 780)))
        assertEquals(listOf(DayOfWeek.MONDAY), logic.overlappingDays(s))
    }

    @Test
    fun overlap_unsortedInputIsSortedFirst() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(660, 780), TimeSlot(540, 720)))
        assertEquals(listOf(DayOfWeek.MONDAY), logic.overlappingDays(s))
    }

    @Test
    fun overlap_disabledDaysIgnoredAndOrderFollowsWeek() {
        val s =
            week(
                day(DayOfWeek.SUNDAY, TimeSlot(0, 100), TimeSlot(50, 150)),
                day(DayOfWeek.WEDNESDAY, TimeSlot(0, 100), TimeSlot(50, 150)),
                day(DayOfWeek.THURSDAY, TimeSlot(0, 100), TimeSlot(50, 150), enabled = false),
            )
        assertEquals(listOf(DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), logic.overlappingDays(s))
    }

    // --- edits ---

    @Test
    fun setEnabled_onSeedsWhenEmpty() {
        val s = week()
        val out = logic.setEnabled(s, DayOfWeek.MONDAY, true)
        assertTrue(out[0].enabled)
        assertEquals(listOf(TimeSlot(540, 1020)), out[0].slots)
        // other days untouched
        assertEquals(s.drop(1), out.drop(1))
    }

    @Test
    fun setEnabled_onKeepsExistingSlots() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(600, 660), enabled = false))
        val out = logic.setEnabled(s, DayOfWeek.MONDAY, true)
        assertTrue(out[0].enabled)
        assertEquals(listOf(TimeSlot(600, 660)), out[0].slots)
    }

    @Test
    fun setEnabled_offKeepsSlots() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(600, 660)))
        val out = logic.setEnabled(s, DayOfWeek.MONDAY, false)
        assertFalse(out[0].enabled)
        assertEquals(listOf(TimeSlot(600, 660)), out[0].slots)
    }

    @Test
    fun addHours_emptyDaySeeds() {
        val s = week(day(DayOfWeek.MONDAY))
        assertEquals(listOf(TimeSlot(540, 1020)), logic.addHours(s, DayOfWeek.MONDAY)[0].slots)
    }

    @Test
    fun addHours_appendsAfterLatestEnd() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1020)))
        assertEquals(
            listOf(TimeSlot(540, 1020), TimeSlot(1050, 1110)),
            logic.addHours(s, DayOfWeek.MONDAY)[0].slots,
        )
    }

    @Test
    fun addHours_emptyDayCanAdd() {
        assertTrue(logic.canAddHours(emptyList()))
    }

    @Test
    fun addHours_latestEnd2200_addsHalfPastTenToHalfPastEleven() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1320)))
        assertTrue(logic.canAddHours(s[0].slots))
        assertEquals(TimeSlot(1350, 1410), logic.addHours(s, DayOfWeek.MONDAY)[0].slots.last())
    }

    @Test
    fun addHours_latestEnd2230_hasNoRoomAndLeavesTheDayUnchanged() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1350)))
        assertFalse(logic.canAddHours(s[0].slots))
        assertEquals(s, logic.addHours(s, DayOfWeek.MONDAY))
    }

    @Test
    fun addHours_offGridLatestEnd2215_clampsTheEndTo2330() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1335)))
        assertTrue(logic.canAddHours(s[0].slots))
        assertEquals(TimeSlot(1365, 1410), logic.addHours(s, DayOfWeek.MONDAY)[0].slots.last())
    }

    @Test
    fun addHours_latestEndLaterThanTheLastSlotIsUsed() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1410), TimeSlot(600, 660)))
        assertFalse(logic.canAddHours(s[0].slots))
        assertEquals(s, logic.addHours(s, DayOfWeek.MONDAY))
    }

    @Test
    fun addHours_midnightEndHasNoRoom() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(1320, 1440)))
        assertFalse(logic.canAddHours(s[0].slots))
        assertEquals(s, logic.addHours(s, DayOfWeek.MONDAY))
    }

    @Test
    fun removeSlot_removesByIndexAndKeepsDayEnabled() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 600), TimeSlot(660, 720)))
        val one = logic.removeSlot(s, DayOfWeek.MONDAY, 0)
        assertEquals(listOf(TimeSlot(660, 720)), one[0].slots)
        val none = logic.removeSlot(one, DayOfWeek.MONDAY, 0)
        assertTrue(none[0].enabled)
        assertTrue(none[0].slots.isEmpty())
    }

    @Test
    fun replaceSlot_replacesByIndex() {
        val s = week(day(DayOfWeek.MONDAY, TimeSlot(540, 600), TimeSlot(660, 720)))
        val out = logic.replaceSlot(s, DayOfWeek.MONDAY, 1, TimeSlot(700, 800))
        assertEquals(listOf(TimeSlot(540, 600), TimeSlot(700, 800)), out[0].slots)
    }

    // --- wire ---

    @Test
    fun wireRanges_disabledDayIsEmpty() {
        assertTrue(logic.wireRanges(day(DayOfWeek.MONDAY, TimeSlot(540, 600), enabled = false)).isEmpty())
    }

    @Test
    fun wireRanges_sortedAndLossless() {
        val d = day(DayOfWeek.MONDAY, TimeSlot(780, 840), TimeSlot(540, 600), TimeSlot(1320, 1440))
        val wire = logic.wireRanges(d)
        assertEquals(listOf("09:00" to "10:00", "13:00" to "14:00", "22:00" to "24:00"), wire)
        val back = logic.hydrate(mapOf(DayOfWeek.MONDAY to wire))
        assertEquals(listOf(TimeSlot(540, 600), TimeSlot(780, 840), TimeSlot(1320, 1440)), back[0].slots)
    }

    // --- picker options ---

    @Test
    fun startOptions_gridStrictlyBeforeDraftEnd() {
        val o = logic.startOptions(540, 1020)
        assertEquals((0 until 1020 step 30).toList(), o)
        assertEquals(990, o.last())
    }

    @Test
    fun startOptions_includesOffGridDraftValueInOrder() {
        val o = logic.startOptions(555, 1020)
        assertTrue(555 in o)
        assertEquals(o.sorted(), o)
        assertEquals(o.distinct(), o)
        assertEquals(listOf(510, 540, 555, 570), o.filter { it in 510..570 })
    }

    @Test
    fun startOptions_neverExceedsLatestStartExceptInjectedDraft() {
        val o = logic.startOptions(1380, 1440)
        assertEquals((0..1380 step 30).toList(), o)
        assertEquals(1380, o.last())
    }

    @Test
    fun endOptions_gridStrictlyAfterDraftStart() {
        val o = logic.endOptions(540, 1020)
        assertEquals((570..1410 step 30).toList(), o)
        assertFalse(1440 in o)
    }

    @Test
    fun endOptions_midnightEndStaysSelectable() {
        val o = logic.endOptions(1320, 1440)
        assertEquals(1440, o.last())
        assertEquals(listOf(1350, 1380, 1410, 1440), o)
    }

    @Test
    fun endOptions_startsAfterDraftStart() {
        assertEquals(1410, logic.endOptions(1395, 1410).first())
    }

    // --- display format ---

    private fun withDefaultZone(
        id: String,
        block: () -> Unit,
    ) {
        val saved = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(saved)
        }
    }

    @Test
    fun formatMinutes_24HourIgnoresDeviceZoneAndDst() {
        withDefaultZone("America/New_York") {
            val fmt = SimpleDateFormat("HH:mm", Locale.US)
            assertEquals("09:00", logic.formatMinutes(540, fmt))
            // 02:30 does not exist on 2026-03-08 in New York; the UTC-pinned clone never shifts it.
            assertEquals("02:30", logic.formatMinutes(150, fmt))
            assertEquals("00:00", logic.formatMinutes(1440, fmt))
            assertEquals(TimeZone.getTimeZone("America/New_York"), fmt.timeZone)
        }
    }

    @Test
    fun formatMinutes_12Hour() {
        withDefaultZone("America/New_York") {
            val fmt = SimpleDateFormat("h:mm a", Locale.US)
            assertEquals("12:00 AM", logic.formatMinutes(1440, fmt))
            assertEquals("1:00 PM", logic.formatMinutes(780, fmt))
        }
    }

    // --- saved-state codec ---

    @Test
    fun encodeDecode_roundTrips() {
        val days =
            week(
                day(DayOfWeek.MONDAY, TimeSlot(540, 600), TimeSlot(1320, 1440)),
                day(DayOfWeek.TUESDAY, TimeSlot(600, 660), enabled = false),
                day(DayOfWeek.WEDNESDAY),
            )
        val tz = "America/Argentina/Buenos_Aires"
        assertEquals(days to tz, logic.decode(logic.encode(days, tz)))
    }

    @Test
    fun decode_rejectsMalformedInput() {
        assertNull(logic.decode(null))
        assertNull(logic.decode(""))
        assertNull(logic.decode("garbage"))
        val six = logic.encode(week(), "UTC").substringBeforeLast(";")
        assertNull(logic.decode(six))
    }

    @Test
    fun decode_rejectsOutOfRangeSlots() {
        val good = logic.encode(week(day(DayOfWeek.MONDAY, TimeSlot(540, 600))), "UTC")
        assertTrue(logic.decode(good) != null)
        assertNull(logic.decode(good.replace("540-600", "600-540")))
        assertNull(logic.decode(good.replace("540-600", "540-1441")))
        assertNull(logic.decode(good.replace("540-600", "-5-600")))
        assertNull(logic.decode(good.replace("540-600", "x-y")))
    }

    // --- joinNames ---

    @Test
    fun joinNames_englishListWithoutOxfordComma() {
        assertEquals("", logic.joinNames(emptyList(), ", ", " and "))
        assertEquals("Monday", logic.joinNames(listOf("Monday"), ", ", " and "))
        assertEquals("Monday and Tuesday", logic.joinNames(listOf("Monday", "Tuesday"), ", ", " and "))
        assertEquals(
            "Monday, Tuesday and Friday",
            logic.joinNames(listOf("Monday", "Tuesday", "Friday"), ", ", " and "),
        )
    }
}
