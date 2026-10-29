package com.uptrendinvestments.clickmeapp.model.responses

import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EarningsTransactionsDataTest {
    private fun rows(count: Int) = (1..count).map { earningsTransaction("e$it") }

    @Test
    fun serverFlagWins() {
        val more = EarningsTransactionsData(rows(2), PaginationMeta(currentPage = 3, totalPages = 3, hasMore = true))
        val done = EarningsTransactionsData(rows(20), PaginationMeta(currentPage = 1, totalPages = 3, hasMore = false))
        assertTrue(more.hasMoreRows(pageSize = 20))
        assertFalse(done.hasMoreRows(pageSize = 20))
    }

    @Test
    fun pageCountsAreUsedWithoutTheFlag() {
        assertTrue(EarningsTransactionsData(rows(1), PaginationMeta(1, 2)).hasMoreRows(pageSize = 20))
        assertFalse(EarningsTransactionsData(rows(1), PaginationMeta(2, 2)).hasMoreRows(pageSize = 20))
    }

    @Test
    fun withoutPaginationAFullPageMeansMaybeMore() {
        assertTrue(EarningsTransactionsData(rows(20), pagination = null).hasMoreRows(pageSize = 20))
        assertFalse(EarningsTransactionsData(rows(5), pagination = null).hasMoreRows(pageSize = 20))
    }
}
