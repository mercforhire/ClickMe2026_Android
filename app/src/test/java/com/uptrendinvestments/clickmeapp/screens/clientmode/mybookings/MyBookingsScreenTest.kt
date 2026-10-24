package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
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
    ) = UpcomingBookingCard(item, onClick, zone = zone, timeFormat = timeFormat(), locale = locale)

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
    fun upcomingCard_confirmedShowsDisabledJoinPlaceholder() {
        show { UpcomingCard(bookingItem(status = "confirmed")) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsNotEnabled()
        val node = rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNode()
        assertEquals(
            "Available closer to the session start",
            node.config.getOrNull(SemanticsProperties.StateDescription),
        )
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
}
