@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.ConnectLinkData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.screens.expertmode.ConnectReturn
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.connectStatus
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.testing.payoutSummary
import com.uptrendinvestments.clickmeapp.testing.transactionsPage
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
    private val launch = ExpertLaunchRequest()

    private suspend fun TestScope.harness(): SignupTestHarness {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.signIn()
        h.api.expertBookingsHandler = { ExpertBookingsPayload() }
        h.api.earningsTransactionsHandler = { _, _ -> transactionsPage(emptyList()) }
        return h
    }

    private fun vm(h: SignupTestHarness) =
        PayoutsViewModel(ExpertFinancialRepository(h.api), ExpertBookingsRepository(h.api), guard, launch)

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

    private fun SignupTestHarness.incompleteAccount() {
        api.connectStatusHandler =
            { connectStatus(chargesEnabled = false, currentlyDue = listOf("external_account")) }
    }

    @Test
    fun setupMintsRefreshLinkWhenAccountExists() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.incompleteAccount()
            h.api.connectRefreshLinkHandler = { ConnectLinkData("https://connect.stripe.com/setup/abc") }
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutsContent.SetupRequired(useRefreshLink = true), vm.uiState.value.content)

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(listOf(RestrictedAction.SetUpPayouts), guard.attempts)
            assertEquals(1, h.api.connectRefreshLinkCalls)
            assertEquals(0, h.api.connectOnboardCalls)
            assertEquals("https://connect.stripe.com/setup/abc", vm.uiState.value.onboardingUrl)
            assertFalse(vm.uiState.value.isMintingLink)

            vm.consumeOnboardingUrl()
            assertNull(vm.uiState.value.onboardingUrl)
        }

    @Test
    fun setupMintsOnboardWhenNoAccount() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.noAccount()
            h.api.connectOnboardHandler = { ConnectLinkData("https://connect.stripe.com/setup/new") }
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(1, h.api.connectOnboardCalls)
            assertEquals(0, h.api.connectRefreshLinkCalls)
            assertEquals("https://connect.stripe.com/setup/new", vm.uiState.value.onboardingUrl)
        }

    @Test
    fun repeatTapsAndGuard() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.incompleteAccount()
            val gate = CompletableDeferred<Unit>()
            h.api.connectRefreshLinkHandler = {
                gate.await()
                ConnectLinkData("https://connect.stripe.com/setup/abc")
            }
            val vm = vm(h)
            runCurrent()

            repeat(3) { vm.startPayoutSetup() }
            runCurrent()
            assertTrue(vm.uiState.value.isMintingLink)
            assertEquals(1, h.api.connectRefreshLinkCalls)
            gate.complete(Unit)
            runCurrent()
            vm.startPayoutSetup()
            runCurrent()
            assertEquals(1, h.api.connectRefreshLinkCalls)
            assertEquals(1, guard.attempts.size)
        }

    @Test
    fun blockedGuardCallsNothing() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.incompleteAccount()
            guard.blocked = true
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(listOf(RestrictedAction.SetUpPayouts), guard.attempts)
            assertEquals(0, h.api.connectRefreshLinkCalls + h.api.connectOnboardCalls)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun disallowedUrlIsAnError() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.incompleteAccount()
            val vm = vm(h)
            runCurrent()

            for (bad in listOf("http://connect.stripe.com/x", "https://", "javascript:alert(1)")) {
                h.api.connectRefreshLinkHandler = { ConnectLinkData(bad) }
                vm.startPayoutSetup()
                runCurrent()

                assertNull(bad, vm.uiState.value.onboardingUrl)
                assertEquals(FieldError.Res(R.string.expert_error_generic), vm.uiState.value.onboardingError)
                vm.dismissOnboardingError()
            }
            assertNull(vm.uiState.value.onboardingError)
        }

    @Test
    fun notRatifiedShowsServerMessage() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.noAccount()
            h.api.connectOnboardHandler = {
                throw h.api.httpError(
                    403,
                    """{"status":"error","code":"CONNECT_NOT_RATIFIED","message":"Payouts are not open yet."}""",
                )
            }
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(FieldError.Text("Payouts are not open yet."), vm.uiState.value.onboardingError)
            assertNull(vm.uiState.value.onboardingUrl)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun nothingToMintWhenOnboarded() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.onboardedWithEarnings()
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertTrue(guard.attempts.isEmpty())
            assertEquals(0, h.api.connectRefreshLinkCalls + h.api.connectOnboardCalls)
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

    private fun SignupTestHarness.ownLinkSetup(): () -> Int {
        incompleteAccount()
        var minted = 0
        api.connectRefreshLinkHandler = { ConnectLinkData("https://connect.stripe.com/setup/${++minted}") }
        return { minted }
    }

    @Test
    fun returnReloadsQuietly() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.incompleteAccount()
            val vm = vm(h)
            runCurrent()
            assertTrue(vm.uiState.value.content is PayoutsContent.SetupRequired)
            assertEquals(1, h.api.connectStatusCalls)
            assertEquals(1, h.api.payoutSummaryCalls)

            val gate = CompletableDeferred<Unit>()
            h.api.connectStatusHandler = {
                gate.await()
                connectStatus()
            }
            h.api.payoutSummaryHandler = { payoutSummary(available = 12_500L) }
            launch.connectReturn.post(ConnectReturn.Done)
            runCurrent()
            assertEquals(PayoutsLoad.Loaded, vm.uiState.value.load)
            gate.complete(Unit)
            runCurrent()

            assertEquals(2, h.api.connectStatusCalls)
            assertEquals(2, h.api.payoutSummaryCalls)
            assertTrue(vm.uiState.value.content is PayoutsContent.Loaded)
            assertNull(launch.connectReturn.pending.value)
        }

    @Test
    fun returnNeverAssumesSuccess() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.incompleteAccount()
            val vm = vm(h)
            runCurrent()

            launch.connectReturn.post(ConnectReturn.Done)
            runCurrent()

            assertEquals(2, h.api.connectStatusCalls)
            assertEquals(PayoutsContent.SetupRequired(useRefreshLink = true), vm.uiState.value.content)
        }

    @Test
    fun refreshAfterOwnLinkReopens() =
        runTest(main.testDispatcher) {
            val h = harness()
            val minted = h.ownLinkSetup()
            val vm = vm(h)
            runCurrent()
            vm.startPayoutSetup()
            runCurrent()
            assertEquals("https://connect.stripe.com/setup/1", vm.uiState.value.onboardingUrl)
            vm.consumeOnboardingUrl()

            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()

            assertEquals(2, minted())
            assertEquals(2, h.api.connectRefreshLinkCalls)
            assertEquals(0, h.api.connectOnboardCalls)
            assertEquals(listOf(RestrictedAction.SetUpPayouts, RestrictedAction.SetUpPayouts), guard.attempts)
            assertEquals("https://connect.stripe.com/setup/2", vm.uiState.value.onboardingUrl)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun strayRefreshAfterDoneMintsNothing() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.ownLinkSetup()
            val vm = vm(h)
            runCurrent()
            vm.startPayoutSetup()
            runCurrent()
            vm.consumeOnboardingUrl()
            assertEquals(1, h.api.connectRefreshLinkCalls)

            launch.connectReturn.post(ConnectReturn.Done)
            runCurrent()
            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()

            assertEquals(1, h.api.connectRefreshLinkCalls)
            assertNull(vm.uiState.value.onboardingUrl)
        }

    @Test
    fun refreshWithNoOutstandingLinkAfterReMintsMintsNothing() =
        runTest(main.testDispatcher) {
            val h = harness()
            val minted = h.ownLinkSetup()
            val vm = vm(h)
            runCurrent()
            vm.startPayoutSetup()
            runCurrent()
            vm.consumeOnboardingUrl()

            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()
            assertEquals(2, minted())
            vm.consumeOnboardingUrl()

            // The re-minted link was opened (flag set again), so a further refresh may re-mint once more ...
            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()
            assertEquals(3, minted())
            vm.consumeOnboardingUrl()
            // ... but a refresh that arrives while no link is outstanding does not.
            launch.connectReturn.post(ConnectReturn.Done)
            runCurrent()
            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()
            assertEquals(3, minted())
        }

    @Test
    fun refreshForAnOnboardedExpertMintsNothing() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.ownLinkSetup()
            val vm = vm(h)
            runCurrent()
            vm.startPayoutSetup()
            runCurrent()
            vm.consumeOnboardingUrl()
            assertEquals(1, h.api.connectRefreshLinkCalls)

            // The account finishes onboarding in the browser; the next load sees it.
            h.onboardedWithEarnings()
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.content is PayoutsContent.Loaded)

            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()

            assertEquals(1, h.api.connectRefreshLinkCalls)
            assertNull(vm.uiState.value.onboardingUrl)
        }

    @Test
    fun foreignRefreshOnlyReloads() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.ownLinkSetup()
            val vm = vm(h)
            runCurrent()

            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()

            assertEquals(0, h.api.connectRefreshLinkCalls + h.api.connectOnboardCalls)
            assertTrue(guard.attempts.isEmpty())
            assertEquals(2, h.api.connectStatusCalls)
            assertNull(vm.uiState.value.onboardingUrl)
            assertEquals(PayoutsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun stalePendingIsDropped() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.ownLinkSetup()
            launch.connectReturn.post(ConnectReturn.LinkExpired)
            val vm = vm(h)
            runCurrent()

            assertNull(launch.connectReturn.pending.value)
            assertEquals(1, h.api.connectStatusCalls)
            assertEquals(1, h.api.payoutSummaryCalls)
            assertEquals(0, h.api.connectRefreshLinkCalls + h.api.connectOnboardCalls)
            assertNull(vm.uiState.value.onboardingUrl)
        }

    @Test
    fun blockedGuardOnRefreshDoesNothing() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.ownLinkSetup()
            val vm = vm(h)
            runCurrent()
            vm.startPayoutSetup()
            runCurrent()
            vm.consumeOnboardingUrl()
            assertEquals(1, h.api.connectRefreshLinkCalls)

            guard.blocked = true
            launch.connectReturn.post(ConnectReturn.LinkExpired)
            runCurrent()

            assertEquals(1, h.api.connectRefreshLinkCalls)
            assertEquals(2, guard.attempts.size)
            assertNull(vm.uiState.value.onboardingUrl)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun loadFetchesFirstLedgerPage() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.onboardedWithEarnings()
            h.api.earningsTransactionsHandler =
                { _, _ -> transactionsPage((1..7).map { earningsTransaction("e$it") }, hasMore = true) }
            val vm = vm(h)
            runCurrent()

            assertEquals(listOf(1 to TRANSACTIONS_PAGE_SIZE), h.api.earningsTransactionsQueries)
            assertEquals(7, vm.uiState.value.recentRows.size)
            assertTrue(vm.uiState.value.hasMoreActivity)

            vm.retry()
            runCurrent()
            assertEquals(2, h.api.earningsTransactionsCalls)
        }

    @Test
    fun failingLedgerLeavesContentLoaded() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.onboardedWithEarnings()
            h.api.earningsTransactionsHandler = { _, _ ->
                throw h.api.httpError(500, """{"status":"error","message":"Down"}""")
            }
            val vm = vm(h)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(PayoutsLoad.Loaded, state.load)
            assertTrue(state.content is PayoutsContent.Loaded)
            assertTrue(state.recentRows.isEmpty())
            assertFalse(state.hasMoreActivity)
        }
}
