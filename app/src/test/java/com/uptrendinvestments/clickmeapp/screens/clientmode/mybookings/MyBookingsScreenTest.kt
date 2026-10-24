package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MY_BOOKINGS_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsEmptyState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsError
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.PAST_BOOKING_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.PastBookingCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.UPCOMING_BOOKING_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.UpcomingBookingCard
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MyBookingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone: ZoneId = ZoneId.of("America/Toronto")
    private val locale: Locale = Locale.US

    private fun timeFormat() = SimpleDateFormat("h:mm a", Locale.US)

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun existsTag(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private val defaultStart: Instant = Instant.parse("2026-10-24T18:00:00Z")

    // 2026-10-24T18:00Z is 2:00 PM to 3:00 PM in Toronto (EDT).
    private val dateText = "Oct 24, 2026"
    private val timeRange = "2:00 PM – 3:00 PM"

    /** Time text normalised for the narrow no-break space newer JDKs put before AM / PM. */
    private fun anyTextContains(fragment: String): Boolean =
        rule
            .onAllNodesWithText(fragment, substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    @Composable
    private fun UpcomingCard(
        item: ClientBookingItem,
        onClick: () -> Unit = {},
        joinEnabled: Boolean = false,
        onJoin: () -> Unit = {},
    ) = UpcomingBookingCard(
        item,
        onClick,
        joinEnabled,
        onJoin,
        zone = zone,
        timeFormat = timeFormat(),
        locale = locale,
    )

    @Composable
    private fun PastCard(
        row: PastRow,
        onClick: () -> Unit = {},
    ) = PastBookingCard(row, onClick, zone = zone, timeFormat = timeFormat(), locale = locale)

    private fun rescheduleItem(initiatedBy: String) =
        bookingItem(status = STATUS_PENDING_RESCHEDULE, reschedule = rescheduleDto(initiatedBy = initiatedBy))

    // Upcoming card

    @Test
    fun upcomingCard_showsExpertTopicDateAndTime() {
        show { UpcomingCard(bookingItem(status = "confirmed")) }
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText(dateText).assertExists()
        assertTrue(anyTextContains("2:00"))
        assertTrue(anyTextContains("3:00"))
    }

    @Test
    fun upcomingCard_outsideTheWindowShowsADisabledJoinWithTheHint() {
        show { UpcomingCard(bookingItem(status = "confirmed"), joinEnabled = false) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsNotEnabled()
        val node = rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNode()
        assertEquals(
            "Available closer to the session start",
            node.config.getOrNull(SemanticsProperties.StateDescription),
        )
    }

    @Test
    fun upcomingCard_insideTheWindowShowsAnEnabledJoinThatDoesNotOpenTheDetails() {
        var joins = 0
        var clicks = 0
        show {
            UpcomingCard(
                bookingItem(status = "confirmed"),
                onClick = { clicks++ },
                joinEnabled = true,
                onJoin = { joins++ },
            )
        }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsEnabled().performClick()
        assertEquals(1, joins)
        assertEquals(0, clicks)
    }

    @Test
    fun upcomingCard_clickCallsOnClickOnce() {
        var clicks = 0
        show { UpcomingCard(bookingItem(), onClick = { clicks++ }) }
        rule.onNodeWithText("Algorithms").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun upcomingCard_pendingApprovalShowsBannerAndNoJoin() {
        show { UpcomingCard(bookingItem(status = STATUS_PENDING_APPROVAL)) }
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        assertTrue(!existsTag(MY_BOOKINGS_JOIN_TAG))
    }

    @Test
    fun upcomingCard_expertRescheduleBadge() {
        show { UpcomingCard(rescheduleItem("expert")) }
        rule.onNodeWithText("Expert proposed a new time · Tap to review").assertExists()
    }

    @Test
    fun upcomingCard_clientRescheduleBadge() {
        show { UpcomingCard(rescheduleItem("client")) }
        rule.onNodeWithText("Reschedule pending expert's response").assertExists()
        assertTrue(!exists("Expert proposed a new time · Tap to review"))
    }

    @Test
    fun upcomingCard_noRescheduleShowsNoBadge() {
        show { UpcomingCard(bookingItem()) }
        assertTrue(!exists("Expert proposed a new time · Tap to review"))
        assertTrue(!exists("Reschedule pending expert's response"))
    }

    @Test
    fun upcomingCard_nullNameAndTopicFallBack() {
        show { UpcomingCard(bookingItem(expertName = null, topic = null)) }
        rule.onNodeWithText("Expert").assertExists()
        rule.onNodeWithText("Consultation").assertExists()
    }

    // Past card

    @Test
    fun pastCard_showsExpertTopicWhenAndBadge() {
        val row = PastRow(bookingItem(status = "declined"), pastStatusDisplay("declined")!!)
        show { PastCard(row) }
        rule.onNodeWithText("Ada Lovelace").assertExists()
        assertTrue(anyTextContains("Topic:"))
        assertTrue(anyTextContains("Algorithms"))
        assertTrue(anyTextContains("$dateText, 2:00"))
        rule.onNodeWithText("Cancelled").assertExists()
    }

    @Test
    fun pastCard_clickCallsOnClick() {
        var clicks = 0
        val row = PastRow(bookingItem(status = "completed"), pastStatusDisplay("completed")!!)
        show { PastCard(row, onClick = { clicks++ }) }
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(1, clicks)
        rule.onNodeWithText("Completed").assertExists()
    }

    // States

    @Test
    fun emptyState_upcomingHasExploreAction() {
        var explored = 0
        show { MyBookingsEmptyState(BookingsTab.Upcoming, onExploreExperts = { explored++ }) }
        rule.onNodeWithText("No Bookings Yet!").assertExists()
        rule.onNodeWithText("Explore Experts").performClick()
        assertEquals(1, explored)
    }

    @Test
    fun emptyState_pastHasNoAction() {
        show { MyBookingsEmptyState(BookingsTab.Past, onExploreExperts = {}) }
        rule.onNodeWithText("No past bookings yet").assertExists()
        rule.onNodeWithText("Completed, cancelled and missed sessions will show up here.").assertExists()
        assertTrue(!exists("Explore Experts"))
    }

    @Test
    fun errorState_showsMessageAndRetry() {
        var retries = 0
        show { MyBookingsError(FieldError.Text("Boom"), onRetry = { retries++ }) }
        rule.onNodeWithText("Couldn't load bookings").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun skeleton_exposesLoadingDescription() {
        show { MyBookingsSkeleton() }
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun cards_exposeTags() {
        show {
            UpcomingCard(bookingItem())
            PastCard(PastRow(bookingItem(id = "b2", status = "missed"), pastStatusDisplay("missed")!!))
        }
        rule.onNodeWithTag(UPCOMING_BOOKING_CARD_TAG).assertExists()
        rule.onNodeWithTag(PAST_BOOKING_CARD_TAG).assertExists()
    }
    // Screen content

    private class Callbacks {
        var selectedTab: BookingsTab? = null
        var refreshes = 0
        var retries = 0
        var openedUpcoming: String? = null
        var openedPast: String? = null
        var explored = 0
        var noticeConsumed = 0
        var refreshFailureConsumed = 0
        val joined = mutableListOf<String>()
    }

    @Composable
    private fun Content(
        state: MyBookingsUiState,
        calls: Callbacks = Callbacks(),
        joinNow: Instant = defaultStart.minusSeconds(600),
    ) = MyBookingsContent(
        state = state,
        onSelectTab = { calls.selectedTab = it },
        onRefresh = { calls.refreshes++ },
        onRetry = { calls.retries++ },
        onOpenUpcoming = { calls.openedUpcoming = it },
        onOpenPast = { calls.openedPast = it },
        onExploreExperts = { calls.explored++ },
        onNoticeConsumed = { calls.noticeConsumed++ },
        onRefreshFailureConsumed = { calls.refreshFailureConsumed++ },
        joinNow = joinNow,
        onJoin = { calls.joined += it.bookingId },
        zone = zone,
        timeFormat = timeFormat(),
        locale = locale,
    )

    private fun pastRow(
        id: String,
        status: String = "completed",
    ) = PastRow(bookingItem(id = id, status = status), pastStatusDisplay(status)!!)

    private fun loaded(
        tab: BookingsTab = BookingsTab.Upcoming,
        upcoming: List<ClientBookingItem> = emptyList(),
        past: List<PastRow> = emptyList(),
        notice: BookingsNotice? = null,
        refreshFailed: Boolean = false,
    ) = MyBookingsUiState(
        load = MyBookingsLoad.Loaded,
        lists = BookingLists(upcoming, past),
        selectedTab = tab,
        notice = notice,
        refreshFailed = refreshFailed,
    )

    @Test
    fun content_showsTitleAndBothTabs() {
        show { Content(loaded()) }
        rule.onNodeWithText("My Bookings").assertExists()
        rule.onNodeWithText("Upcoming").assertExists()
        rule.onNodeWithText("Past").assertExists()
        rule.onNodeWithText("Upcoming").assertIsSelected()
        rule.onNodeWithText("Past").assertIsNotSelected()
    }

    @Test
    fun content_tappingPastSelectsPastTab() {
        val calls = Callbacks()
        show { Content(loaded(), calls) }
        rule.onNodeWithText("Past").performClick()
        assertEquals(BookingsTab.Past, calls.selectedTab)
    }

    @Test
    fun content_loadingShowsSkeletonOnly() {
        show { Content(MyBookingsUiState()) }
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertTrue(!existsTag(UPCOMING_BOOKING_CARD_TAG))
    }

    @Test
    fun content_upcomingTabShowsCardsAndOpensDetails() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1"), bookingItem("b3"))), calls) }
        assertEquals(2, rule.onAllNodesWithTag(UPCOMING_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
        rule.onAllNodesWithTag(UPCOMING_BOOKING_CARD_TAG)[0].performClick()
        assertEquals("b1", calls.openedUpcoming)
    }

    @Test
    fun content_pastTabShowsCardAndOpensSummary() {
        val calls = Callbacks()
        show { Content(loaded(tab = BookingsTab.Past, past = listOf(pastRow("b2"))), calls) }
        assertEquals(1, rule.onAllNodesWithTag(PAST_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
        rule.onNodeWithTag(PAST_BOOKING_CARD_TAG).performClick()
        assertEquals("b2", calls.openedPast)
    }

    @Test
    fun content_emptyUpcomingOffersExplore() {
        val calls = Callbacks()
        show { Content(loaded(), calls) }
        rule.onNodeWithText("No Bookings Yet!").assertExists()
        rule.onNodeWithText("Explore Experts").performClick()
        assertEquals(1, calls.explored)
    }

    @Test
    fun content_emptyPastHasNoAction() {
        show { Content(loaded(tab = BookingsTab.Past)) }
        rule.onNodeWithText("No past bookings yet").assertExists()
        assertTrue(!exists("Explore Experts"))
    }

    @Test
    fun content_failedShowsErrorWithRetry() {
        val calls = Callbacks()
        show { Content(MyBookingsUiState(load = MyBookingsLoad.Failed(FieldError.Text("Boom"))), calls) }
        rule.onNodeWithText("Couldn't load bookings").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, calls.retries)
    }

    @Test
    fun content_refundNoticeShowsSnackbarAndIsConsumedOnce() {
        val calls = Callbacks()
        show { Content(loaded(notice = BookingsNotice.CancelledWithRefund("CA$12.50")), calls) }
        rule.onNodeWithText("Booking cancelled. CA$12.50 will be refunded").assertExists()
        assertEquals(1, calls.noticeConsumed)
    }

    @Test
    fun content_plainNoticeShowsCancelledSnackbar() {
        val calls = Callbacks()
        show { Content(loaded(notice = BookingsNotice.Cancelled), calls) }
        rule.onNodeWithText("Booking cancelled").assertExists()
        assertEquals(1, calls.noticeConsumed)
    }

    @Test
    fun content_refreshFailureShowsSnackbarAndIsConsumed() {
        val calls = Callbacks()
        show { Content(loaded(refreshFailed = true), calls) }
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, calls.refreshFailureConsumed)
    }

    @Test
    fun content_neverOffersExcludedActions() {
        val upcoming = listOf(bookingItem("b1"), bookingItem("b3", status = STATUS_PENDING_APPROVAL))
        val state = loaded(upcoming = upcoming, past = listOf(pastRow("b2")))
        var tab by mutableStateOf(BookingsTab.Upcoming)
        show { Content(state.copy(selectedTab = tab)) }
        val forbidden = listOf("Reschedule", "Message", "Private Note", "Leave a Review")
        forbidden.forEach { assertTrue("$it on Upcoming", !exists(it)) }
        rule.runOnIdle { tab = BookingsTab.Past }
        rule.waitForIdle()
        forbidden.forEach { assertTrue("$it on Past", !exists(it)) }
    }

    // Join in the list (D-01 .. D-03)

    @Test
    fun content_aConfirmedCardTenMinutesBeforeStartHasAnEnabledJoinThatJoinsOnce() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1"))), calls) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsEnabled().performClick()
        assertEquals(listOf("b1"), calls.joined)
        assertEquals(null, calls.openedUpcoming)
    }

    @Test
    fun content_aConfirmedCardThreeHoursBeforeStartHasADisabledJoin() {
        show {
            Content(
                loaded(upcoming = listOf(bookingItem("b1"))),
                joinNow = defaultStart.minusSeconds(3 * 3600),
            )
        }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsNotEnabled()
    }

    @Test
    fun content_aRejoinableCompletedCardHasAnEnabledJoin() {
        val item =
            bookingItem(
                id = "r1",
                status = "completed",
                start = defaultStart.minusSeconds(2 * 3600),
                end = defaultStart.plusSeconds(300),
            )
        show { Content(loaded(upcoming = listOf(item)), joinNow = defaultStart) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsEnabled()
    }

    @Test
    fun content_aPendingApprovalCardShowsTheBannerAndNoJoin() {
        show { Content(loaded(upcoming = listOf(bookingItem("b1", status = STATUS_PENDING_APPROVAL)))) }
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        assertTrue(!existsTag(MY_BOOKINGS_JOIN_TAG))
    }
}
