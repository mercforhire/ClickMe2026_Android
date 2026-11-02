package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_CONFIRMED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_IN_PROGRESS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_APPROVAL
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_RESCHEDULE
import com.uptrendinvestments.clickmeapp.screens.expertmode.netEarningsText
import java.util.Locale

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

private val AMOUNT_STATUSES =
    setOf(
        STATUS_PENDING_APPROVAL,
        STATUS_CONFIRMED,
        STATUS_PENDING_RESCHEDULE,
        STATUS_IN_PROGRESS,
        STATUS_COMPLETED,
    )

/**
 * One read-only history row (D-10): data only, so a row carries no route to a booking.
 */
data class ClientHistoryRow(
    val item: ExpertBookingItem,
    val bucket: ClientHistoryBucket,
) {
    /**
     * The expert's net earnings for this row, or null when nothing should be shown (D-09; iOS always shows a stub
     * "$0"). The feed's breakdown is populated even on rows that never earned money, so this status gate, and not
     * the data, keeps the screen from implying money that was not earned: `cancelled`, `declined`, `missed`,
     * `expired` and any unknown status show no amount (A1). Null also covers a missing net amount or currency, and
     * gross never stands in for net.
     *
     * RESEARCH A6: client-no-show `missed` rows and late-cancel captured rows were paid, but the list payload cannot
     * tell them apart from unpaid ones, so they show no amount. A details call per row to tell them apart is not
     * worth it (RESEARCH Open Question 1).
     */
    fun amountText(locale: Locale): String? =
        if (item.statusCode in AMOUNT_STATUSES) item.netEarningsText(locale) else null
}

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
