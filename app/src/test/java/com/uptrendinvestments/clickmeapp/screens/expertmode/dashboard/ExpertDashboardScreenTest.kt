@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.DASHBOARD_PENDING_BANNER_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.DASHBOARD_RESCHEDULE_BANNER_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.DASHBOARD_SESSION_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.DASHBOARD_VIEW_CALENDAR_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.bookingRequestItem
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** The real Dashboard screen over the real ViewModel and repositories, with only the HTTP layer faked. */
@RunWith(RobolectricTestRunner::class)
class ExpertDashboardScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val tmp = TemporaryFolder()

    @get:Rule(order = 2)
    val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private class Fixture(
        val vm: ExpertDashboardViewModel,
        val callStarter: FakeCallStarter,
        val launch: ExpertLaunchRequest,
        val api: FakeClickMeApi,
    )

    private suspend fun TestScope.fixture(
        requests: List<BookingRequestItem> = emptyList(),
        bookings: List<ExpertBookingItem> = emptyList(),
        now: Instant = NOW,
        bookingsFail: Boolean = false,
    ): Fixture {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.signIn()
        h.api.bookingRequestsHandler = { requests }
        h.api.expertBookingsHandler = {
            if (bookingsFail) {
                throw h.api.httpError(500, """{"status":"error","message":"Server down"}""")
            }
            ExpertBookingsPayload(bookings = bookings)
        }
        val clock = MutableClock(now)
        val callStarter = FakeCallStarter()
        val launch = ExpertLaunchRequest()
        val vm =
            ExpertDashboardViewModel(
                ExpertBookingsRepository(h.api),
                BookingRequestsRepository(h.api),
                h.userManager,
                FakeRealtimeConnection(),
                clock,
                launch,
                callStarter,
                JoinClock(clock, backgroundScope),
            )
        return Fixture(vm, callStarter, launch, h.api)
    }

    private fun setContent(f: Fixture) {
        rule.setContent {
            ClickMeTheme {
                ExpertDashboardScreen(
                    avatarUrl = null,
                    onOpenModeSwitch = { calls += "modeSwitch" },
                    onOpenRequests = { calls += "requests" },
                    onOpenBookingsUpcoming = { calls += "bookingsUpcoming" },
                    onOpenBooking = { calls += "booking:$it" },
                    viewModel = f.vm,
                )
            }
        }
    }

    @Test
    fun greetingAndPendingCountFromTheApi() =
        runTest(main.testDispatcher) {
            val f = fixture(requests = listOf("r1", "r2", "r3").map { bookingRequestItem(it) })
            setContent(f)

            rule.onNodeWithText("Dashboard overview").assertExists()
            rule.onNodeWithText("Hello, Ada").assertExists()
            rule.onNodeWithText("3 Pending Requests").assertExists()
            rule.onNodeWithTag(DASHBOARD_PENDING_BANNER_TAG).performClick()

            assertEquals(listOf("requests"), calls)
        }

    @Test
    fun onePendingIsSingular() =
        runTest(main.testDispatcher) {
            val f = fixture(requests = listOf(bookingRequestItem("r1")))
            setContent(f)

            rule.onNodeWithText("1 Pending Request").assertExists()
        }

    @Test
    fun zeroPendingBannerStaysTappable() =
        runTest(main.testDispatcher) {
            val f = fixture()
            setContent(f)

            rule.onNodeWithText("No pending requests").assertExists()
            rule.onNodeWithText("New bookings will show up here").assertExists()
            rule.onNodeWithTag(DASHBOARD_PENDING_BANNER_TAG).performClick()

            assertEquals(listOf("requests"), calls)
        }

    private fun clientProposed(
        id: String,
        start: Instant,
    ) = expertBookingItem(
        id = id,
        statusCode = "pending_reschedule",
        start = start,
        reschedule = RescheduleDto(initiatedBy = RESCHEDULE_INITIATED_BY_CLIENT),
    )

    @Test
    fun todaysSessionCardWithJoin() =
        runTest(main.testDispatcher) {
            val f = fixture(bookings = listOf(expertBookingItem("b1", start = NOW.plusSeconds(20 * 60))))
            setContent(f)

            rule.onNodeWithText("Today's Sessions").assertExists()
            rule.onNodeWithText("Ada Lovelace").assertExists()
            rule.onNodeWithText("Algorithms").assertExists()
            rule.onNodeWithText("Starting now").assertExists()
            rule.onNodeWithTag(DASHBOARD_SESSION_JOIN_TAG).assertIsEnabled()
            rule
                .onNodeWithTag(DASHBOARD_SESSION_JOIN_TAG)
                .performScrollTo()
                .performClick()
            val started = f.callStarter.requests.single()
            assertEquals("c1", started.peerId)
            assertTrue(calls.isEmpty())

            rule.onNodeWithText("Ada Lovelace").performClick()
            assertEquals(listOf("booking:b1"), calls)
        }

    @Test
    fun noSessionsShowsEmptyCard() =
        runTest(main.testDispatcher) {
            val f = fixture()
            setContent(f)

            rule.onNodeWithText("No sessions today").assertExists()
            rule.onNodeWithText("Enjoy the breather — new bookings will show up here.").assertExists()
        }

    @Test
    fun rescheduleBannerOnlyWhenClientProposed() =
        runTest(main.testDispatcher) {
            val later = NOW.plusSeconds(2 * 86_400)
            val f = fixture(bookings = listOf(clientProposed("b1", later), clientProposed("b2", later)))
            setContent(f)

            rule.onNodeWithText("2 Reschedule Requests").assertExists()
            rule.onNodeWithTag(DASHBOARD_RESCHEDULE_BANNER_TAG).performClick()
            assertEquals(listOf("bookingsUpcoming"), calls)
            assertEquals(BookingsLaunch(BookingsTab.Upcoming), f.launch.bookings.pending.value)

            rule.onNodeWithTag(DASHBOARD_VIEW_CALENDAR_TAG).performClick()
            assertEquals(listOf("bookingsUpcoming", "bookingsUpcoming"), calls)
        }

    @Test
    fun rescheduleBannerHiddenWhenTheExpertProposedIt() =
        runTest(main.testDispatcher) {
            val expertProposed =
                expertBookingItem(
                    id = "b1",
                    statusCode = "pending_reschedule",
                    start = NOW.plusSeconds(2 * 86_400),
                    reschedule = RescheduleDto(initiatedBy = "expert"),
                )
            val f = fixture(bookings = listOf(expertProposed))
            setContent(f)

            rule.onNodeWithTag(DASHBOARD_RESCHEDULE_BANNER_TAG).assertDoesNotExist()
        }

    @Test
    fun hardFailureShowsTheErrorStateAndRetryRecovers() =
        runTest(main.testDispatcher) {
            val f = fixture(bookingsFail = true)
            setContent(f)

            rule.onNodeWithText("Couldn't load dashboard").assertExists()
            rule.onNodeWithText("Server down").assertExists()

            f.api.expertBookingsHandler = { ExpertBookingsPayload() }
            rule.onNodeWithText("Retry").performClick()

            rule.onNodeWithText("Hello, Ada").assertExists()
        }

    private companion object {
        val NOW: Instant = Instant.parse("2026-11-02T15:00:00Z")
    }
}
