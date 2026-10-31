package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ReschedulePickerUiStateTest {
    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-02T15:00:00Z")
    private val currentStart = Instant.parse("2026-11-04T15:00:00Z")

    private fun booking(isPaid: Boolean = false) =
        PickerBooking(
            viewer = RescheduleViewer.Client,
            counterpartName = "Ada",
            subtitle = "Career advice",
            avatarUrl = null,
            currentStart = currentStart,
            currentEnd = currentStart.plus(Duration.ofMinutes(60)),
            slotsTimezone = "UTC",
            availabilityExpertId = "e1",
            isPaid = isPaid,
            authorizationExpiresAt = null,
        )

    private fun slot(start: Instant) = BookingTimeSlot(start, start.plus(Duration.ofMinutes(30)), true)

    private fun state(
        load: PickerLoad = PickerLoad.Loaded(booking()),
        availability: AvailabilityState = AvailabilityState.Loading,
        selectedDate: LocalDate = LocalDate.of(2026, 11, 5),
        selectedSlotStart: Instant? = null,
        isSubmitting: Boolean = false,
    ) = ReschedulePickerUiState(
        zone = zone,
        now = now,
        displayedMonth = YearMonth.of(2026, 11),
        selectedDate = selectedDate,
        load = load,
        availability = availability,
        selectedSlotStart = selectedSlotStart,
        isSubmitting = isSubmitting,
    )

    @Test
    fun paidBookingCapsLatestStart() {
        val paid = state(PickerLoad.Loaded(booking(isPaid = true)))
        val free = state(PickerLoad.Loaded(booking(isPaid = false)))
        assertEquals(now.plus(Duration.ofDays(6)), paid.latestSelectableStart)
        assertNull(free.latestSelectableStart)
    }

    @Test
    fun latestStartIsNullBeforeBookingLoads() {
        assertNull(state(PickerLoad.Loading).latestSelectableStart)
    }

    @Test
    fun openDaysExcludeCurrentOnlyDay() {
        val other = Instant.parse("2026-11-06T15:00:00Z")
        val availability =
            AvailabilityState.Loaded(
                mapOf(
                    LocalDate.of(2026, 11, 4) to listOf(slot(currentStart)),
                    LocalDate.of(2026, 11, 6) to listOf(slot(other)),
                ),
            )
        assertEquals(setOf(LocalDate.of(2026, 11, 6)), state(availability = availability).openDays)
    }

    @Test
    fun openDaysEmptyWhileAvailabilityNotLoaded() {
        assertTrue(state(availability = AvailabilityState.Loading).openDays.isEmpty())
    }

    @Test
    fun slotsForSelectedDateReadsLoadedMap() {
        val slots = listOf(slot(Instant.parse("2026-11-05T15:00:00Z")))
        val loaded = AvailabilityState.Loaded(mapOf(LocalDate.of(2026, 11, 5) to slots))
        assertEquals(slots, state(availability = loaded).slotsForSelectedDate)
        assertTrue(state(availability = AvailabilityState.Loading).slotsForSelectedDate.isEmpty())
        assertTrue(
            state(availability = AvailabilityState.Failed(FieldError.Text("x"))).slotsForSelectedDate.isEmpty(),
        )
    }

    @Test
    fun canSubmitNeedsBookingSlotAndIdle() {
        val slotStart = Instant.parse("2026-11-05T15:00:00Z")
        val loaded = AvailabilityState.Loaded(mapOf(LocalDate.of(2026, 11, 5) to listOf(slot(slotStart))))
        assertFalse(state(PickerLoad.Loading, loaded, selectedSlotStart = slotStart).canSubmit)
        assertFalse(state(availability = loaded, selectedSlotStart = null).canSubmit)
        assertFalse(state(availability = loaded, selectedSlotStart = slotStart, isSubmitting = true).canSubmit)
        assertTrue(state(availability = loaded, selectedSlotStart = slotStart).canSubmit)
    }

    @Test
    fun todayFollowsZone() {
        val toronto =
            ReschedulePickerUiState(
                zone = ZoneId.of("America/Toronto"),
                now = Instant.parse("2026-11-02T03:00:00Z"),
                displayedMonth = YearMonth.of(2026, 11),
                selectedDate = LocalDate.of(2026, 11, 1),
            )
        assertEquals(LocalDate.of(2026, 11, 1), toronto.today)
    }

    @Test
    fun originalDurationIsEndMinusStart() {
        val b =
            booking().copy(
                currentStart = Instant.parse("2026-11-04T15:00:00Z"),
                currentEnd = Instant.parse("2026-11-04T16:00:00Z"),
            )
        assertEquals(Duration.ofMinutes(60), b.originalDuration)
    }

    @Test
    fun bookingAndCurrentStartComeFromLoaded() {
        val s = state()
        assertEquals(currentStart, s.currentStart)
        assertEquals("Ada", s.booking?.counterpartName)
        assertNull(state(PickerLoad.Loading).booking)
        assertNull(state(PickerLoad.Loading).currentStart)
    }
}
