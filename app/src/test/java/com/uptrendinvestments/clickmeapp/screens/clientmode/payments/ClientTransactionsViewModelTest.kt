package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.clientTransaction
import com.uptrendinvestments.clickmeapp.testing.clientTransactionsPage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClientTransactionsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm() = ClientTransactionsViewModel(ClientTransactionsRepository(api))

    private fun rows(
        from: Int,
        count: Int,
    ): List<ClientTransactionItem> = (from until from + count).map { clientTransaction(bookingId = "b$it") }

    private val ClientTransactionsViewModel.ids: List<String>
        get() = uiState.value.rows.map { it.bookingId }

    @Test
    fun firstPageLoads() =
        runTest(main.testDispatcher) {
            val rows = listOf(clientTransaction(bookingId = "b1"), clientTransaction(bookingId = "b2"))
            api.clientTransactionsHandler = { _, _ -> clientTransactionsPage(rows) }

            val vm = vm()
            runCurrent()

            assertEquals(listOf(1 to 20), api.clientTransactionsQueries)
            assertEquals(ClientTransactionsLoad.Loaded, vm.uiState.value.load)
            assertEquals(listOf("b1", "b2"), vm.ids)
            assertFalse(vm.uiState.value.isPaging)
        }

    @Test
    fun emptyFirstPageIsEmpty() =
        runTest(main.testDispatcher) {
            api.clientTransactionsHandler = { _, _ -> clientTransactionsPage(emptyList()) }

            val vm = vm()
            runCurrent()

            assertEquals(ClientTransactionsLoad.Empty, vm.uiState.value.load)
        }

    @Test
    fun firstPageFailureIsFailed() =
        runTest(main.testDispatcher) {
            api.clientTransactionsHandler = { _, _ -> throw java.io.IOException("offline") }

            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is ClientTransactionsLoad.Failed)

            api.clientTransactionsHandler = { _, _ -> clientTransactionsPage(rows(0, 2)) }
            vm.retry()
            assertEquals(ClientTransactionsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(ClientTransactionsLoad.Loaded, vm.uiState.value.load)
            assertEquals(listOf("b0", "b1"), vm.ids)
        }

    @Test
    fun firstPage500IsARealError() =
        runTest(main.testDispatcher) {
            api.clientTransactionsHandler = { _, _ -> throw api.httpError(500, SERVER_DOWN) }

            val vm = vm()
            runCurrent()

            assertEquals(ClientTransactionsLoad.Failed(FieldError.Text("Down")), vm.uiState.value.load)
            assertTrue(
                vm.uiState.value.rows
                    .isEmpty(),
            )
        }

    @Test
    fun duplicateIdsInAPageAreDropped() =
        runTest(main.testDispatcher) {
            val dup = clientTransaction(bookingId = "b1")
            api.clientTransactionsHandler = { _, _ -> clientTransactionsPage(listOf(dup, dup)) }

            val vm = vm()
            runCurrent()

            assertEquals(listOf("b1"), vm.ids)
        }

    private fun serveTwoPages() {
        api.clientTransactionsHandler = { page, _ ->
            if (page == 1) {
                clientTransactionsPage(rows(0, CLIENT_TRANSACTIONS_PAGE_SIZE), page = 1, hasMore = true)
            } else {
                clientTransactionsPage(rows(CLIENT_TRANSACTIONS_PAGE_SIZE, 5), page = page, hasMore = false)
            }
        }
    }

    @Test
    fun nextPageWhenARowWithinTheLastThreeAppears() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()

            vm.onRowVisible(16)
            runCurrent()
            assertEquals(listOf(1 to 20), api.clientTransactionsQueries)

            vm.onRowVisible(17)
            runCurrent()

            assertEquals(listOf(1 to 20, 2 to 20), api.clientTransactionsQueries)
            assertEquals((0 until 25).map { "b$it" }, vm.ids)
        }

    @Test
    fun onlyOnePageRequestAtATime() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            api.clientTransactionsHandler = { page, _ ->
                gate.await()
                clientTransactionsPage(rows(CLIENT_TRANSACTIONS_PAGE_SIZE, 5), page = page, hasMore = false)
            }

            vm.onRowVisible(19)
            runCurrent()
            assertTrue(vm.uiState.value.isPaging)
            vm.onRowVisible(19)
            vm.onRowVisible(18)
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(1 to 20, 2 to 20), api.clientTransactionsQueries)
            assertFalse(vm.uiState.value.isPaging)
        }

    @Test
    fun hasMoreFalseStopsPaging() =
        runTest(main.testDispatcher) {
            api.clientTransactionsHandler = { _, _ -> clientTransactionsPage(rows(0, 20), hasMore = false) }
            val vm = vm()
            runCurrent()

            vm.onRowVisible(19)
            runCurrent()

            assertEquals(listOf(1 to 20), api.clientTransactionsQueries)
        }

    @Test
    fun page2Status500EndsTheListSilently() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.clientTransactionsHandler = { _, _ -> throw api.httpError(500, SERVER_DOWN) }

            vm.onRowVisible(19)
            runCurrent()

            assertNull(vm.uiState.value.pageError)
            assertFalse(vm.uiState.value.isPaging)
            assertEquals(CLIENT_TRANSACTIONS_PAGE_SIZE, vm.uiState.value.rows.size)
            assertEquals(ClientTransactionsLoad.Loaded, vm.uiState.value.load)

            vm.onRowVisible(19)
            runCurrent()
            assertEquals(listOf(1 to 20, 2 to 20), api.clientTransactionsQueries)
        }

    @Test
    fun page2OtherFailureShowsTheFooter() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.clientTransactionsHandler = { _, _ -> throw api.httpError(503, SERVER_DOWN) }

            vm.onRowVisible(19)
            runCurrent()

            assertEquals(FieldError.Text("Down"), vm.uiState.value.pageError)
            assertEquals(CLIENT_TRANSACTIONS_PAGE_SIZE, vm.uiState.value.rows.size)

            vm.onRowVisible(19)
            runCurrent()
            assertEquals(listOf(1 to 20, 2 to 20), api.clientTransactionsQueries)

            serveTwoPages()
            vm.retryPage()
            runCurrent()

            assertEquals(listOf(1 to 20, 2 to 20, 2 to 20), api.clientTransactionsQueries)
            assertNull(vm.uiState.value.pageError)
            assertEquals(25, vm.uiState.value.rows.size)
        }

    @Test
    fun page2NetworkFailureShowsTheFooter() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.clientTransactionsHandler = { _, _ -> throw java.io.IOException("offline") }

            vm.onRowVisible(19)
            runCurrent()

            assertNotNull(vm.uiState.value.pageError)
            assertEquals(CLIENT_TRANSACTIONS_PAGE_SIZE, vm.uiState.value.rows.size)
        }

    @Test
    fun refreshKeepsRowsThenReplacesThem() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            vm.onRowVisible(19)
            runCurrent()
            assertEquals(25, vm.uiState.value.rows.size)

            val gate = CompletableDeferred<Unit>()
            api.clientTransactionsHandler = { _, _ ->
                gate.await()
                clientTransactionsPage(rows(100, 20), hasMore = true)
            }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(25, vm.uiState.value.rows.size)

            gate.complete(Unit)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals((100 until 120).map { "b$it" }, vm.ids)

            vm.onRowVisible(19)
            runCurrent()
            assertEquals(2 to 20, api.clientTransactionsQueries.last())
        }

    @Test
    fun refreshFailureKeepsRowsAndFlagsIt() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.clientTransactionsHandler = { _, _ -> throw api.httpError(500, SERVER_DOWN) }

            vm.refresh()
            runCurrent()

            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(CLIENT_TRANSACTIONS_PAGE_SIZE, vm.uiState.value.rows.size)
            assertEquals(ClientTransactionsLoad.Loaded, vm.uiState.value.load)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun refreshCancelsAPageInFlight() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            api.clientTransactionsHandler = { page, _ ->
                if (page == 1) {
                    clientTransactionsPage(rows(100, 3), hasMore = false)
                } else {
                    gate.await()
                    clientTransactionsPage(rows(200, 5), page = page, hasMore = false)
                }
            }
            vm.onRowVisible(19)
            runCurrent()
            assertTrue(vm.uiState.value.isPaging)

            vm.refresh()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf("b100", "b101", "b102"), vm.ids)
            assertFalse(vm.uiState.value.isPaging)
        }

    @Test
    fun retryPageDuringARefreshIsIgnored() =
        runTest(main.testDispatcher) {
            serveTwoPages()
            val vm = vm()
            runCurrent()
            api.clientTransactionsHandler = { _, _ -> throw api.httpError(503, SERVER_DOWN) }
            vm.onRowVisible(19)
            runCurrent()
            assertEquals(FieldError.Text("Down"), vm.uiState.value.pageError)

            val gate = CompletableDeferred<Unit>()
            api.clientTransactionsHandler = { _, _ ->
                gate.await()
                clientTransactionsPage(rows(0, CLIENT_TRANSACTIONS_PAGE_SIZE), page = 1, hasMore = true)
            }
            vm.refresh()
            runCurrent()
            vm.retryPage()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(1 to 20, 2 to 20, 1 to 20), api.clientTransactionsQueries)
            assertNull(vm.uiState.value.pageError)
        }

    @Test
    fun appendDropsARepeatedBookingId() =
        runTest(main.testDispatcher) {
            api.clientTransactionsHandler = { page, _ ->
                if (page == 1) {
                    clientTransactionsPage(rows(0, 20), page = 1, hasMore = true)
                } else {
                    clientTransactionsPage(rows(19, 3), page = 2, hasMore = false)
                }
            }
            val vm = vm()
            runCurrent()

            vm.onRowVisible(19)
            runCurrent()

            assertEquals((0 until 22).map { "b$it" }, vm.ids)
        }

    private companion object {
        const val SERVER_DOWN = """{"status":"error","message":"Down"}"""
    }
}
