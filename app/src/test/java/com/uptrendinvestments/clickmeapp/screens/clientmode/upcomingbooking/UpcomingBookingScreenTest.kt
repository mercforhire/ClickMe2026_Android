package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_ACCEPT_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_CARD_TAG
import com.uptrendinvestments.clickmeapp.components.chat.MESSAGE_BUTTON_SPINNER_TAG
import com.uptrendinvestments.clickmeapp.managers.CallResumeNotice
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleSubmitting
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.StatusTone
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.detailStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
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
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.initiateData
import com.uptrendinvestments.clickmeapp.testing.lineCountsOf
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.SimpleDateFormat
import java.time.Duration
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
    fun inAppVoiceShowsADisabledJoinCallOutsideTheWindow() {
        show { UpcomingBookingJoinCard("in_app_voice", null, joinEnabled = false, onJoin = {}) }
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).assertIsNotEnabled().assert(hasJoinHint())
        rule.onNodeWithText("Join Call").assertExists()
    }

    @Test
    fun anUnknownMeetingTypeFallsBackToTheJoinCall() {
        show { UpcomingBookingJoinCard(null, null, joinEnabled = false, onJoin = {}) }
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).assertIsNotEnabled().assert(hasJoinHint())
    }

    @Test
    fun inAppVoiceJoinIsEnabledInsideTheWindowAndClicksOnce() {
        var joins = 0
        show { UpcomingBookingJoinCard("in_app_voice", null, joinEnabled = true, onJoin = { joins++ }) }
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).assertIsEnabled().performClick()
        assertEquals(1, joins)
    }

    @Test
    fun skypeZoomShowsTheLinkAnEnabledCopyButtonAndJoinBelowIt() {
        show { UpcomingBookingJoinCard("skype_zoom", "https://zoom.us/j/1", joinEnabled = true, onJoin = {}) }
        rule.onNodeWithText("Meeting link").assertExists()
        rule.onNodeWithText("https://zoom.us/j/1").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG).assertIsEnabled()
        rule.onNodeWithContentDescription("Copy meeting link").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).assertIsEnabled()
        val copyBottom =
            rule
                .onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        val joinTop =
            rule
                .onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        assertTrue(copyBottom < joinTop)
    }

    @Test
    fun copyingTheLinkShowsTheCopiedState() {
        show { UpcomingBookingJoinCard("skype_zoom", "https://zoom.us/j/1", joinEnabled = false, onJoin = {}) }
        rule.onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Copied").assertExists()
    }

    @Test
    fun skypeZoomWithoutALinkShowsNotAvailableAndDisablesCopy() {
        show { UpcomingBookingJoinCard("skype_zoom", null, joinEnabled = false, onJoin = {}) }
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

    private val detailsStart = Instant.parse("2026-10-24T18:00:00Z")

    @Suppress("LongParameterList")
    private fun details(
        status: String = "confirmed",
        price: PriceDisplay = PriceDisplay.Paid("$50.00"),
        paymentPill: PaymentPill? = PaymentPill.HoldPlaced,
        meetingType: String? = "in_app_voice",
        clientNotes: String? = "Please prepare slides",
        reschedule: RescheduleProposalUi? = null,
        start: Instant = detailsStart,
        end: Instant = detailsStart.plusSeconds(3600),
        expertId: String? = "e1",
    ) = UpcomingBookingDetails(
        bookingId = "b1",
        shortId = "#B1",
        status = detailStatusDisplay(status),
        expertName = "Ada Lovelace",
        expertTitle = "Engineer",
        expertAvatarUrl = null,
        expertId = expertId,
        topicTitle = "Algorithms",
        price = price,
        start = start,
        end = end,
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
        joinNow: Instant = detailsStart.minusSeconds(600),
        messageBusy: Boolean = false,
        rescheduleSent: Boolean = false,
    ): Calls {
        rule.setContent {
            ClickMeTheme {
                UpcomingBookingContent(
                    state = state,
                    rescheduleState = rescheduleState,
                    joinNow = joinNow,
                    onJoin = { calls.events += "join" },
                    onBack = { calls.events += "back" },
                    onRetry = { calls.events += "retry" },
                    onCancel = { calls.events += "cancel" },
                    onAccept = { calls.events += "accept" },
                    onDecline = { calls.events += "decline:$it" },
                    onDismissRescheduleDialog = { calls.events += "dismiss" },
                    onOpenPaymentMethods = { calls.events += "paymentMethods" },
                    onRescheduleNoticeConsumed = { calls.events += "consumed" },
                    onReschedule = { calls.events += "reschedule" },
                    rescheduleSent = rescheduleSent,
                    onRescheduleSentConsumed = { calls.events += "sentConsumed" },
                    messageBusy = messageBusy,
                    onMessage = { calls.events += "message" },
                    onRefresh = { calls.events += "refresh" },
                    onRefreshFailureConsumed = { calls.events += "refreshFailureConsumed" },
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
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).performScrollTo().assertIsEnabled()
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

    // ---- Reschedule button (Phase 16.2, D-04 / D-05) ----

    @Test
    fun sentNoticeShowsSnackbarOnce() {
        val calls = setScreen(loaded(), rescheduleSent = true)
        rule.waitForIdle()

        rule.onNodeWithText("Reschedule request sent").assertExists()
        assertEquals(listOf("sentConsumed"), calls.events)
    }

    @Test
    fun noSentNoticeShowsNoSnackbar() {
        val calls = setScreen(loaded())
        rule.waitForIdle()

        rule.onAllNodesWithText("Reschedule request sent").assertCountEquals(0)
        assertTrue(calls.events.isEmpty())
    }

    private val sixHours = Duration.ofHours(6)
    private val clientNow = detailsStart.minus(Duration.ofDays(3))

    private fun detailsAt(
        lead: Duration,
        status: String = "confirmed",
    ): UpcomingBookingDetails {
        val start = clientNow.plus(lead)
        return details(status = status, start = start, end = start.plus(Duration.ofHours(1)))
    }

    /** Wide enough (411 dp) that the pair shares one row; the default 320 dp window stacks it. */
    @Test
    @Config(qualifiers = "w411dp-h900dp")
    fun confirmedFarOutShowsRescheduleAndCancel() {
        val calls = setScreen(loaded(detailsAt(Duration.ofDays(3))), joinNow = clientNow)
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Reschedule").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().assertExists()

        val reschedule = rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).getUnclippedBoundsInRoot()
        assertEquals(reschedule.top, cancel.top)
        assertTrue("Reschedule ends ${reschedule.right}, Cancel starts ${cancel.left}", reschedule.right <= cancel.left)

        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performClick()
        assertEquals(listOf("reschedule"), calls.events)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w411dp-h900dp")
    fun rescheduleAndCancelFitOneLineAt411dp() {
        setScreen(loaded(detailsAt(Duration.ofDays(3))), joinNow = clientNow)
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo()

        assertEquals(listOf(1), rule.lineCountsOf("Reschedule", UPCOMING_BOOKING_RESCHEDULE_TAG))
        assertEquals(listOf(1), rule.lineCountsOf("Cancel Booking", UPCOMING_BOOKING_CANCEL_TAG))
        val reschedule = rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).getUnclippedBoundsInRoot()
        assertEquals(reschedule.top, cancel.top)
    }

    @Test
    fun exactly24hShows() {
        setScreen(loaded(detailsAt(Duration.ofHours(24))), joinNow = clientNow)
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertExists()
    }

    @Test
    fun under24hShowsRescheduleAndCancel() {
        setScreen(loaded(detailsAt(Duration.ofHours(24).minusMinutes(1))), joinNow = clientNow)
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().assertExists()
    }

    @Test
    fun keepsRescheduleWhenTheJoinClockPassesThe24hMark() {
        var now by mutableStateOf(clientNow)
        val d = detailsAt(Duration.ofHours(24).plus(sixHours))
        rule.setContent {
            ClickMeTheme {
                UpcomingBookingContent(
                    state = loaded(d),
                    rescheduleState = RescheduleUiState(),
                    joinNow = now,
                    onJoin = {},
                    onBack = {},
                    onRetry = {},
                    onCancel = {},
                    onAccept = {},
                    onDecline = {},
                    onDismissRescheduleDialog = {},
                    onOpenPaymentMethods = {},
                    onRescheduleNoticeConsumed = {},
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertExists()
        now = clientNow.plus(sixHours).plusSeconds(1)
        rule.waitForIdle()
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertExists()
    }

    @Test
    fun pendingRescheduleHides() {
        setScreen(loaded(detailsAt(Duration.ofDays(3), status = "pending_reschedule")), joinNow = clientNow)
        rule.onAllNodesWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).assertCountEquals(0)
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().assertExists()
    }

    @Test
    fun pendingApprovalShowsRescheduleAndCancel() {
        val calls =
            setScreen(loaded(detailsAt(Duration.ofDays(3), status = "pending_approval")), joinNow = clientNow)
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().assertExists()
        rule
            .onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG)
            .performScrollTo()
            .assertExists()
            .performClick()
        assertEquals(listOf("reschedule"), calls.events)
    }

    @Test
    fun inProgressShowsRescheduleAlone() {
        setScreen(loaded(details(status = "in_progress")))
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertExists()
        rule.onAllNodesWithTag(UPCOMING_BOOKING_CANCEL_TAG).assertCountEquals(0)
    }

    @Test
    fun disabledWhileResponseInFlight() {
        setScreen(
            loaded(detailsAt(Duration.ofDays(3))),
            RescheduleUiState(submitting = RescheduleSubmitting.Accepting),
            joinNow = clientNow,
        )
        rule.onNodeWithTag(UPCOMING_BOOKING_RESCHEDULE_TAG).performScrollTo().assertIsNotEnabled()
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
    fun skypeZoomBookingKeepsTheMeetingLinkRowAndAddsJoinBelowIt() {
        setScreen(loaded(details(meetingType = "skype_zoom")))
        rule.onNodeWithText("Meeting link").assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_COPY_LINK_TAG).assertExists()
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).performScrollTo().assertIsEnabled()
    }

    @Test
    fun joinIsEnabledTenMinutesBeforeStartAndClicksOnce() {
        val calls = setScreen(loaded())
        rule
            .onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(listOf("join"), calls.events)
    }

    @Test
    fun joinIsDisabledTwoHoursBeforeStartWithTheHint() {
        setScreen(loaded(), joinNow = detailsStart.minusSeconds(2 * 3600))
        rule
            .onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG)
            .performScrollTo()
            .assertIsNotEnabled()
            .assert(hasJoinHint())
    }

    @Test
    fun aCompletedBookingWhoseEndHasNotPassedCanBeRejoined() {
        val start = detailsStart.minusSeconds(2 * 3600)
        setScreen(
            loaded(details(status = "completed", start = start, end = detailsStart.plusSeconds(300))),
            joinNow = detailsStart,
        )
        rule.onNodeWithTag(UPCOMING_BOOKING_JOIN_TAG).performScrollTo().assertIsEnabled()
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
    fun noPrivateNoteActionExists() {
        setScreen(loaded())
        rule.onAllNodesWithText("Private Note").assertCountEquals(0)
        rule.onNodeWithText("Reschedule").assertExists()
    }

    // ---- call resume note, refresh standard, unavailable and width cap (Phase 15) ----

    @Test
    fun noteIsPoliteLiveRegionAboveJoin() {
        setScreen(UpcomingBookingUiState.Loaded(details(), showCallResumeNote = true))
        rule
            .onNodeWithText("You were on a call. Tap Join Call to rejoin.")
            .assertExists()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertTrue(rule.topOf("call_resume_note") < rule.topOf(UPCOMING_BOOKING_JOIN_TAG))
    }

    @Test
    fun noNoteWhenNotFlagged() {
        setScreen(loaded())
        rule.onAllNodesWithText("You were on a call. Tap Join Call to rejoin.").assertCountEquals(0)
    }

    @Test
    fun refreshFailedShowsRetry() {
        val calls = setScreen(UpcomingBookingUiState.Loaded(details(), refreshFailed = true))
        rule.waitForIdle()
        rule.onNodeWithText("Confirmed").assertExists()
        assertEquals(listOf("refreshFailureConsumed"), calls.events)
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(listOf("refreshFailureConsumed", "refresh"), calls.events)
        rule.onNodeWithText("Confirmed").assertExists()
    }

    @Test
    fun refreshingShowsLinearIndicator() {
        setScreen(UpcomingBookingUiState.Loaded(details(), isRefreshing = true))
        rule.onNodeWithTag("upcoming_booking_refresh_progress").assertExists()
        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(0)
        rule.onNodeWithText("Confirmed").assertExists()
    }

    @Test
    fun noIndicatorWhenNotRefreshing() {
        setScreen(loaded())
        rule.onAllNodesWithTag("upcoming_booking_refresh_progress").assertCountEquals(0)
    }

    @Test
    fun firstLoadErrorNamed() {
        val calls = setScreen(UpcomingBookingUiState.Failed(FieldError.Text("Boom")))
        rule
            .onNodeWithText("Couldn't load booking")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), calls.events)
    }

    @Test
    fun unavailableShowsNoRetry() {
        setScreen(UpcomingBookingUiState.Unavailable)
        rule.onNodeWithText("This booking isn't available").assertExists()
        rule.onNodeWithText("It may have been removed. Go back and try another.").assertExists()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
    }

    /** D-02: on a large window the content column is capped at 600 dp and centered. */
    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun contentCapped() {
        setScreen(loaded())
        val message = rule.onNodeWithTag(MESSAGE_EXPERT_TAG).getUnclippedBoundsInRoot()
        assertTrue("message button spans ${message.left} to ${message.right}", message.right <= 750.dp)
        assertTrue("message button spans ${message.left} to ${message.right}", message.left >= 150.dp)
    }

    // ---- Cancel guard (D-13), through the real ViewModel ----

    private fun screenViewModel(
        api: FakeClickMeApi,
        guard: FakeCallGuard = FakeCallGuard(),
    ): UpcomingBookingViewModel {
        val bookings = ClientBookingsRepository(api)
        return UpcomingBookingViewModel(
            UpcomingBookingKey("b1"),
            bookings,
            RescheduleResponder(bookings, PaymentMethodsRepository(api)),
            FakeRealtimeConnection(),
            MutableClock(detailsStart.minusSeconds(600)),
            FakeCallStarter(),
            guard,
            JoinClock(MutableClock(detailsStart.minusSeconds(600)), CoroutineScope(Job())),
            CallResumeNotice(),
            RescheduleSentRequest(),
        )
    }

    private fun cancelWithGuard(blocked: Boolean): Pair<Calls, FakeCallGuard> {
        val api = FakeClickMeApi()
        api.clientBookingDetailHandler = { bookingDetail() }
        val guard = FakeCallGuard().also { it.blocked = blocked }
        val vm = screenViewModel(api, guard)
        val chatEntry = testChatEntryViewModel(api)
        val calls = Calls()
        rule.setContent {
            ClickMeTheme {
                UpcomingBookingScreen(
                    viewModel = vm,
                    onBack = {},
                    onCancel = { calls.events += "cancel" },
                    onOpenPaymentMethods = {},
                    onReschedule = {},
                    chatEntry = chatEntry,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(UPCOMING_BOOKING_CANCEL_TAG).performScrollTo().performClick()
        return calls to guard
    }

    @Test
    fun cancelOpensTheCancelFlowWhenTheGuardAllows() {
        val (calls, guard) = cancelWithGuard(blocked = false)
        assertEquals(listOf("cancel"), calls.events)
        assertEquals(listOf("b1" to RestrictedAction.CancelBooking), guard.modifyAttempts)
    }

    @Test
    fun cancelIsBlockedWhileThisBookingsCallIsLive() {
        val (calls, guard) = cancelWithGuard(blocked = true)
        assertTrue(calls.events.isEmpty())
        assertEquals(listOf("b1" to RestrictedAction.CancelBooking), guard.modifyAttempts)
    }

    // ---- Message Expert (D-14) ----

    @Test
    fun messageExpertSitsBetweenTheJoinCardAndCancel() {
        setScreen(loaded())
        rule.onNodeWithTag(MESSAGE_EXPERT_TAG).performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Message Expert").assertExists()
        val joinY = rule.topOf(UPCOMING_BOOKING_JOIN_TAG)
        val messageY = rule.topOf(MESSAGE_EXPERT_TAG)
        val cancelY = rule.topOf(UPCOMING_BOOKING_CANCEL_TAG)
        assertTrue(joinY < messageY)
        assertTrue(messageY < cancelY)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.topOf(tag: String): Float {
        // The column is not lazy, so off-screen children are laid out and keep a stable position in the scroll content.
        return onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
    }

    @Test
    fun messageExpertClickCallsOnMessage() {
        val calls = setScreen(loaded())
        rule.onNodeWithTag(MESSAGE_EXPERT_TAG).performScrollTo().performClick()
        assertEquals(listOf("message"), calls.events)
    }

    @Test
    fun messageExpertShowsTheSpinnerWhileBusy() {
        setScreen(loaded(), messageBusy = true)
        rule.onNodeWithTag(MESSAGE_EXPERT_TAG).performScrollTo()
        rule.onNodeWithTag(MESSAGE_BUTTON_SPINNER_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun messageExpertDisabledWithoutExpertId() {
        setScreen(loaded(details(expertId = null)))
        rule.onNodeWithTag(MESSAGE_EXPERT_TAG).performScrollTo().assertIsNotEnabled()
    }

    private fun messageThroughScreen(
        api: FakeClickMeApi,
        launch: ChatLaunchRequest,
    ) {
        api.clientBookingDetailHandler = { bookingDetail() }
        val vm = screenViewModel(api)
        val chatEntry = testChatEntryViewModel(api, launch)
        rule.setContent {
            ClickMeTheme {
                UpcomingBookingScreen(
                    viewModel = vm,
                    onBack = {},
                    onCancel = {},
                    onOpenPaymentMethods = {},
                    onReschedule = {},
                    chatEntry = chatEntry,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(MESSAGE_EXPERT_TAG).performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test
    fun messageExpertOpensChat() {
        val api = FakeClickMeApi()
        api.initiateChatHandler = { initiateData("t9") }
        val launch = ChatLaunchRequest()
        messageThroughScreen(api, launch)
        assertEquals(listOf("e1"), api.initiateChatRequests.map { it.peerId })
        val thread = launch.thread.consume()
        assertEquals("t9", thread?.threadId)
        assertEquals("e1", thread?.peerId)
        assertEquals(true, thread?.peerIsExpert)
    }

    @Test
    fun messageExpertFailureShowsTheErrorDialog() {
        val api = FakeClickMeApi()
        api.initiateChatHandler = {
            throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
        }
        val launch = ChatLaunchRequest()
        messageThroughScreen(api, launch)
        rule.onNodeWithText("Couldn't open chat").assertExists()
        assertEquals(null, launch.thread.consume())
    }
}
