package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.pastStatusDisplay
import java.time.Instant

/** A row in the expert Past tab: the booking and the badge it shows. */
data class ExpertPastRow(
    val item: ExpertBookingItem,
    val status: BookingStatusDisplay,
)

/** What the two expert Bookings tabs show. */
data class ExpertBookingLists(
    val upcoming: List<ExpertBookingItem>,
    val past: List<ExpertPastRow>,
) {
    companion object {
        val EMPTY = ExpertBookingLists(emptyList(), emptyList())
    }
}

/**
 * A completed session whose end time is still in the future: the server already moved it to Past, but both parties can
 * still rejoin it, so it belongs in Upcoming (iOS `isRejoinableCompleted`, D-15). Strictly after [now].
 *
 * The list keeps this end-based edge on purpose, whatever `can_join` says: iOS `isRejoinableCompleted`
 * (`UpcomingBookingsViewModel.swift` and `MyBookingsViewModel.swift`) moves a completed row to Past at its scheduled
 * end on both lists. Only the detail screens extend a completed booking's Join visibility into the reconnect window
 * (`showsJoinCard`, quick 261123-rdc).
 */
fun ExpertBookingItem.isRejoinableCompleted(now: Instant): Boolean =
    statusCode == STATUS_COMPLETED && endTime.isAfter(now)

/**
 * Builds both expert tab lists from the two server buckets (iOS `UpcomingBookingsViewModel` `isUpcoming`,
 * `isRejoinableCompleted` and `mapPastIfApplicable`, D-15, RESEARCH C12).
 *
 * Both buckets are needed on every load: the rejoin rule moves rows from the server's Past bucket into Upcoming.
 * Upcoming is the upcoming bucket's joinable-status rows whose end is still in the future (the feed also returns
 * `pending_approval`, which belongs on the Incoming Requests screen and is excluded), plus the rejoinable rows,
 * ascending by start. Past drops the rejoinable rows and any status without a Past label, descending by start. Each
 * list holds a booking id at most once, because LazyColumn keys must be unique.
 *
 * Consequence of loading page 1 only (limit 50): a rejoinable session beyond the first 50 Past rows is not shown
 * (accepted limit, as Phase 8 D-09).
 *
 * [now] must come from the caller's injected `Clock`.
 */
fun buildExpertBookingLists(
    upcoming: List<ExpertBookingItem>,
    past: List<ExpertBookingItem>,
    now: Instant,
): ExpertBookingLists {
    val live = upcoming.filter { it.statusCode in JOINABLE_BOOKING_STATUSES && it.endTime.isAfter(now) }
    val rejoinable = past.filter { it.isRejoinableCompleted(now) }
    return ExpertBookingLists(
        upcoming = (live + rejoinable).sortedBy { it.startTime }.distinctBy { it.bookingId },
        past =
            past
                .sortedByDescending { it.startTime }
                .filterNot { it.isRejoinableCompleted(now) }
                .mapNotNull { item -> pastStatusDisplay(item.statusCode)?.let { ExpertPastRow(item, it) } }
                .distinctBy { it.item.bookingId },
    )
}
