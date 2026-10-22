package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Booking statuses that can still be joined today (iOS `isJoinable`). Raw server strings, as in Phase 3. */
val JOINABLE_BOOKING_STATUSES: Set<String> = setOf("confirmed", "in_progress", "pending_reschedule")

/**
 * Today's Sessions (D-01): joinable bookings whose start falls on today's date in [zone], ascending by start. Today
 * comes from [clock], so callers pass the device zone per call and a timezone change is honored. A session that has
 * already ended earlier today is kept, as on iOS.
 */
fun List<ClientBookingItem>.todaysSessions(
    clock: Clock,
    zone: ZoneId,
): List<ClientBookingItem> {
    val today = LocalDate.now(clock.withZone(zone))
    return filter { it.status in JOINABLE_BOOKING_STATUSES && it.startTime.atZone(zone).toLocalDate() == today }
        .sortedBy { it.startTime }
}

/** "3:00 PM – 4:00 PM" in the device's 12 or 24 hour style for [locale]. */
fun formatSessionTimeRange(
    start: Instant,
    end: Instant,
    zone: ZoneId,
    locale: Locale,
): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).withZone(zone)
    return "${formatter.format(start)} – ${formatter.format(end)}"
}
