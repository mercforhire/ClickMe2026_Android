package com.uptrendinvestments.clickmeapp.screens.push

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.uptrendinvestments.clickmeapp.AppRoute
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClientBookingProbe
import com.uptrendinvestments.clickmeapp.testing.FakePendingRequestLookup
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PushRoutingHostTest {
    private val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(mainRule).around(composeRule)

    @Test
    fun hostForwardsSwitchRequests() {
        val launchRequest = PushLaunchRequest()
        val viewModel =
            PushRoutingViewModel(
                launchRequest = launchRequest,
                shellRoutes = ShellRouteRequest(),
                chatLaunch = ChatLaunchRequest(),
                resolver = PushDestinationResolver(FakeClientBookingProbe(), FakePendingRequestLookup()),
                roles = PushRoleSource { true },
                callGuard = FakeCallGuard(),
            )
        launchRequest.destination.post(PushDestination.BookingDetail("B1", PushAudience.Expert))
        val switches = mutableListOf<AppMode>()

        composeRule.setContent {
            PushRoutingHost(route = AppRoute.ClientHome, onSwitchMode = { switches += it }, viewModel = viewModel)
        }
        composeRule.waitForIdle()

        assertEquals(listOf(AppMode.Expert), switches)
        assertNull(viewModel.switchRequest.value)
    }
}
