package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED

private const val STATUS_CANCELLED = "cancelled"
private const val STATUS_DECLINED = "declined"
private const val STATUS_MISSED = "missed"
private const val STATUS_EXPIRED = "expired"

/** The three groups a Client Profile history row falls into (iOS `ClientProfileViewModel.mapStatus`). */
enum class ClientHistoryBucket { Upcoming, Completed, Cancelled }

/**
 * Port of iOS `mapStatus`: `completed` is [ClientHistoryBucket.Completed]; `cancelled`, `declined`, `missed` and
 * `expired` are [ClientHistoryBucket.Cancelled]; everything else, `pending_approval`, `confirmed`,
 * `pending_reschedule`, `in_progress` and any code this build does not know, is [ClientHistoryBucket.Upcoming]
 * (Phase 16.4 flagged assumption A1).
 */
fun clientHistoryBucket(code: String): ClientHistoryBucket =
    when (code) {
        STATUS_COMPLETED -> ClientHistoryBucket.Completed
        STATUS_CANCELLED, STATUS_DECLINED, STATUS_MISSED, STATUS_EXPIRED -> ClientHistoryBucket.Cancelled
        else -> ClientHistoryBucket.Upcoming
    }

/**
 * One read-only history row (D-10): data only, so a row carries no route to a booking.
 */
data class ClientHistoryRow(
    val item: ExpertBookingItem,
    val bucket: ClientHistoryBucket,
)

/**
 * Port of the merge in iOS `ClientProfileViewModel.forceLoad`: both bookings buckets, narrowed to [clientId], newest
 * first.
 *
 * The client id compare ignores case on purpose (D-11): iOS and Android UUID casing differ (RESEARCH Pitfall 7).
 * This is deliberately not `buildExpertBookingLists`, which drops `pending_approval` and statuses with no Past label
 * and moves rejoinable completed rows (RESEARCH Pitfall 3). `distinctBy` keeps a duplicate booking id from breaking
 * list keys.
 */
fun buildClientHistory(
    clientId: String,
    upcoming: List<ExpertBookingItem>,
    past: List<ExpertBookingItem>,
): List<ClientHistoryRow> =
    (upcoming + past)
        .filter { it.client.id.equals(clientId, ignoreCase = true) }
        .distinctBy { it.bookingId }
        .sortedByDescending { it.startTime }
        .map { ClientHistoryRow(it, clientHistoryBucket(it.statusCode)) }
