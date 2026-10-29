package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionsData
import com.uptrendinvestments.clickmeapp.model.responses.PaginationMeta
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.testing.transactionsPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm() = TransactionsViewModel(ExpertFinancialRepository(api))

    private fun rows(
        from: Int,
        count: Int,
    ): List<EarningsTransactionItem> = (from until from + count).map { earningsTransaction(id = "e$it") }

    private fun serveTwoPages() {
        api.earningsTransactionsHandler = { page, _ ->
            if (page == 1) {
                transactionsPage(rows(0, TRANSACTIONS_PAGE_SIZE), page = 1, hasMore = true)
            } else {
                transactionsPage(rows(TRANSACTIONS_PAGE_SIZE, 5), page = page, hasMore = false)
            }
        }
    }

    private val TransactionsViewModel.ids: List<String>
        get() = uiState.value.rows.map { it.id }

    @Test
    fun firstPageLoads() =
        runTest(main.testDispatcher) {
            val rows = listOf(earningsTransaction(id = "e1"), earningsTransaction(id = "e2"))
            api.earningsTransactionsHandler = { _, _ -> transactionsPage(rows) }

            val vm = vm()
            runCurrent()

            assertEquals(listOf(1 to 20), api.earningsTransactionsQueries)
            assertEquals(TransactionsLoad.Loaded, vm.uiState.value.load)
            assertEquals(listOf("e1", "e2"), vm.ids)
        }

    @Test
    fun nextPageWhenNearTail() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()

            vm.onRowVisible(16)
            runCurrent()
            assertEquals(listOf(1 to 20), api.earningsTransactionsQueries)

            vm.onRowVisible(17)
            assertTrue(vm.uiState.value.isPaging)
            vm.onRowVisible(19)
            runCurrent()

            assertEquals(listOf(1 to 20, 2 to 20), api.earningsTransactionsQueries)
            assertFalse(vm.uiState.value.isPaging)
            assertEquals(25, vm.uiState.value.rows.size)
        }

    @Test
    fun noMoreWhenHasMoreFalse() =
        runTest(main.testDispatcher) {
            api.earningsTransactionsHandler = { _, _ -> transactionsPage(rows(0, 20), hasMore = false) }
            val vm = vm()
            runCurrent()

            vm.onRowVisible(19)
            runCurrent()

            assertEquals(listOf(1 to 20), api.earningsTransactionsQueries)
        }

    @Test
    fun hasMoreIsDerivedFromPageCountsWhenFlagIsNull() =
        runTest(main.testDispatcher) {
            api.earningsTransactionsHandler = { page, _ ->
                EarningsTransactionsData(
                    transactions = rows((page - 1) * 20, 20),
                    pagination = PaginationMeta(currentPage = page, totalPages = 2, hasMore = null),
                )
            }
            val vm = vm()
            runCurrent()

            vm.onRowVisible(19)
            runCurrent()
            assertEquals(listOf(1 to 20, 2 to 20), api.earningsTransactionsQueries)

            vm.onRowVisible(39)
            runCurrent()
            assertEquals(listOf(1 to 20, 2 to 20), api.earningsTransactionsQueries)
        }

    @Test
    fun appendKeepsOrderAndDropsDuplicates() =
        runTest(main.testDispatcher) {
            api.earningsTransactionsHandler = { page, _ ->
                if (page == 1) {
                    transactionsPage(rows(0, 20), page = 1, hasMore = true)
                } else {
                    transactionsPage(rows(19, 3), page = 2, hasMore = false)
                }
            }
            val vm = vm()
            runCurrent()

            vm.onRowVisible(19)
            runCurrent()

            assertEquals((0 until 22).map { "e$it" }, vm.ids)
        }

    @Test
    fun pageErrorKeepsRows() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.earningsTransactionsHandler = { _, _ -> throw api.httpError(500, SERVER_DOWN) }

            vm.onRowVisible(19)
            runCurrent()

            assertEquals(TRANSACTIONS_PAGE_SIZE, vm.uiState.value.rows.size)
            assertEquals(FieldError.Text("Down"), vm.uiState.value.pageError)
            assertEquals(TransactionsLoad.Loaded, vm.uiState.value.load)

            // While the error shows, scrolling does not hammer the server.
            vm.onRowVisible(19)
            runCurrent()
            assertEquals(listOf(1 to 20, 2 to 20), api.earningsTransactionsQueries)

            serveTwoPages()
            vm.retryPage()
            runCurrent()

            assertEquals(listOf(1 to 20, 2 to 20, 2 to 20), api.earningsTransactionsQueries)
            assertNull(vm.uiState.value.pageError)
            assertEquals(25, vm.uiState.value.rows.size)
        }

    @Test
    fun emptyAndFailedFirstPage() =
        runTest(main.testDispatcher) {
            api.earningsTransactionsHandler = { _, _ -> transactionsPage(emptyList()) }
            val empty = vm()
            runCurrent()
            assertEquals(TransactionsLoad.Empty, empty.uiState.value.load)

            api.earningsTransactionsHandler = { _, _ -> throw api.httpError(500, SERVER_DOWN) }
            val failed = vm()
            runCurrent()
            assertEquals(TransactionsLoad.Failed(FieldError.Text("Down")), failed.uiState.value.load)

            api.earningsTransactionsHandler = { _, _ -> transactionsPage(rows(0, 2)) }
            failed.retry()
            assertEquals(TransactionsLoad.Loading, failed.uiState.value.load)
            runCurrent()
            assertEquals(TransactionsLoad.Loaded, failed.uiState.value.load)
            assertEquals(listOf("e0", "e1"), failed.ids)
        }

    @Test
    fun refreshReplacesList() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            vm.onRowVisible(19)
            runCurrent()
            assertEquals(25, vm.uiState.value.rows.size)

            api.earningsTransactionsHandler = { _, _ -> transactionsPage(rows(100, 2), hasMore = false) }
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()

            assertEquals(1 to 20, api.earningsTransactionsQueries.last())
            assertEquals(listOf("e100", "e101"), vm.ids)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun refreshFailureKeepsRowsAndFlagsIt() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.earningsTransactionsHandler = { _, _ -> throw api.httpError(500, SERVER_DOWN) }

            vm.refresh()
            runCurrent()

            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(TRANSACTIONS_PAGE_SIZE, vm.uiState.value.rows.size)
            assertEquals(TransactionsLoad.Loaded, vm.uiState.value.load)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    private companion object {
        const val SERVER_DOWN = """{"status":"error","message":"Down"}"""
    }
}
