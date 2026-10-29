@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One bookable time slot in the viewer's calendar.
 *
 * @property isAvailable already folds in `held` (iOS: `(available ?: true) && !(held ?? false)`).
 * Past-ness is not part of it; see [isSelectable].
 */
data class BookingTimeSlot(
    val start: Instant,
    val end: Instant?,
    val isAvailable: Boolean,
)

/** The paid booking window in whole days (quick 261028-to1). */
const val PAID_BOOKING_MAX_LEAD_DAYS: Long = 6

/**
 * How far ahead a PAID topic can be booked (quick 261028-to1, user decision 2026-10-28, option B).
 *
 * Mirrors the backend `LONG_LEAD_THRESHOLD_MS = 6*24h`, which is applied with a strict greater-than
 * (booking.controller.js:382), so a start exactly 6 days out is still short-lead and bookable.
 */
val PAID_BOOKING_MAX_LEAD: Duration = Duration.ofDays(PAID_BOOKING_MAX_LEAD_DAYS)

/**
 * D-04: a slot can be chosen only if it is available and starts after [now]. The server never filters past slots.
 * For a paid topic [latestStart] (now + [PAID_BOOKING_MAX_LEAD]) is the inclusive upper bound; null means no limit.
 */
fun BookingTimeSlot.isSelectable(
    now: Instant,
    latestStart: Instant?,
): Boolean = isAvailable && start.isAfter(now) && (latestStart == null || !start.isAfter(latestStart))

/**
 * Regroups every slot of [days] by the viewer's local date (D-07, BOOK-02).
 *
 * The caller flattens the previous, center and next month days into ONE list and calls this once. The
 * server's expert-local `day.date` is never read: each slot is filed under `startUtc` converted to [zone].
 * Duplicate start instants (the same slot present in neighbouring months) collapse to one entry whose
 * availability is the AND of all copies, so it is bookable only if every copy says so. Each day's list is
 * sorted ascending by start.
 *
 * @param zone the viewer's zone, supplied by the caller (never read from the system here).
 */
fun groupSlots(
    days: List<AvailabilityDayDto>,
    zone: ZoneId,
): Map<LocalDate, List<BookingTimeSlot>> {
    val byStart = LinkedHashMap<Instant, BookingTimeSlot>()
    for (day in days) {
        for (slot in day.slots.orEmpty()) {
            val start = slot.startUtc ?: continue
            val ok = (slot.available ?: true) && slot.held != true
            val existing = byStart[start]
            byStart[start] =
                if (existing == null) {
                    BookingTimeSlot(start, slot.endUtc, ok)
                } else {
                    existing.copy(isAvailable = existing.isAvailable && ok)
                }
        }
    }
    return byStart.values
        .sortedBy { it.start }
        .groupBy { it.start.atZone(zone).toLocalDate() }
}

/** D-03: the days that have at least one selectable slot, within the paid window when [latestStart] is set. */
fun Map<LocalDate, List<BookingTimeSlot>>.openDays(
    now: Instant,
    latestStart: Instant?,
): Set<LocalDate> = filterValues { slots -> slots.any { it.isSelectable(now, latestStart) } }.keys
