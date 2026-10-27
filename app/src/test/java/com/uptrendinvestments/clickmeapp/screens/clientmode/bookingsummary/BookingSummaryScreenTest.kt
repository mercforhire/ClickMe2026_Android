package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import androidx.compose.ui.layout.positionInRoot
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
import com.uptrendinvestments.clickmeapp.components.chat.MESSAGE_BUTTON_SPINNER_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BOOKING_SUMMARY_BOOK_AGAIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.summaryStatusDisplay
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
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class BookingSummaryScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")

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

    private fun details(
        status: String = "completed",
        price: PriceDisplay = PriceDisplay.Paid("$50.00"),
        priceLabel: SummaryPriceLabel = SummaryPriceLabel.PricePaid,
        bookAgain: BookAgainSeed? = BookAgainSeed("e1", "Ada Lovelace", "Engineer", null),
        reschedule: RescheduleProposalUi? = null,
        expertId: String? = "e1",
    ) = BookingSummaryDetails(
        bookingId = "b1",
        status = summaryStatusDisplay(status),
        expertName = "Ada Lovelace",
        expertTitle = "Engineer",
        expertAvatarUrl = null,
        expertId = expertId,
        topicTitle = "Algorithms",
        start = Instant.parse("2026-10-24T18:00:00Z"),
        end = Instant.parse("2026-10-24T19:00:00Z"),
        price = price,
        priceLabel = priceLabel,
        bookAgain = bookAgain,
        reschedule = reschedule,
    )

    private class Calls {
        val events = mutableListOf<String>()
        var bookAgain: BookingKey? = null
    }

    private fun setScreen(
        state: BookingSummaryUiState,
        rescheduleState: RescheduleUiState = RescheduleUiState(),
        calls: Calls = Calls(),
        messageBusy: Boolean = false,
    ): Calls {
        rule.setContent {
            ClickMeTheme {
                BookingSummaryContent(
                    state = state,
                    rescheduleState = rescheduleState,
                    onBack = { calls.events += "back" },
                    onRetry = { calls.events += "retry" },
                    onBookAgain = { calls.bookAgain = it },
                    onAccept = { calls.events += "accept" },
                    onDecline = { calls.events += "decline:$it" },
                    onDismissRescheduleDialog = { calls.events += "dismiss" },
                    onOpenPaymentMethods = { calls.events += "paymentMethods" },
                    onRescheduleNoticeConsumed = { calls.events += "consumed" },
                    messageBusy = messageBusy,
                    onMessage = { calls.events += "message" },
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        return calls
    }

    @Test
    fun completedPaidSummaryShowsEverythingTheSpecNames() {
        setScreen(BookingSummaryUiState.Loaded(details()))

        rule.onNodeWithText("Session Summary").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Engineer").assertExists()
        rule.onNodeWithText("Completed").assertExists()
        rule.onNodeWithText("Topic").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Oct 24, 2026, 2:00 PM – 3:00 PM").assertExists()
        rule.onNodeWithText("Price paid").assertExists()
        rule.onNodeWithText("$50.00").assertExists()
    }

    @Test
    fun refundedCancelledBookingShowsRefundedAndCancelled() {
        setScreen(
            BookingSummaryUiState.Loaded(
                details(status = "cancelled", priceLabel = SummaryPriceLabel.Refunded),
            ),
        )
        rule.onNodeWithText("Refunded").assertExists()
        rule.onNodeWithText("Cancelled").assertExists()
    }

    @Test
    fun freeBookingShowsSessionFeeAndFree() {
        setScreen(
            BookingSummaryUiState.Loaded(
                details(price = PriceDisplay.Free, priceLabel = SummaryPriceLabel.SessionFee),
            ),
        )
        rule.onNodeWithText("Session fee").assertExists()
        rule.onNodeWithText("Free").assertExists()
    }

    @Test
    fun bookAgainOpensMakeABookingWithTheSeed() {
        val calls = setScreen(BookingSummaryUiState.Loaded(details()))
        rule.onNodeWithTag(BOOKING_SUMMARY_BOOK_AGAIN_TAG).performScrollTo().performClick()
        assertEquals(BookingKey("e1", "Ada Lovelace", "Engineer", null), calls.bookAgain)
    }

    @Test
    fun bookAgainIsAbsentWhenTheSeedIsMissing() {
        setScreen(BookingSummaryUiState.Loaded(details(bookAgain = null)))
        rule.onAllNodesWithText("Book Again").assertCountEquals(0)
        rule.onAllNodesWithTag(BOOKING_SUMMARY_BOOK_AGAIN_TAG).assertCountEquals(0)
    }

    @Test
    fun pendingRescheduleShowsTheCardAboveTheSummaryCard() {
        val calls =
            setScreen(
                BookingSummaryUiState.Loaded(details(status = "pending_reschedule", reschedule = proposal)),
            )

        val cardTop =
            rule
                .onNodeWithTag(RESCHEDULE_CARD_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        val summaryTop =
            rule
                .onNodeWithText("Topic")
                .fetchSemanticsNode()
                .positionInRoot.y
        assertTrue(cardTop < summaryTop)

        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(hasText("Accept") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("accept"), calls.events)
    }

    @Test
    fun paymentMethodDialogOpensPaymentMethodsAndDismisses() {
        val calls =
            setScreen(
                BookingSummaryUiState.Loaded(details()),
                RescheduleUiState(dialog = RescheduleDialog.PaymentMethodNeeded),
            )
        rule.onNodeWithText("Add a payment method").assertExists()
        rule.onNodeWithText("Add a Payment Method").performClick()
        assertEquals(listOf("paymentMethods", "dismiss"), calls.events)
    }

    @Test
    fun errorDialogWithPopOnDismissDismissesAndLeaves() {
        val calls =
            setScreen(
                BookingSummaryUiState.Loaded(details()),
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
    fun declinedNoticeShowsASnackbarAndIsConsumed() {
        val calls =
            setScreen(
                BookingSummaryUiState.Loaded(details()),
                RescheduleUiState(notice = RescheduleNotice.Declined),
            )
        rule.waitForIdle()
        rule.onNodeWithText("Reschedule declined").assertExists()
        assertEquals(listOf("consumed"), calls.events)
    }

    @Test
    fun loadingShowsTheLoadingDescription() {
        setScreen(BookingSummaryUiState.Loading)
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun failedShowsTheHeadingMessageAndRetry() {
        val calls =
            setScreen(
                BookingSummaryUiState.Failed(FieldError.Res(R.string.login_error_network)),
            )
        rule.onNodeWithText("Couldn't load session").assertExists()
        rule.onNodeWithText("Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), calls.events)
    }

    @Test
    fun phaseThirteenElementsDoNotRender() {
        setScreen(BookingSummaryUiState.Loaded(details()))
        listOf("Leave a Review", "Feedback", "My Note").forEach {
            rule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    // ---- Message Expert (D-14) ----

    @Test
    fun messageExpertBelowSummaryCallsOnMessage() {
        val calls = setScreen(BookingSummaryUiState.Loaded(details()))
        rule.onNodeWithText("Message Expert").assertExists()
        rule
            .onNodeWithTag(BOOKING_SUMMARY_MESSAGE_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(listOf("message"), calls.events)
    }

    @Test
    fun messageExpertSitsBelowTheSummaryCard() {
        setScreen(BookingSummaryUiState.Loaded(details()))
        val bookAgainY =
            rule
                .onNodeWithTag(BOOKING_SUMMARY_BOOK_AGAIN_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        val messageY =
            rule
                .onNodeWithTag(BOOKING_SUMMARY_MESSAGE_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        assertTrue(bookAgainY < messageY)
    }

    @Test
    fun messageExpertIsDisabledWithoutAnExpertId() {
        setScreen(BookingSummaryUiState.Loaded(details(expertId = null)))
        rule.onNodeWithTag(BOOKING_SUMMARY_MESSAGE_TAG).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun messageExpertShowsTheSpinnerWhileBusy() {
        setScreen(BookingSummaryUiState.Loaded(details()), messageBusy = true)
        rule.onNodeWithTag(BOOKING_SUMMARY_MESSAGE_TAG).performScrollTo()
        rule.onNodeWithTag(MESSAGE_BUTTON_SPINNER_TAG, useUnmergedTree = true).assertExists()
    }
}
