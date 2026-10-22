@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
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

/** D-04: a slot can be chosen only if it is available and starts after [now]. The server never filters past slots. */
fun BookingTimeSlot.isSelectable(now: Instant): Boolean = isAvailable && start.isAfter(now)

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

/** D-03: the days that have at least one selectable slot. */
fun Map<LocalDate, List<BookingTimeSlot>>.openDays(now: Instant): Set<LocalDate> =
    filterValues { slots -> slots.any { it.isSelectable(now) } }.keys
