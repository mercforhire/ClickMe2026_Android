package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClientBookingProbe
import com.uptrendinvestments.clickmeapp.testing.FakePendingRequestLookup
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
        }
}
