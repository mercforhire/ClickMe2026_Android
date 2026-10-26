package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.util.Money
import java.time.Duration
import java.time.Instant
import java.util.Locale

/** The wire status the server's cron sets on a request that ran out of time. */
private const val STATUS_EXPIRED = "expired"

/**
 * Whether this request can no longer be decided. The server flips a row to `expired` by cron, so a row can still read
 * pending after its `expires_at` passed: either condition counts (RESEARCH C3). A null `expires_at` on a row that is
 * not marked expired is treated as pending, and the server's 409 ALREADY_PROCESSED is the backstop.
 */
fun BookingRequestItem.isExpiredAt(now: Instant): Boolean =
    status == STATUS_EXPIRED || (expiresAt != null && !expiresAt.isAfter(now))

/**
 * One Incoming Requests card's data: the server [item] plus the expiry facts worked out once at load time.
 *
 * @property hoursLeft whole hours from load time to `expires_at` for a pending row that has an expiry; null for an
 * expired row and for a pending row without an expiry.
 */
data class IncomingRequestRow(
    val item: BookingRequestItem,
    val isExpired: Boolean,
    val hoursLeft: Long?,
)

/**
 * Builds the list in display order (iOS `IncomingRequestsViewModel`): pending rows by soonest expiry first (a row
 * without an expiry after the dated ones), then expired rows, also oldest expiry first. The sort is stable, so ties
 * keep server order.
 */
fun buildIncomingRequestRows(
    items: List<BookingRequestItem>,
    now: Instant,
): List<IncomingRequestRow> {
    val rows =
        items.map { item ->
            val expired = item.isExpiredAt(now)
            val expiresAt = item.expiresAt
            IncomingRequestRow(
                item = item,
                isExpired = expired,
                hoursLeft = if (!expired && expiresAt != null) Duration.between(now, expiresAt).toHours() else null,
            )
        }
    val byExpiry = compareBy<IncomingRequestRow, Instant?>(nullsLast()) { it.item.expiresAt }
    return rows.filter { !it.isExpired }.sortedWith(byExpiry) + rows.filter { it.isExpired }.sortedWith(byExpiry)
}

/** What a card's earnings column shows. */
sealed interface RequestCardEarnings {
    /** The expert's net payout, already formatted for the currency. */
    data class Net(
        val amount: String,
    ) : RequestCardEarnings

    /** A free session: the price label "Free". */
    data object Free : RequestCardEarnings

    /** A paid session whose net amount the server could not work out (UI-SPEC A6). */
    data object PayoutPending : RequestCardEarnings
}

/**
 * The earnings column for [item] (RESEARCH C4). The figure is the net amount from `earnings_breakdown` in major units;
 * the gross amount is read only to recognise a free session and is never shown as earnings.
 */
fun requestCardEarnings(
    item: BookingRequestItem,
    locale: Locale = Locale.getDefault(),
): RequestCardEarnings {
    val breakdown = item.earningsBreakdown
    val listEarningsIsZero = item.earnings == null || item.earnings == 0.0
    val grossIsZero = breakdown?.grossAmount == null || breakdown.grossAmount == 0.0
    val net = breakdown?.netAmount
    val currency = breakdown?.currency ?: item.currency
    return when {
        listEarningsIsZero && grossIsZero -> RequestCardEarnings.Free
        net != null && currency != null -> RequestCardEarnings.Net(Money.formatMajor(net, currency, locale))
        else -> RequestCardEarnings.PayoutPending
    }
}

/**
 * The key that opens Request Decision for this row. The detail endpoint returns no status or expiry, so the row's
 * [IncomingRequestRow.isExpired], expiry and list earnings travel in the key (RESEARCH C3, C4).
 */
fun IncomingRequestRow.toDecisionKey(): RequestDecisionKey =
    RequestDecisionKey(
        requestId = item.requestId,
        isExpired = isExpired,
        expiresAtMillis = item.expiresAt?.toEpochMilli(),
        listEarningsMajor = item.earnings,
        listCurrency = item.currency ?: item.earningsBreakdown?.currency,
    )
