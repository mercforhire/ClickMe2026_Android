package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.testing.transactionsPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm() = TransactionsViewModel(ExpertFinancialRepository(api))

    @Test
    fun firstPageLoads() =
        runTest(main.testDispatcher) {
            val rows = listOf(earningsTransaction(id = "e1"), earningsTransaction(id = "e2"))
            api.earningsTransactionsHandler = { _, _ -> transactionsPage(rows) }

            val vm = vm()
            runCurrent()

            assertEquals(listOf(1 to 20), api.earningsTransactionsQueries)
            val state = vm.uiState.value
            assertEquals(TransactionsLoad.Loaded, state.load)
            assertEquals(listOf("e1", "e2"), state.rows.map { it.id })
        }
}
