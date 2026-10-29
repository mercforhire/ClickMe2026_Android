@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.connectStatus
import com.uptrendinvestments.clickmeapp.testing.payoutSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PayoutsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val guard = FakeCallGuard()

    private suspend fun TestScope.harness(): SignupTestHarness {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.signIn()
        h.api.expertBookingsHandler = { ExpertBookingsPayload() }
        return h
    }

    private fun vm(h: SignupTestHarness) =
        PayoutsViewModel(ExpertFinancialRepository(h.api), ExpertBookingsRepository(h.api), guard)

    private fun SignupTestHarness.onboardedWithEarnings() {
        api.connectStatusHandler = { connectStatus() }
        api.payoutSummaryHandler =
            { payoutSummary(available = 12_500L, pending = 3_000L, nextPayoutDate = "2026-11-07") }
    }

    @Test
    fun onboardedWithEarningsIsLoaded() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.onboardedWithEarnings()
            val vm = vm(h)
            assertEquals(PayoutsLoad.Loading, vm.uiState.value.load)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(PayoutsLoad.Loaded, state.load)
            val content = state.content as PayoutsContent.Loaded
            assertTrue(content.eligible)
            assertEquals(12_500L, content.summary.availableAmount)
            assertEquals(3_000L, content.summary.pendingAmount)
            assertEquals(1, h.api.connectStatusCalls)
            assertEquals(1, h.api.payoutSummaryCalls)
            assertEquals(1, h.api.expertBookingsCalls)
        }

    private fun SignupTestHarness.noAccount() {
        api.connectStatusHandler = { throw api.httpError(404, """{"status":"error","message":"No account"}""") }
        api.payoutSummaryHandler =
            { throw api.httpError(400, """{"status":"error","code":"NO_PAYOUT_METHOD","message":"Set up payouts"}""") }
    }

    @Test
    fun noAccountIsSetupWithOnboardLink() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.noAccount()
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutsContent.SetupRequired(useRefreshLink = false), vm.uiState.value.content)

            val due = listOf("external_account")
            h.api.connectStatusHandler = { connectStatus(chargesEnabled = false, currentlyDue = due) }
            vm.retry()
            runCurrent()
            assertEquals(PayoutsContent.SetupRequired(useRefreshLink = true), vm.uiState.value.content)
        }

    @Test
    fun bothFailHardIsFailed() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            h.api.payoutSummaryHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutsLoad.Failed(FieldError.Text("Down")), vm.uiState.value.load)

            h.onboardedWithEarnings()
            vm.retry()
            runCurrent()
            assertEquals(PayoutsLoad.Loaded, vm.uiState.value.load)
            assertEquals(2, h.api.payoutSummaryCalls)
        }

    @Test
    fun resumeReloadsQuietly() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.onboardedWithEarnings()
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutsLoad.Loaded, vm.uiState.value.load)
            vm.onScreenHidden()

            val gate = CompletableDeferred<Unit>()
            h.api.payoutSummaryHandler = {
                gate.await()
                payoutSummary(available = 500L)
            }
            vm.onScreenShown()
            runCurrent()
            assertEquals(PayoutsLoad.Loaded, vm.uiState.value.load)
            gate.complete(Unit)
            runCurrent()
            assertEquals(500L, (vm.uiState.value.content as PayoutsContent.Loaded).summary.availableAmount)
            assertEquals(2, h.api.connectStatusCalls)

            // A failing quiet reload keeps what is shown and raises no flag.
            vm.onScreenHidden()
            h.api.payoutSummaryHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            h.api.connectStatusHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            vm.onScreenShown()
            runCurrent()
            val state = vm.uiState.value
            assertEquals(PayoutsLoad.Loaded, state.load)
            assertEquals(500L, (state.content as PayoutsContent.Loaded).summary.availableAmount)
            assertFalse(state.refreshFailed)
            assertFalse(state.isRefreshing)
        }

    @Test
    fun pullRefreshFailureFlags() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.onboardedWithEarnings()
            val vm = vm(h)
            runCurrent()

            h.api.payoutSummaryHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            h.api.connectStatusHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            vm.refresh()
            runCurrent()

            val state = vm.uiState.value
            assertEquals(PayoutsLoad.Loaded, state.load)
            assertTrue(state.content is PayoutsContent.Loaded)
            assertTrue(state.refreshFailed)
            assertFalse(state.isRefreshing)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
            assertNull(vm.uiState.value.onboardingUrl)
        }
}
