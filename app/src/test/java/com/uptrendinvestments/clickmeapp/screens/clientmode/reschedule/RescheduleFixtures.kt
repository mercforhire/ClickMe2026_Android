package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The clock every picker test uses: Mon 2026-11-02 08:00 in Toronto (EST). */
val PickerNow: Instant = Instant.parse("2026-11-02T13:00:00Z")

/** A booking behind the reschedule picker, with the defaults the picker tests share. */
fun pickerBooking(
    viewer: RescheduleViewer = RescheduleViewer.Client,
    counterpartName: String? = "Ada Lovelace",
    subtitle: String? = "Engineer",
    currentStart: Instant = Instant.parse("2026-11-04T15:00:00Z"),
    currentEnd: Instant = currentStart.plus(Duration.ofMinutes(60)),
    slotsTimezone: String? = "America/Toronto",
    availabilityExpertId: String = "e1",
    isPaid: Boolean = false,
    authorizationExpiresAt: Instant? = null,
): PickerBooking =
    PickerBooking(
        viewer = viewer,
        counterpartName = counterpartName,
        subtitle = subtitle,
        avatarUrl = null,
        currentStart = currentStart,
        currentEnd = currentEnd,
        slotsTimezone = slotsTimezone,
        availabilityExpertId = availabilityExpertId,
        isPaid = isPaid,
        authorizationExpiresAt = authorizationExpiresAt,
    )

/** [starts] as one day of slots; every start must fall on [date] in [zone]. */
fun slotsOn(
    date: LocalDate,
    zone: ZoneId,
    vararg starts: Instant,
    available: Boolean = true,
): Map<LocalDate, List<BookingTimeSlot>> {
    starts.forEach { require(it.atZone(zone).toLocalDate() == date) { "$it is not on $date in $zone" } }
    return mapOf(date to starts.map { BookingTimeSlot(it, it.plus(Duration.ofMinutes(60)), available) })
}

/** The booking's own day with its current slot and two later ones on the same day. */
fun defaultPickerSlots(
    booking: PickerBooking,
    zone: ZoneId,
): Map<LocalDate, List<BookingTimeSlot>> =
    slotsOn(
        booking.currentStart.atZone(zone).toLocalDate(),
        zone,
        booking.currentStart,
        booking.currentStart.plus(Duration.ofHours(1)),
        booking.currentStart.plus(Duration.ofHours(2)),
    )

/**
 * A Loaded [ReschedulePickerUiState]. [selectedDate] defaults to the booking's own day so the Current chip is on
 * screen. Pass [load] to build a Loading or Failed state.
 */
@Suppress("LongParameterList")
fun pickerState(
    booking: PickerBooking = pickerBooking(),
    now: Instant = PickerNow,
    zone: ZoneId = ZoneId.of("America/Toronto"),
    selectedDate: LocalDate = booking.currentStart.atZone(zone).toLocalDate(),
    slots: Map<LocalDate, List<BookingTimeSlot>> = defaultPickerSlots(booking, zone),
    selectedSlotStart: Instant? = null,
    isSubmitting: Boolean = false,
    dialog: ReschedulePickerDialog? = null,
    load: PickerLoad = PickerLoad.Loaded(booking),
    availability: AvailabilityState = AvailabilityState.Loaded(slots),
    message: String = "",
): ReschedulePickerUiState =
    ReschedulePickerUiState(
        zone = zone,
        now = now,
        displayedMonth = YearMonth.from(selectedDate),
        selectedDate = selectedDate,
        load = load,
        availability = availability,
        selectedSlotStart = selectedSlotStart,
        message = message,
        isSubmitting = isSubmitting,
        dialog = dialog,
    )
