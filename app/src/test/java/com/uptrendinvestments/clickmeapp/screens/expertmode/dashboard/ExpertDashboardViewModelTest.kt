@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.ConnectLinkData
import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.DashboardLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.bookingRequestItem
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

class ExpertDashboardViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val realtime = FakeRealtimeConnection()
    private val clock = MutableClock(NOW)
    private val launchRequest = ExpertLaunchRequest()
    private val starter = FakeCallStarter()
    private val guard = FakeCallGuard()

    private fun requests(vararg ids: String): List<BookingRequestItem> = ids.map { bookingRequestItem(it) }

    private suspend fun TestScope.harness(pending: List<BookingRequestItem> = requests("r1")): SignupTestHarness {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.signIn()
        h.api.bookingRequestsHandler = { pending }
        h.api.expertBookingsHandler = { ExpertBookingsPayload() }
        return h
    }

    private fun TestScope.vm(h: SignupTestHarness) =
        ExpertDashboardViewModel(
            ExpertBookingsRepository(h.api),
            BookingRequestsRepository(h.api),
            h.userManager,
            realtime,
            clock,
            launchRequest,
            starter,
            ExpertFinancialRepository(h.api),
            guard,
            JoinClock(clock, backgroundScope),
        )

    private fun serverError(
        h: SignupTestHarness,
        message: String,
    ) = h.api.httpError(500, """{"status":"error","message":"$message"}""")

    @Test
    fun loadsBothFetchesAndDerivesTheState() =
        runTest(main.testDispatcher) {
            val h = harness(pending = requests("r1", "r2"))
            val start = NOW.plusSeconds(1200)
            val future = NOW.plusSeconds(2 * 86_400)
            h.api.expertBookingsHandler = {
                ExpertBookingsPayload(
                    bookings =
                        listOf(
                            expertBookingItem("today", start = start),
                            expertBookingItem(
                                id = "proposed",
                                statusCode = "pending_reschedule",
                                start = future,
                                reschedule = RescheduleDto(initiatedBy = RESCHEDULE_INITIATED_BY_CLIENT),
                            ),
                        ),
                )
            }
            val vm = vm(h)
            assertEquals(DashboardLoad.Loading, vm.uiState.value.load)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(DashboardLoad.Loaded, state.load)
            assertEquals("Ada", state.firstName)
            assertEquals(2, state.pendingCount)
            assertEquals(1, state.rescheduleCount)
            assertEquals(listOf("today"), state.todaysSessions.map { it.bookingId })
            assertEquals(1, h.api.expertBookingsCalls)
            assertEquals(1, h.api.bookingRequestsCalls)
        }

    @Test
    fun bookingsFailureIsHard() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.expertBookingsHandler = { throw serverError(h, "Server down") }
            val vm = vm(h)
            runCurrent()
            assertTrue(vm.uiState.value.load is DashboardLoad.Failed)

            h.api.expertBookingsHandler = { ExpertBookingsPayload() }
            vm.retry()
            runCurrent()
            assertEquals(DashboardLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun requestsFailureIsHard() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.bookingRequestsHandler = { throw serverError(h, "Server down") }
            val vm = vm(h)
            runCurrent()
            assertTrue(vm.uiState.value.load is DashboardLoad.Failed)

            h.api.bookingRequestsHandler = { requests("r1", "r2", "r3") }
            vm.retry()
            runCurrent()
            assertEquals(DashboardLoad.Loaded, vm.uiState.value.load)
            assertEquals(3, vm.uiState.value.pendingCount)
        }

    @Test
    fun realtimeUpdateReloadsQuietly() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()
            val seen = mutableListOf<DashboardLoad>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { seen += it.load } }
            seen.clear()

            h.api.bookingRequestsHandler = { requests("r1", "r2") }
            realtime.emit(RealtimeEvent.BookingUpdate("x9", "cancelled"))
            runCurrent()

            assertEquals(2, vm.uiState.value.pendingCount)
            assertEquals(2, h.api.expertBookingsCalls)
            assertTrue(seen.none { it is DashboardLoad.Loading })
        }

    @Test
    fun aBurstOfRealtimeUpdatesEndsWithOneCompletedLoad() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()

            h.api.bookingRequestsHandler = { requests("r1", "r2", "r3") }
            repeat(3) { realtime.emit(RealtimeEvent.BookingUpdate("b$it", "confirmed")) }
            runCurrent()

            assertEquals(3, vm.uiState.value.pendingCount)
            assertEquals(2, h.api.expertBookingsCalls)
        }

    @Test
    fun screenShownDuringTheFirstLoadDoesNotDoubleLoad() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            vm.onScreenShown()
            runCurrent()

            assertEquals(1, h.api.expertBookingsCalls)
            assertEquals(DashboardLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun screenShownReloadsQuietly() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()
            val seen = mutableListOf<DashboardLoad>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { seen += it.load } }
            seen.clear()

            h.api.bookingRequestsHandler = { requests("r1", "r2") }
            vm.onScreenShown()
            runCurrent()

            assertEquals(2, vm.uiState.value.pendingCount)
            assertEquals(2, h.api.expertBookingsCalls)
            assertTrue(seen.none { it is DashboardLoad.Loading })
        }

    @Test
    fun aFailedQuietReloadKeepsTheDataAndIsSilent() =
        runTest(main.testDispatcher) {
            val h = harness(pending = requests("r1", "r2"))
            val vm = vm(h)
            runCurrent()

            h.api.bookingRequestsHandler = { throw serverError(h, "Server down") }
            vm.onScreenShown()
            runCurrent()

            val state = vm.uiState.value
            assertEquals(DashboardLoad.Loaded, state.load)
            assertEquals(2, state.pendingCount)
            assertFalse(state.refreshFailed)
        }

    // Phase 15 D-05: a realtime refetch failure flags the refresh-failed snackbar, a tab-return reload stays silent.

    @Test
    fun realtimeFailureFlags() =
        runTest(main.testDispatcher) {
            val h = harness(pending = requests("r1", "r2"))
            val vm = vm(h)
            runCurrent()

            h.api.bookingRequestsHandler = { throw serverError(h, "Server down") }
            realtime.emit(RealtimeEvent.BookingUpdate("x9", "cancelled"))
            runCurrent()

            val state = vm.uiState.value
            assertEquals(DashboardLoad.Loaded, state.load)
            assertEquals(2, state.pendingCount)
            assertTrue(state.refreshFailed)
            assertFalse(state.isRefreshing)
        }

    @Test
    fun quietTabReturnFailureIsSilent() =
        runTest(main.testDispatcher) {
            val h = harness(pending = requests("r1", "r2"))
            val vm = vm(h)
            runCurrent()

            h.api.expertBookingsHandler = { throw serverError(h, "Server down") }
            vm.onScreenShown()
            runCurrent()

            assertEquals(DashboardLoad.Loaded, vm.uiState.value.load)
            assertEquals(2, vm.uiState.value.pendingCount)
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun realtimeDuringTheFirstLoadStaysLoud() =
        runTest(main.testDispatcher) {
            val h = harness()
            val gate = CompletableDeferred<Unit>()
            h.api.expertBookingsHandler = {
                gate.await()
                throw serverError(h, "Server down")
            }
            val vm = vm(h)
            runCurrent()
            assertEquals(DashboardLoad.Loading, vm.uiState.value.load)

            realtime.emit(RealtimeEvent.BookingUpdate("x9", "confirmed"))
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertTrue(vm.uiState.value.load is DashboardLoad.Failed)
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun pullFailureFlagsRefreshFailedAndKeepsData() =
        runTest(main.testDispatcher) {
            val h = harness(pending = requests("r1", "r2"))
            val vm = vm(h)
            runCurrent()

            h.api.bookingRequestsHandler = { throw serverError(h, "Server down") }
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(DashboardLoad.Loaded, state.load)
            assertEquals(2, state.pendingCount)
            assertTrue(state.refreshFailed)
            assertFalse(state.isRefreshing)

            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun backToHomeLaunchReloads() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()
            val seen = mutableListOf<DashboardLoad>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { seen += it.load } }
            seen.clear()

            h.api.bookingRequestsHandler = { emptyList() }
            launchRequest.dashboard.post(DashboardLaunch)
            runCurrent()

            assertEquals(0, vm.uiState.value.pendingCount)
            assertEquals(2, h.api.expertBookingsCalls)
            assertNull(launchRequest.dashboard.pending.value)
            assertTrue(seen.none { it is DashboardLoad.Loading })
        }

    @Test
    fun aLaunchPostedBeforeConstructionDoesNotCauseASecondInitialLoad() =
        runTest(main.testDispatcher) {
            val h = harness()
            launchRequest.dashboard.post(DashboardLaunch)
            val vm = vm(h)
            runCurrent()

            assertEquals(1, h.api.expertBookingsCalls)
            assertEquals(DashboardLoad.Loaded, vm.uiState.value.load)
            assertNull(launchRequest.dashboard.pending.value)
        }

    @Test
    fun pullDuringQuietReloadKeepsOneLoadInFlight() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            var inFlight = 0
            var maxInFlight = 0
            h.api.expertBookingsHandler = {
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                try {
                    gate.await()
                    ExpertBookingsPayload()
                } finally {
                    inFlight--
                }
            }

            vm.onScreenShown()
            runCurrent()
            assertEquals(1, inFlight)

            vm.refresh()
            runCurrent()
            assertEquals(1, inFlight)
            assertEquals(1, maxInFlight)

            gate.complete(Unit)
            runCurrent()

            assertEquals(DashboardLoad.Loaded, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(0, inFlight)
        }

    @Test
    fun joinStartsTheCallWithTheClientAsPeer() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()

            vm.join(expertBookingItem("b1", start = NOW.plusSeconds(600)))

            val started = starter.requests.single()
            assertEquals("b1", started.bookingId)
            assertEquals("c1", started.peerId)
            assertEquals("Ada Lovelace", started.peerName)
        }

    @Test
    fun openBookingsUpcomingPostsTheUpcomingHandOff() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()

            vm.openBookingsUpcoming()

            assertEquals(BookingsLaunch(BookingsTab.Upcoming), launchRequest.bookings.pending.value)
        }

    private fun incompleteStatus() =
        ConnectStatusData(chargesEnabled = false, currentlyDue = listOf("external_account"))

    private fun SignupTestHarness.noAccount() {
        api.connectStatusHandler = { throw api.httpError(404, """{"status":"error","message":"No account"}""") }
        api.payoutSummaryHandler = {
            throw api.httpError(400, """{"status":"error","code":"NO_PAYOUT_METHOD","message":"Set up payouts"}""")
        }
    }

    @Test
    fun incompleteAccountMintsRefreshLink() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { incompleteStatus() }
            h.api.connectRefreshLinkHandler = { ConnectLinkData("https://connect.stripe.com/setup/abc") }
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutOnboarding.Required(useRefreshLink = true), vm.uiState.value.onboarding)

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
    fun noAccountMintsOnboard() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.noAccount()
            h.api.connectOnboardHandler = { ConnectLinkData("https://connect.stripe.com/setup/new") }
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutOnboarding.Required(useRefreshLink = false), vm.uiState.value.onboarding)

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(1, h.api.connectOnboardCalls)
            assertEquals(0, h.api.connectRefreshLinkCalls)
            assertEquals("https://connect.stripe.com/setup/new", vm.uiState.value.onboardingUrl)
        }

    @Test
    fun repeatTapsWhileMintingAreIgnored() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { incompleteStatus() }
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

            // The first URL has not been consumed yet, so a further tap is still ignored.
            assertEquals(1, h.api.connectRefreshLinkCalls)
            assertEquals(1, guard.attempts.size)
        }

    @Test
    fun guardBlocksMint() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { incompleteStatus() }
            guard.blocked = true
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(listOf(RestrictedAction.SetUpPayouts), guard.attempts)
            assertEquals(0, h.api.connectRefreshLinkCalls)
            assertEquals(0, h.api.connectOnboardCalls)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun nothingToMintWhenOnboarded() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { ConnectStatusData(chargesEnabled = true) }
            val vm = vm(h)
            runCurrent()
            assertEquals(PayoutOnboarding.Onboarded, vm.uiState.value.onboarding)

            vm.startPayoutSetup()
            runCurrent()

            assertTrue(guard.attempts.isEmpty())
            assertEquals(0, h.api.connectOnboardCalls + h.api.connectRefreshLinkCalls)
        }

    @Test
    fun nonHttpsUrlIsRejected() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { incompleteStatus() }
            val vm = vm(h)
            runCurrent()

            for (bad in listOf("http://connect.stripe.com/x", "javascript:alert(1)")) {
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
    fun mintFailureShowsServerMessage() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.noAccount()
            h.api.connectOnboardHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","code":"INVALID_COUNTRY","message":"Only Canadian accounts are supported."}""",
                )
            }
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertEquals(FieldError.Text("Only Canadian accounts are supported."), vm.uiState.value.onboardingError)
            assertNull(vm.uiState.value.onboardingUrl)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun suspendedAccountShowsNoDialog() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { incompleteStatus() }
            h.api.connectRefreshLinkHandler = {
                throw h.api.httpError(
                    403,
                    """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"This account has been suspended."}""",
                )
            }
            val vm = vm(h)
            runCurrent()

            vm.startPayoutSetup()
            runCurrent()

            assertNull(vm.uiState.value.onboardingError)
            assertFalse(vm.uiState.value.isMintingLink)
        }

    @Test
    fun statusRefetchedOnResume() =
        runTest(main.testDispatcher) {
            val h = harness()
            h.api.connectStatusHandler = { incompleteStatus() }
            val vm = vm(h)
            runCurrent()
            assertEquals(1, h.api.connectStatusCalls)
            assertTrue(vm.uiState.value.onboarding is PayoutOnboarding.Required)

            h.api.connectStatusHandler = { ConnectStatusData(chargesEnabled = true) }
            vm.onScreenShown()
            runCurrent()

            assertEquals(2, h.api.connectStatusCalls)
            assertEquals(PayoutOnboarding.Onboarded, vm.uiState.value.onboarding)
        }

    @Test
    fun financialFailuresNeverFailTheLoad() =
        runTest(main.testDispatcher) {
            val h = harness()
            val vm = vm(h)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(DashboardLoad.Loaded, state.load)
            assertNull(state.weekly)
            assertNull(state.payoutSummary)
            assertEquals(PayoutOnboarding.Unknown, state.onboarding)
        }

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-02T15:00:00Z")
    }
}
