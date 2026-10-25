package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

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
    fun addHours_clampsNearMidnight() {
        val a = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1350)))
        assertEquals(TimeSlot(1380, 1410), logic.addHours(a, DayOfWeek.MONDAY)[0].slots.last())
        val b = week(day(DayOfWeek.MONDAY, TimeSlot(540, 1410)))
        assertEquals(TimeSlot(1380, 1410), logic.addHours(b, DayOfWeek.MONDAY)[0].slots.last())
        val c = week(day(DayOfWeek.MONDAY, TimeSlot(1320, 1440)))
        assertEquals(TimeSlot(1380, 1410), logic.addHours(c, DayOfWeek.MONDAY)[0].slots.last())
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
}
