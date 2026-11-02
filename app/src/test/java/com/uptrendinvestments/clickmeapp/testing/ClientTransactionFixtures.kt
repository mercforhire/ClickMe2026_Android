package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionsData
import com.uptrendinvestments.clickmeapp.model.responses.PaginationMeta
import java.time.Instant

/*
 * Phase 16.5 payload builders for the client payment history (`GET /client/transactions`). Defaults mirror a realistic
 * captured row; override only what a test cares about. Amounts are MINOR units.
 */

/** One payment-history row. */
fun clientTransaction(
    bookingId: String = "b1",
    sessionDate: Instant? = Instant.parse("2026-08-17T14:30:00Z"),
    topic: String? = "Algorithms",
    expert: String? = "Ada Lovelace",
    amount: Long? = 7500L,
    currency: String = "CAD",
    status: String = "captured",
    refunded: Long = 0L,
    createdAt: Instant = Instant.parse("2026-08-17T14:23:14Z"),
): ClientTransactionItem =
    ClientTransactionItem(
        bookingId = bookingId,
        sessionDate = sessionDate,
        topicTitle = topic,
        expertName = expert,
        amount = amount,
        currency = currency,
        paymentStatus = status,
        refundedAmount = refunded,
        createdAt = createdAt,
    )

/** One page of the payment history. */
fun clientTransactionsPage(
    items: List<ClientTransactionItem> = listOf(clientTransaction()),
    page: Int = 1,
    hasMore: Boolean = false,
): ClientTransactionsData =
    ClientTransactionsData(
        transactions = items,
        pagination =
            PaginationMeta(
                currentPage = page,
                totalPages = if (hasMore) page + 1 else page,
                totalCount = items.size,
                hasMore = hasMore,
            ),
    )
