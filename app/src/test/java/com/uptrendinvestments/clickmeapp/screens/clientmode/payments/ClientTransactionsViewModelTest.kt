package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.clientTransaction
import com.uptrendinvestments.clickmeapp.testing.clientTransactionsPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private companion object {
        const val SERVER_DOWN = """{"status":"error","message":"Down"}"""
    }
}
