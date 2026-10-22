package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import java.text.DateFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

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

/**
 * "3:00 PM – 4:00 PM": [start] and [end] in [zone], each formatted with [timeFormat]. Pass the device time format
 * (`android.text.format.DateFormat.getTimeFormat(context)`) from the UI so the user's 12 or 24 hour setting wins over
 * the locale default and a locale change takes effect on the next composition. [timeFormat] is not mutated.
 */
fun formatSessionTimeRange(
    start: Instant,
    end: Instant,
    zone: ZoneId,
    timeFormat: DateFormat,
): String {
    val format = (timeFormat.clone() as DateFormat).apply { timeZone = TimeZone.getTimeZone(zone) }
    return "${format.format(Date.from(start))} – ${format.format(Date.from(end))}"
}
