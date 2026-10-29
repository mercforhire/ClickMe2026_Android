package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/** `GET /expert/earnings/transactions` data: one page of the expert's per-session earnings ledger, newest first. */
@JsonClass(generateAdapter = true)
data class EarningsTransactionsData(
    val transactions: List<EarningsTransactionItem> = emptyList(),
    val pagination: PaginationMeta? = null,
)

/**
 * One ledger row, ported from iOS `EarningsTransactionItem`. The three amounts are integer MINOR units: format them
 * with `Money.format` and never divide by 100 or use `formatMajor` (PAYOUT-03, RESEARCH Pitfall 6). A row whose
 * booking, topic or client cannot be resolved arrives with those fields null and must still be rendered.
 * [payoutStatus] is a raw nullable string on purpose: `settled_direct` is the normal terminal state, and an unknown
 * value must decode instead of failing the whole page.
 */
@JsonClass(generateAdapter = true)
data class EarningsTransactionItem(
    val id: String,
    @Json(name = "booking_id") val bookingId: String? = null,
    @Json(name = "session_date") val sessionDate: Instant? = null,
    @Json(name = "topic_title") val topicTitle: String? = null,
    @Json(name = "client_name") val clientName: String? = null,
    @Json(name = "gross_amount") val grossAmount: Long,
    @Json(name = "commission_amount") val commissionAmount: Long,
    @Json(name = "net_amount") val netAmount: Long,
    val currency: String,
    @Json(name = "payout_status") val payoutStatus: String? = null,
    @Json(name = "processed_at") val processedAt: Instant? = null,
    @Json(name = "created_at") val createdAt: Instant,
)
