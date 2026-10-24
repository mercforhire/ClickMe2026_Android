package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_ACCEPT_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleSubmitting
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
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
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        var pill by mutableStateOf(PaymentPill.HoldPlaced)
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
        rule
            .onNodeWithText(
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

    // ---- Task 2: screen ----

    private val proposal =
        RescheduleProposalUi(
            currentStart = Instant.parse("2026-10-24T18:00:00Z"),
            proposedStart = Instant.parse("2026-10-28T18:00:00Z"),
            proposedEnd = Instant.parse("2026-10-28T19:00:00Z"),
            note = null,
            initiatedByExpert = true,
            showsPaymentDisclosure = false,
            chargesSoon = false,
        )

    @Suppress("LongParameterList")
    private fun details(
        status: String = "confirmed",
        price: PriceDisplay = PriceDisplay.Paid("$50.00"),
        paymentPill: PaymentPill? = PaymentPill.HoldPlaced,
        meetingType: String? = "in_app_voice",
        clientNotes: String? = "Please prepare slides",
        reschedule: RescheduleProposalUi? = null,
    ) = UpcomingBookingDetails(
        bookingId = "b1",
        shortId = "#B1",
        status = detailStatusDisplay(status),
        expertName = "Ada Lovelace",
        expertTitle = "Engineer",
        expertAvatarUrl = null,
        topicTitle = "Algorithms",
        price = price,
        start = Instant.parse("2026-10-24T18:00:00Z"),
        end = Instant.parse("2026-10-24T19:00:00Z"),
        expertTimezone = "America/Toronto",
        paymentPill = paymentPill,
        meetingType = meetingType,
        joinLink = null,
        clientNotes = clientNotes,
        showsPendingBanner = status == "pending_approval",
        canCancel = status in setOf("confirmed", "pending_approval", "pending_reschedule"),
        reschedule = reschedule,
    )

    private class Calls {
        val events = mutableListOf<String>()
    }

    private fun setScreen(
        state: UpcomingBookingUiState,
        rescheduleState: RescheduleUiState = RescheduleUiState(),
        calls: Calls = Calls(),
    ): Calls {
        rule.setContent {
            ClickMeTheme {
                UpcomingBookingContent(
                    state = state,
                    rescheduleState = rescheduleState,
                    onBack = { calls.events += "back" },
                    onRetry = { calls.events += "retry" },
                    onCancel = { calls.events += "cancel" },
                    onAccept = { calls.events += "accept" },
                    onDecline = { calls.events += "decline:$it" },
                    onDismissRescheduleDialog = { calls.events += "dismiss" },
                    onOpenPaymentMethods = { calls.events += "paymentMethods" },
                    onRescheduleNoticeConsumed = { calls.events += "consumed" },
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        return calls
    }

    private fun loaded(d: UpcomingBookingDetails = details()) = UpcomingBookingUiState.Loaded(d)

    @Test
    fun titleIsUpcomingBooking() {
        setScreen(loaded())
        rule.onNodeWithText("Upcoming Booking").assertExists()
    }

    @Test
    fun loadingShowsTheLoadingDescription() {
        setScreen(UpcomingBookingUiState.Loading)
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun failedShowsHeadingMessageAndRetry() {
        val calls = setScreen(UpcomingBookingUiState.Failed(FieldError.Text("Boom")))
        rule.onNodeWithText("Couldn't load booking").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), calls.events)
    }

    @Test
    fun confirmedPaidHeldBookingShowsEveryBlockAndCancels() {
        val calls = setScreen(loaded())
        rule.onNodeWithText("Confirmed").assertExists()
        rule.onNodeWithText("Card hold placed").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Please prepare slides").assertExists()
        rule
            .onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(listOf("cancel"), calls.events)
    }

    @Test
    fun pendingApprovalShowsTheBannerNotTheJoinCard() {
        setScreen(loaded(details(status = "pending_approval")))
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        rule.onAllNodesWithTag(UPCOMING_BOOKING_JOIN_TAG).assertCountEquals(0)
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().assertExists()
    }

    @Test
    fun inProgressAndCompletedShowNoCancel() {
        setScreen(loaded(details(status = "in_progress")))
        rule.onAllNodesWithTag(UPCOMING_BOOKING_CANCEL_TAG).assertCountEquals(0)
    }

    @Test
    fun completedShowsNoCancel() {
        setScreen(loaded(details(status = "completed")))
        rule.onAllNodesWithTag(UPCOMING_BOOKING_CANCEL_TAG).assertCountEquals(0)
    }

    @Test
    fun freeBookingShowsNoPaymentPill() {
        setScreen(loaded(details(price = PriceDisplay.Free, paymentPill = null)))
        rule.onAllNodesWithTag(UPCOMING_BOOKING_PAYMENT_PILL_TAG).assertCountEquals(0)
        rule.onNodeWithText("Free").assertExists()
    }

    @Test
    fun skypeZoomBookingShowsTheMeetingLinkRow() {
        setScreen(loaded(details(meetingType = "skype_zoom")))
        rule.onNodeWithText("Meeting link").assertExists()
        rule.onAllNodesWithTag(UPCOMING_BOOKING_JOIN_TAG).assertCountEquals(0)
    }

    @Test
    fun pendingRescheduleShowsTheCardAboveTheStatusRow() {
        setScreen(loaded(details(status = "pending_reschedule", reschedule = proposal)))
        val cardTop =
            rule
                .onNodeWithTag(RESCHEDULE_CARD_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        val statusTop =
            rule
                .onNodeWithTag(UPCOMING_BOOKING_STATUS_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        assertTrue(cardTop < statusTop)
    }

    @Test
    fun cancelIsDisabledWhileARescheduleResponseRuns() {
        setScreen(
            loaded(details(status = "pending_reschedule", reschedule = proposal)),
            RescheduleUiState(submitting = RescheduleSubmitting.Accepting),
        )
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun acceptingFromTheCardCallsOnAccept() {
        val calls = setScreen(loaded(details(status = "pending_reschedule", reschedule = proposal)))
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(hasText("Accept") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("accept"), calls.events)
    }

    @Test
    fun paymentMethodDialogOpensPaymentMethodsAndDismisses() {
        val calls = setScreen(loaded(), RescheduleUiState(dialog = RescheduleDialog.PaymentMethodNeeded))
        rule.onNodeWithText("Add a Payment Method").performClick()
        assertEquals(listOf("paymentMethods", "dismiss"), calls.events)
    }

    @Test
    fun errorDialogWithPopOnDismissDismissesAndLeaves() {
        val calls =
            setScreen(
                loaded(),
                RescheduleUiState(
                    dialog =
                        RescheduleDialog.Error(
                            FieldError.Res(R.string.reschedule_error_not_pending),
                            popOnDismiss = true,
                        ),
                ),
            )
        rule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss", "back"), calls.events)
    }

    @Test
    fun acceptedNoticeShowsASnackbarAndIsConsumed() {
        val calls = setScreen(loaded(), RescheduleUiState(notice = RescheduleNotice.Accepted))
        rule.waitForIdle()
        rule.onNodeWithText("Reschedule accepted").assertExists()
        assertEquals(listOf("consumed"), calls.events)
    }

    @Test
    fun noRescheduleMessageOrPrivateNoteActionsExist() {
        setScreen(loaded())
        listOf("Reschedule", "Message Expert", "Private Note").forEach {
            rule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }
}
