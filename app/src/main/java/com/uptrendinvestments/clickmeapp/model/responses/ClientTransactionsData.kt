package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/** `GET /client/transactions` data: one page of the client's own payment history, newest first. */
@JsonClass(generateAdapter = true)
data class ClientTransactionsData(
    val transactions: List<ClientTransactionItem> = emptyList(),
    val pagination: PaginationMeta? = null,
) {
    /** Whether the server has more rows after this page. Callers page forward on this only. */
    val hasMore: Boolean get() = pagination?.hasMoreOrDerived == true
}

/**
 * One payment-history row, ported from iOS `ClientTransactionItem`. [amount] and [refundedAmount] are integer MINOR
 * units; a null [amount] means no amount was ever set and renders as an em dash, never as zero. [paymentStatus] is
 * deliberately a raw string holding this endpoint's own 11-value vocabulary (it is never shared with
 * `/client/bookings/{id}`, which collapses several states to `none`), so an unknown value still decodes. The
 * backend's always-null refund timestamp is deliberately not declared and Moshi ignores it. [toString] prints the
 * booking id and the status only: the expert name and the topic title are personal data.
 */
@JsonClass(generateAdapter = true)
data class ClientTransactionItem(
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "session_date") val sessionDate: Instant? = null,
    @Json(name = "topic_title") val topicTitle: String? = null,
    @Json(name = "expert_name") val expertName: String? = null,
    @Json(name = "expert_avatar_url") val expertAvatarUrl: String? = null,
    val amount: Long? = null,
    val currency: String = "CAD",
    @Json(name = "payment_status") val paymentStatus: String = "none",
    @Json(name = "refunded_amount") val refundedAmount: Long = 0L,
    @Json(name = "paid_at") val paidAt: Instant? = null,
    @Json(name = "created_at") val createdAt: Instant,
) {
    override fun toString(): String = "ClientTransactionItem(bookingId=$bookingId, paymentStatus=$paymentStatus)"
}
