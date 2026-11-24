package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import java.time.Instant

/** A row in the Past tab: the booking and the badge it shows. */
data class PastRow(
    val item: ClientBookingItem,
    val status: BookingStatusDisplay,
)

/** What the two My Bookings tabs show. */
data class BookingLists(
    val upcoming: List<ClientBookingItem>,
    val past: List<PastRow>,
) {
    companion object {
        val EMPTY = BookingLists(emptyList(), emptyList())
    }
}

/**
 * A completed session whose end time is still in the future: the server already moved it to Past, but the client can
 * still rejoin it, so it belongs in Upcoming (iOS `isRejoinableCompleted`). Strictly after [now].
 *
 * The list keeps this end-based edge on purpose, whatever `can_join` says: iOS `isRejoinableCompleted`
 * (`UpcomingBookingsViewModel.swift` and `MyBookingsViewModel.swift`) moves a completed row to Past at its scheduled
 * end on both lists. Only the detail screens extend a completed booking's Join visibility into the reconnect window
 * (`showsJoinCard`, quick 261123-rdc). The client list payload carries no `can_join` at all.
 */
fun ClientBookingItem.isRejoinableCompleted(now: Instant): Boolean = status == STATUS_COMPLETED && endTime.isAfter(now)

/**
 * Builds both tab lists from the two server buckets (iOS `MyBookingsViewModel` `mapPastIfApplicable` and
 * `isRejoinableCompleted`).
 *
 * Both buckets are needed on every load (RESEARCH Finding 1): the rejoin rule moves rows from the server's Past bucket
 * into Upcoming. Upcoming is the upcoming bucket plus the rejoinable rows, ascending by start. Past drops the
 * rejoinable rows and any status without a Past label, descending by start. Each list holds a booking id at most once,
 * because LazyColumn keys must be unique.
 *
 * Consequence of loading page 1 only (D-09): a rejoinable session beyond the first 20 Past rows is not shown
 * (accepted limit).
 *
 * [now] must come from the caller's injected `Clock`.
 */
fun buildBookingLists(
    upcoming: List<ClientBookingItem>,
    past: List<ClientBookingItem>,
    now: Instant,
): BookingLists {
    val rejoinable = past.filter { it.isRejoinableCompleted(now) }
    return BookingLists(
        upcoming = (upcoming + rejoinable).sortedBy { it.startTime }.distinctBy { it.bookingId },
        past =
            past
                .sortedByDescending { it.startTime }
                .filterNot { it.isRejoinableCompleted(now) }
                .mapNotNull { item -> pastStatusDisplay(item.status)?.let { PastRow(item, it) } }
                .distinctBy { it.item.bookingId },
    )
}
