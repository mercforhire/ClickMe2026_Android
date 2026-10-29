package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilitySlotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BookingSlotsTest {
    private val toronto = ZoneId.of("America/Toronto")
    private val utc = ZoneId.of("UTC")
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val kiritimati = ZoneId.of("Pacific/Kiritimati")

    private fun slot(
        start: String?,
        available: Boolean? = true,
        held: Boolean? = null,
    ) = AvailabilitySlotDto(
        startUtc = start?.let(Instant::parse),
        endUtc = start?.let { Instant.parse(it).plusSeconds(1800) },
        available = available,
        held = held,
    )

    private fun day(
        date: String,
        vararg slots: AvailabilitySlotDto,
    ) = AvailabilityDayDto(date = date, slots = slots.toList())

    private fun d(text: String) = LocalDate.parse(text)

    // Expert in Pacific/Auckland: 2026-11-01 16:30 NZDT is 2026-11-01T03:30:00Z, filed under server day 2026-11-01.
    private val aucklandDays = listOf(day("2026-11-01", slot("2026-11-01T03:30:00Z")))

    @Test
    fun auckland_slot_groups_under_previous_date_for_toronto() {
        assertEquals(setOf(d("2026-10-31")), groupSlots(aucklandDays, toronto).keys)
    }

    @Test
    fun auckland_slot_groups_under_same_date_for_utc() {
        assertEquals(setOf(d("2026-11-01")), groupSlots(aucklandDays, utc).keys)
    }

    @Test
    fun auckland_slot_groups_under_same_date_for_kolkata() {
        assertEquals(setOf(d("2026-11-01")), groupSlots(aucklandDays, kolkata).keys)
    }

    @Test
    fun auckland_slot_groups_under_same_date_for_kiritimati() {
        assertEquals(setOf(d("2026-11-01")), groupSlots(aucklandDays, kiritimati).keys)
    }

    @Test
    fun month_boundary_slot_stays_on_october_31_for_toronto_and_utc() {
        val days = listOf(day("2026-11-01", slot("2026-10-31T23:30:00Z")))
        assertEquals(setOf(d("2026-10-31")), groupSlots(days, toronto).keys)
        assertEquals(setOf(d("2026-10-31")), groupSlots(days, utc).keys)
    }

    @Test
    fun month_boundary_slot_moves_to_november_1_for_kiritimati() {
        val days = listOf(day("2026-10-31", slot("2026-10-31T23:30:00Z")))
        assertEquals(setOf(d("2026-11-01")), groupSlots(days, kiritimati).keys)
    }

    @Test
    fun us_fall_back_gives_two_distinct_slots_on_the_same_local_date() {
        val first = Instant.parse("2026-11-01T05:30:00Z")
        val second = Instant.parse("2026-11-01T06:30:00Z")
        // Out of order on purpose.
        val days = listOf(day("2026-11-01", slot("2026-11-01T06:30:00Z"), slot("2026-11-01T05:30:00Z")))
        val grouped = groupSlots(days, toronto)
        assertEquals(setOf(d("2026-11-01")), grouped.keys)
        assertEquals(listOf(first, second), grouped.getValue(d("2026-11-01")).map { it.start })
    }

    @Test
    fun duplicate_start_across_months_appears_once() {
        val october = day("2026-10-31", slot("2026-10-31T14:00:00Z"))
        val november = day("2026-10-31", slot("2026-10-31T14:00:00Z"))
        val grouped = groupSlots(listOf(october, november), utc)
        assertEquals(1, grouped.getValue(d("2026-10-31")).size)
    }

    @Test
    fun duplicate_with_mixed_availability_is_unavailable() {
        val a = day("2026-10-31", slot("2026-10-31T14:00:00Z", available = true))
        val b = day("2026-10-31", slot("2026-10-31T14:00:00Z", available = false))
        assertFalse(groupSlots(listOf(a, b), utc).getValue(d("2026-10-31")).single().isAvailable)
        assertFalse(groupSlots(listOf(b, a), utc).getValue(d("2026-10-31")).single().isAvailable)
    }

    @Test
    fun slots_are_sorted_within_each_day_even_when_input_is_shuffled_across_days() {
        val days =
            listOf(
                day("2026-10-15", slot("2026-10-15T16:00:00Z"), slot("2026-10-15T09:00:00Z")),
                day("2026-10-15", slot("2026-10-15T12:00:00Z")),
            )
        val starts = groupSlots(days, utc).getValue(d("2026-10-15")).map { it.start.toString() }
        assertEquals(listOf("2026-10-15T09:00:00Z", "2026-10-15T12:00:00Z", "2026-10-15T16:00:00Z"), starts)
    }

    @Test
    fun server_day_date_is_ignored() {
        val days = listOf(day("2026-12-25", slot("2026-10-15T14:00:00Z")))
        assertEquals(setOf(d("2026-10-15")), groupSlots(days, utc).keys)
    }

    @Test
    fun held_slot_is_unavailable() {
        val days = listOf(day("2026-10-15", slot("2026-10-15T14:00:00Z", available = true, held = true)))
        assertFalse(groupSlots(days, utc).getValue(d("2026-10-15")).single().isAvailable)
    }

    @Test
    fun null_flags_mean_available() {
        val days = listOf(day("2026-10-15", slot("2026-10-15T14:00:00Z", available = null, held = null)))
        assertTrue(groupSlots(days, utc).getValue(d("2026-10-15")).single().isAvailable)
    }

    @Test
    fun available_false_is_unavailable() {
        val days = listOf(day("2026-10-15", slot("2026-10-15T14:00:00Z", available = false)))
        assertFalse(groupSlots(days, utc).getValue(d("2026-10-15")).single().isAvailable)
    }

    @Test
    fun slot_without_start_is_dropped_and_null_slots_day_contributes_nothing() {
        val days =
            listOf(
                day("2026-10-15", slot(null)),
                AvailabilityDayDto(date = "2026-10-16", slots = null),
            )
        assertTrue(groupSlots(days, utc).isEmpty())
    }

    @Test
    fun end_instant_is_carried_through() {
        val days = listOf(day("2026-10-15", slot("2026-10-15T14:00:00Z")))
        val result = groupSlots(days, utc).getValue(d("2026-10-15")).single()
        assertEquals(Instant.parse("2026-10-15T14:30:00Z"), result.end)
    }

    private val now = Instant.parse("2026-10-15T12:00:00Z")

    private fun timeSlot(
        start: String,
        available: Boolean = true,
    ) = BookingTimeSlot(Instant.parse(start), null, available)

    @Test
    fun available_future_slot_is_selectable() {
        assertTrue(timeSlot("2026-10-15T12:00:01Z").isSelectable(now, null))
    }

    @Test
    fun slot_starting_exactly_now_or_earlier_is_not_selectable() {
        assertFalse(timeSlot("2026-10-15T12:00:00Z").isSelectable(now, null))
        assertFalse(timeSlot("2026-10-15T11:00:00Z").isSelectable(now, null))
    }

    @Test
    fun unavailable_future_slot_is_not_selectable() {
        assertFalse(timeSlot("2026-10-15T15:00:00Z", available = false).isSelectable(now, null))
    }

    @Test
    fun open_days_includes_only_days_with_a_selectable_slot() {
        val grouped =
            mapOf(
                d("2026-10-15") to listOf(timeSlot("2026-10-15T15:00:00Z")),
                d("2026-10-14") to listOf(timeSlot("2026-10-14T15:00:00Z")),
                d("2026-10-16") to listOf(timeSlot("2026-10-16T15:00:00Z", available = false)),
            )
        assertEquals(setOf(d("2026-10-15")), grouped.openDays(now, null))
    }

    // ---- quick 261028-to1: paid booking window ----

    private val latest = now.plus(PAID_BOOKING_MAX_LEAD)

    @Test
    fun paid_booking_max_lead_is_144_hours() {
        assertEquals(java.time.Duration.ofHours(144), PAID_BOOKING_MAX_LEAD)
    }

    @Test
    fun slot_exactly_six_days_out_is_selectable_with_a_window() {
        assertTrue(timeSlot("2026-10-21T12:00:00Z").isSelectable(now, latest))
    }

    @Test
    fun slot_six_days_and_a_minute_out_is_not_selectable_with_a_window_but_is_without() {
        val slot = timeSlot("2026-10-21T12:01:00Z")
        assertFalse(slot.isSelectable(now, latest))
        assertTrue(slot.isSelectable(now, null))
    }

    @Test
    fun open_days_drops_a_day_whose_only_slot_is_beyond_the_window() {
        val grouped =
            mapOf(
                d("2026-10-21") to listOf(timeSlot("2026-10-21T12:00:00Z")),
                d("2026-10-22") to listOf(timeSlot("2026-10-22T12:00:00Z")),
            )
        assertEquals(setOf(d("2026-10-21")), grouped.openDays(now, latest))
        assertEquals(setOf(d("2026-10-21"), d("2026-10-22")), grouped.openDays(now, null))
    }
}
