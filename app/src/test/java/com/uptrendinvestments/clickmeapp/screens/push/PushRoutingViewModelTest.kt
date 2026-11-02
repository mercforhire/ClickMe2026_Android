package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.shownRootRoute
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClientBookingProbe
import com.uptrendinvestments.clickmeapp.testing.FakePendingRequestLookup
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushRoutingViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val launchRequest = PushLaunchRequest()
    private val shellRoutes = ShellRouteRequest()
    private val chatLaunch = ChatLaunchRequest()
    private val probe = FakeClientBookingProbe { ClientBookingProbeResult.Client("completed") }
    private val callGuard = FakeCallGuard()
    private var expertRole = true

    private fun viewModel() =
        PushRoutingViewModel(
            launchRequest = launchRequest,
            shellRoutes = shellRoutes,
            chatLaunch = chatLaunch,
            resolver = PushDestinationResolver(probe, FakePendingRequestLookup()),
            roles = PushRoleSource { expertRole },
            callGuard = callGuard,
        )

    private fun clientSummary(id: String = "B1") =
        ShellRoute(AppMode.Client, ClientBookingsRoot, listOf(BookingSummaryKey(id)))

    private fun expertSummary(id: String = "B1") =
        ShellRoute(
            AppMode.Expert,
            ExpertBookingsRoot,
            listOf(ExpertBookingSummaryKey(id, ExpertBookingOrigin.Bookings)),
        )

    @Test
    fun waitsOnSplashThenDeliversOnClientHome() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.Splash)
            val tap = PushDestination.BookingDetail("B1", PushAudience.Client)
            launchRequest.destination.post(tap)
            advanceUntilIdle()
            assertNull(shellRoutes.pending.value)
            assertEquals(tap, launchRequest.destination.pending.value)

            vm.onRouteChanged(AppRoute.ClientHome)
            advanceUntilIdle()

            assertEquals(clientSummary(), shellRoutes.pending.value)
            assertNull(launchRequest.destination.pending.value)
        }

    @Test
    fun warmTapOnHomeDeliversAtOnce() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ExpertHome)
            advanceUntilIdle()

            launchRequest.destination.post(PushDestination.BookingDetail("B1", PushAudience.Expert))
            advanceUntilIdle()

            assertEquals(expertSummary(), shellRoutes.pending.value)
            assertNull(vm.switchRequest.value)
        }

    @Test
    fun switchesModeThenDelivers() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ClientHome)
            launchRequest.destination.post(PushDestination.BookingDetail("B1", PushAudience.Expert))
            runCurrent()
            assertEquals(AppMode.Expert, vm.switchRequest.value)
            assertNull(shellRoutes.pending.value)

            vm.onRouteChanged(AppRoute.ExpertHome)
            runCurrent()

            assertEquals(expertSummary(), shellRoutes.pending.value)
            assertNull(vm.switchRequest.value)
        }

    @Test
    fun switchTimeoutDrops() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ClientHome)
            launchRequest.destination.post(PushDestination.BookingDetail("B1", PushAudience.Expert))
            runCurrent()
            assertEquals(AppMode.Expert, vm.switchRequest.value)

            advanceTimeBy(PushRoutingViewModel.SWITCH_TIMEOUT_MS + 1)
            runCurrent()

            assertNull(shellRoutes.pending.value)
            assertNull(vm.switchRequest.value)
        }

    @Test
    fun switchLandingOnSignupSetupDrops() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ClientHome)
            launchRequest.destination.post(PushDestination.BookingDetail("B1", PushAudience.Expert))
            runCurrent()

            vm.onRouteChanged(AppRoute.SignupSetup)
            runCurrent()

            assertNull(shellRoutes.pending.value)
            assertNull(vm.switchRequest.value)
        }

    @Test
    fun chatRoutesThroughChatLaunch() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ClientHome)
            launchRequest.destination.post(PushDestination.ChatThread("T1"))
            advanceUntilIdle()

            assertEquals(ChatThreadKey("T1"), chatLaunch.thread.pending.value)
            assertNull(shellRoutes.pending.value)
            assertNull(vm.switchRequest.value)
        }

    private fun clientTap(id: String) = PushDestination.BookingDetail(id, PushAudience.Client)

    @Test
    fun loginDropsPendingTap() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.Splash)
            launchRequest.destination.post(clientTap("B1"))
            runCurrent()

            vm.onRouteChanged(AppRoute.Login)
            runCurrent()
            assertNull(launchRequest.destination.pending.value)

            vm.onRouteChanged(AppRoute.ClientHome)
            advanceUntilIdle()
            assertNull(shellRoutes.pending.value)
        }

    @Test
    fun signupSetupDropsPendingTap() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.Splash)
            launchRequest.destination.post(clientTap("B1"))
            runCurrent()

            vm.onRouteChanged(AppRoute.SignupSetup)
            runCurrent()
            assertNull(launchRequest.destination.pending.value)

            vm.onRouteChanged(AppRoute.ClientHome)
            advanceUntilIdle()
            assertNull(shellRoutes.pending.value)
        }

    @Test
    fun newestTapWinsBeforeHome() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.Splash)
            launchRequest.destination.post(clientTap("A"))
            launchRequest.destination.post(clientTap("B"))
            runCurrent()

            vm.onRouteChanged(AppRoute.ClientHome)
            advanceUntilIdle()

            assertEquals(clientSummary("B"), shellRoutes.pending.value)
        }

    @Test
    fun newerTapCancelsSlowResolution() =
        runTest(mainRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            probe.result = { id ->
                if (id == "A") gate.await()
                ClientBookingProbeResult.Client("completed")
            }
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ClientHome)
            launchRequest.destination.post(clientTap("A"))
            runCurrent()
            assertEquals(listOf("A"), probe.probed)

            launchRequest.destination.post(clientTap("B"))
            runCurrent()
            assertEquals(clientSummary("B"), shellRoutes.pending.value)

            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(clientSummary("B"), shellRoutes.pending.value)
        }

    @Test
    fun logoutDuringResolutionDrops() =
        runTest(mainRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            probe.result = {
                gate.await()
                ClientBookingProbeResult.Client("completed")
            }
            val vm = viewModel()
            vm.onRouteChanged(AppRoute.ClientHome)
            launchRequest.destination.post(clientTap("A"))
            runCurrent()
            assertEquals(listOf("A"), probe.probed)

            vm.onRouteChanged(AppRoute.Login)
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            assertNull(shellRoutes.pending.value)
            assertNull(chatLaunch.thread.pending.value)
        }

    // ---- Phase 16.5: the 3 s splash hold shows Splash to the router (D-09) ----

    @Test
    fun aPushPostedDuringTheHoldWaitsForTheShownRoute() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(shownRootRoute(AppRoute.ClientHome, held = true))
            val tap = PushDestination.BookingDetail("B1", PushAudience.Client)
            launchRequest.destination.post(tap)
            advanceUntilIdle()
            assertNull(shellRoutes.pending.value)
            assertEquals(tap, launchRequest.destination.pending.value)

            vm.onRouteChanged(shownRootRoute(AppRoute.ClientHome, held = false))
            advanceUntilIdle()

            assertEquals(clientSummary(), shellRoutes.pending.value)
            assertNull(launchRequest.destination.pending.value)
        }

    @Test
    fun aResumedCallDestinationWaitsForTheShownRoute() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(shownRootRoute(AppRoute.ClientHome, held = true))
            val resumed = PushDestination.BookingDetail("B1", PushAudience.Either)
            launchRequest.destination.postIfAbsent(resumed)
            advanceUntilIdle()
            assertNull(shellRoutes.pending.value)
            assertEquals(resumed, launchRequest.destination.pending.value)

            vm.onRouteChanged(shownRootRoute(AppRoute.ClientHome, held = false))
            advanceUntilIdle()

            assertEquals(clientSummary(), shellRoutes.pending.value)
            assertNull(launchRequest.destination.pending.value)
        }

    @Test
    fun aPushDuringTheHoldDropsOnlyWhenTheShownRouteReachesLogin() =
        runTest(mainRule.testDispatcher) {
            val vm = viewModel()
            vm.onRouteChanged(shownRootRoute(AppRoute.Login, held = true))
            val tap = PushDestination.BookingDetail("B1", PushAudience.Client)
            launchRequest.destination.post(tap)
            advanceUntilIdle()
            assertEquals(tap, launchRequest.destination.pending.value)

            vm.onRouteChanged(shownRootRoute(AppRoute.Login, held = false))
            advanceUntilIdle()

            assertNull(launchRequest.destination.pending.value)
            assertNull(shellRoutes.pending.value)
        }
}
