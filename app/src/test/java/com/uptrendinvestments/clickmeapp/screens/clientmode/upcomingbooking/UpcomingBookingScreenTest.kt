package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.StatusTone
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.detailStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UPCOMING_BOOKING_COPY_LINK_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UPCOMING_BOOKING_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UPCOMING_BOOKING_PAYMENT_PILL_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UPCOMING_BOOKING_STATUS_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingDateTimeCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingJoinCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingMessageCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingPaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingPendingBanner
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingStatusRow
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingTimezoneNote
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingTopicCard
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class UpcomingBookingScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    // ---- Task 1: subviews ----

    @Test
    fun statusRowShowsPendingApprovalAndId() {
        show { UpcomingBookingStatusRow(detailStatusDisplay("pending_approval"), "#B1") }
        rule.onNodeWithTag(UPCOMING_BOOKING_STATUS_TAG).assertExists()
        rule.onNodeWithText("Pending approval").assertExists()
        rule.onNodeWithText("ID: #B1").assertExists()
    }

    @Test
    fun statusRowShowsConfirmed() {
        show { UpcomingBookingStatusRow(detailStatusDisplay("confirmed"), "#B1") }
        rule.onNodeWithText("Confirmed").assertExists()
    }

    @Test
    fun statusRowHumanizesAnUnknownStatus() {
        show {
            UpcomingBookingStatusRow(
                BookingStatusDisplay(BookingStatusLabel.Other, StatusTone.Neutral, "no_show_both"),
                "#B1",
            )
        }
        rule.onNodeWithText("No show both").assertExists()
    }

    @Test
    fun paymentPillShowsEachMoneyState() {
        var pill by androidx.compose.runtime.mutableStateOf(PaymentPill.HoldPlaced)
        show { UpcomingBookingPaymentPill(pill) }
        rule.onNodeWithTag(UPCOMING_BOOKING_PAYMENT_PILL_TAG).assertExists()
        rule.onNodeWithText("Card hold placed").assertExists()
        pill = PaymentPill.Paid
        rule.onNodeWithText("Paid").assertExists()
        pill = PaymentPill.Refunded
        rule.onNodeWithText("Refunded").assertExists()
    }

    @Test
    fun topicCardShowsFreeFee() {
        show { UpcomingBookingTopicCard("Algorithms", PriceDisplay.Free) }
        rule.onNodeWithText("Session topic").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Session fee").assertExists()
        rule.onNodeWithText("Free").assertExists()
    }

    @Test
    fun topicCardShowsMoneyFee() {
        val price = PriceDisplay.of(false, 5000, "CAD")
        val text = (price as PriceDisplay.Paid).text
        show { UpcomingBookingTopicCard("Algorithms", price) }
        rule.onNodeWithText(text).assertExists()
    }

    @Test
    fun dateTimeCardShowsDateAndRange() {
        val start = Instant.parse("2026-10-24T18:00:00Z")
        val end = Instant.parse("2026-10-24T19:00:00Z")
        show {
            UpcomingBookingDateTimeCard(start, end, zone, SimpleDateFormat("h:mm a", Locale.US), Locale.US)
        }
        rule.onNodeWithText("Date").assertExists()
        rule.onNodeWithText(formatShortDate(LocalDate.of(2026, 10, 24), Locale.US)).assertExists()
        rule.onNodeWithText("Time").assertExists()
        rule.onNodeWithText("2:00 PM – 3:00 PM").assertExists()
    }

    @Test
    fun timezoneNoteNamesTheCityAndAbbreviation() {
        show { UpcomingBookingTimezoneNote("America/Toronto", LocalDate.of(2026, 10, 24), Locale.US) }
        rule.onNodeWithText("Expert timezone: Toronto (EDT)").assertExists()
    }

    @Test
    fun timezoneNoteRendersNothingForNullBlankOrInvalid() {
        var zoneId by androidx.compose.runtime.mutableStateOf<String?>(null)
        show { UpcomingBookingTimezoneNote(zoneId, LocalDate.of(2026, 10, 24), Locale.US) }
        rule.onAllNodesWithText("Expert timezone", substring = true).assertCountEquals(0)
        zoneId = "  "
        rule.onAllNodesWithText("Expert timezone", substring = true).assertCountEquals(0)
        zoneId = "Not/AZone"
        rule.onAllNodesWithText("Expert timezone", substring = true).assertCountEquals(0)
    }

    private fun hasJoinHint() =
        SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Available closer to the session start",
        )

    @Test
    fun inAppVoiceShowsADisabledJoinCall() {
        show { UpcomingBookingJoinCard("in_app_voice", null) }
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).assertIsNotEnabled().assert(hasJoinHint())
        rule.onNodeWithText("Join Call").assertExists()
    }

    @Test
    fun anUnknownMeetingTypeFallsBackToTheDisabledJoinCall() {
        show { UpcomingBookingJoinCard(null, null) }
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).assertIsNotEnabled().assert(hasJoinHint())
    }

    @Test
    fun skypeZoomShowsTheLinkAndAnEnabledCopyButton() {
        show { UpcomingBookingJoinCard("skype_zoom", "https://zoom.us/j/1") }
        rule.onNodeWithText("Meeting link").assertExists()
        rule.onNodeWithText("https://zoom.us/j/1").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG).assertIsEnabled()
        rule.onNodeWithContentDescription("Copy meeting link").assertExists()
        rule.onAllNodesWithText("Join Call").assertCountEquals(0)
    }

    @Test
    fun copyingTheLinkShowsTheCopiedState() {
        show { UpcomingBookingJoinCard("skype_zoom", "https://zoom.us/j/1") }
        rule.onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Copied").assertExists()
    }

    @Test
    fun skypeZoomWithoutALinkShowsNotAvailableAndDisablesCopy() {
        show { UpcomingBookingJoinCard("skype_zoom", null) }
        rule.onNodeWithText("Not available yet").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG).assertIsNotEnabled()
    }

    @Test
    fun pendingBannerNamesTheExpert() {
        show { UpcomingBookingPendingBanner("Ada Lovelace") }
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        rule.onNodeWithText(
            "You'll get a notification once Ada Lovelace confirms this session. " +
                "The Join Call option unlocks then.",
        ).assertExists()
    }

    @Test
    fun pendingBannerFallsBackWhenTheNameIsMissing() {
        show { UpcomingBookingPendingBanner(null) }
        rule.onNodeWithText("You'll get a notification once the expert confirms", substring = true).assertExists()
    }

    @Test
    fun messageCardShowsTheClientMessage() {
        show { UpcomingBookingMessageCard("Please prepare slides") }
        rule.onNodeWithText("Your message").assertExists()
        rule.onNodeWithText("Please prepare slides").assertExists()
    }
}
