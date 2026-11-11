package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RATING_STAR_FILLED_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_ACCEPT_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_CARD_TAG
import com.uptrendinvestments.clickmeapp.components.booknote.MY_NOTE_CARD_TAG
import com.uptrendinvestments.clickmeapp.components.chat.MESSAGE_BUTTON_SPINNER_TAG
import com.uptrendinvestments.clickmeapp.model.responses.ReviewContextData
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.ClientBookingNoteKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BOOKING_SUMMARY_BOOK_AGAIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BOOKING_SUMMARY_FEEDBACK_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BOOKING_SUMMARY_LEAVE_REVIEW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.summaryStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.time.Clock
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
        privateNote: String? = null,
        bookingId: String = "b1",
    ) = BookingSummaryDetails(
        bookingId = bookingId,
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
        privateNote = privateNote,
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
        reviewedIds: Set<String> = emptySet(),
        reviewThanks: Boolean = false,
        fontScale: Float = 1f,
    ): Calls {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
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
                        reviewedIds = reviewedIds,
                        onLeaveReview = { calls.events += "leaveReview" },
                        onOpenNote = { calls.events += "note" },
                        reviewThanks = reviewThanks,
                        onReviewThanksConsumed = { calls.events += "thanksConsumed" },
                        messageBusy = messageBusy,
                        onMessage = { calls.events += "message" },
                        zone = zone,
                        timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                        locale = Locale.US,
                    )
                }
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
    fun bookingIdShowsInTheShortFormat() {
        setScreen(BookingSummaryUiState.Loaded(details()))
        rule.onNodeWithText("ID: #B1").assertExists()
    }

    @Test
    fun fullUuidBookingIdMatchesIos() {
        setScreen(BookingSummaryUiState.Loaded(details(bookingId = "7f3c9a2b-1d4e-4c8a-9b0f-123456789abc")))
        rule.onNodeWithText("ID: #7F3C9A2B").assertExists()
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
    @Config(qualifiers = "w900dp-h1200dp")
    fun capped() {
        setScreen(BookingSummaryUiState.Loaded(details()))
        val button = rule.onNodeWithTag(BOOKING_SUMMARY_BOOK_AGAIN_TAG).getUnclippedBoundsInRoot()
        assertTrue("button width ${button.right - button.left}", button.right - button.left <= 600.dp)
        // Centered: the content column starts well in from the 900 dp window edge.
        assertTrue("button left ${button.left}", button.left >= 150.dp)
    }

    // ---- My Note (Phase 16.4, D-06, D-07, D-08) ----

    private val notePrompt = "Tap to jot down your thoughts \u2014 only you can see this."

    @Test
    fun myNoteCardShowsTheFullNote() {
        setScreen(BookingSummaryUiState.Loaded(details(privateNote = "Call back")))
        rule.onNodeWithText("My Note").assertExists()
        rule.onNodeWithText("Call back").assertExists()
        rule.onAllNodesWithText(notePrompt).assertCountEquals(0)
    }

    @Test
    fun myNoteCardShowsThePromptWhenThereIsNoNote() {
        setScreen(BookingSummaryUiState.Loaded(details(privateNote = null)))
        rule.onNodeWithText("My Note").assertExists()
        rule.onNodeWithText(notePrompt).assertExists()
    }

    @Test
    fun myNoteSitsAfterFeedbackAndBeforeBookAgain() {
        setScreen(loaded(SummaryReview.Submitted(4, "Helpful")))

        fun top(tag: String) =
            rule
                .onNodeWithTag(tag)
                .fetchSemanticsNode()
                .positionInRoot.y
        val feedback = top(BOOKING_SUMMARY_FEEDBACK_TAG)
        val note = top(MY_NOTE_CARD_TAG)
        val bookAgain = top(BOOKING_SUMMARY_BOOK_AGAIN_TAG)
        assertTrue("feedback $feedback note $note", feedback < note)
        assertTrue("note $note bookAgain $bookAgain", note < bookAgain)
    }

    @Test
    fun tappingMyNoteCallsOnOpenNoteOnce() {
        val calls = setScreen(BookingSummaryUiState.Loaded(details()))
        rule.onNodeWithTag(MY_NOTE_CARD_TAG).performScrollTo().performClick()
        assertEquals(listOf("note"), calls.events)
    }

    @Test
    fun myNoteShowsOnEveryStatus() {
        var state by mutableStateOf<BookingSummaryUiState>(BookingSummaryUiState.Loaded(details(status = "completed")))
        rule.setContent {
            ClickMeTheme {
                BookingSummaryContent(
                    state = state,
                    rescheduleState = RescheduleUiState(),
                    onBack = {},
                    onRetry = {},
                    onBookAgain = {},
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
        listOf("completed", "cancelled", "missed").forEach { status ->
            state = BookingSummaryUiState.Loaded(details(status = status))
            rule.waitForIdle()
            rule.onNodeWithTag(MY_NOTE_CARD_TAG).assertExists()
            rule.onNodeWithText("My Note").assertExists()
        }
    }

    @Test
    @Suppress("MagicNumber")
    fun largeFontKeepsMyNote() {
        val long = "A long private note about the session. ".repeat(12).trim()
        setScreen(BookingSummaryUiState.Loaded(details(privateNote = long)), fontScale = 2f)
        rule.onNodeWithTag(MY_NOTE_CARD_TAG).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("My Note").assertIsDisplayed()
        rule.onNodeWithText(long).assertIsDisplayed()
    }

    @Test
    fun theRealViewModelCarriesTheNoteToTheCard() {
        val api = FakeClickMeApi()
        api.clientBookingDetailHandler = { bookingDetail(id = it, status = "completed", privateNote = "Call back") }
        api.reviewContextHandler = { ReviewContextData() }
        val viewModel =
            BookingSummaryViewModel(
                BookingSummaryKey("b1"),
                ClientBookingsRepository(api),
                RescheduleResponder(ClientBookingsRepository(api), PaymentMethodsRepository(api)),
                Clock.systemUTC(),
                ReviewsRepository(api),
                ReviewLaunchRequest(),
            )
        val chatEntry = testChatEntryViewModel(api)
        val opened = mutableListOf<ClientBookingNoteKey>()
        rule.setContent {
            ClickMeTheme {
                BookingSummaryScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onBookAgain = {},
                    onOpenPaymentMethods = {},
                    onLeaveReview = {},
                    onOpenNote = { opened += it },
                    chatEntry = chatEntry,
                )
            }
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Call back").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(MY_NOTE_CARD_TAG).performScrollTo().performClick()
        assertEquals(1, opened.size)
        assertEquals("b1", opened.single().bookingId)
        assertEquals("Ada Lovelace", opened.single().expertName)
        assertEquals("Algorithms", opened.single().topic)
    }

    // ---- Leave a review and Feedback (D-18, REV-01) ----

    private fun loaded(
        review: SummaryReview,
        status: String = "completed",
    ) = BookingSummaryUiState.Loaded(details(status = status), review)

    @Test
    fun feedbackShownWhenSubmitted() {
        setScreen(loaded(SummaryReview.Submitted(4, "Helpful")))
        rule.onNodeWithText("Your feedback").assertExists()
        rule.onNodeWithTag(BOOKING_SUMMARY_FEEDBACK_TAG).assertExists()
        rule.onNodeWithContentDescription("Rated 4 out of 5", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Helpful").assertExists()
        rule.onAllNodesWithTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG).assertCountEquals(0)
    }

    @Test
    fun starsOnlyWithoutComment() {
        setScreen(loaded(SummaryReview.Submitted(5, null)))
        rule.onNodeWithTag(BOOKING_SUMMARY_FEEDBACK_TAG).assertExists()
        rule.onNodeWithContentDescription("Rated 5 out of 5", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithTag(RATING_STAR_FILLED_TAG, useUnmergedTree = true).assertCountEquals(5)
    }

    @Test
    fun leaveReviewWhileUnknownOrNotSubmitted() {
        setScreen(loaded(SummaryReview.Unknown))
        rule.onNodeWithTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG).assertExists()
        rule.onAllNodesWithTag(BOOKING_SUMMARY_FEEDBACK_TAG).assertCountEquals(0)
    }

    @Test
    fun leaveReviewSitsBetweenBookAgainAndMessageExpertAndClicks() {
        val calls = setScreen(loaded(SummaryReview.NotSubmitted))
        rule.onAllNodesWithTag(BOOKING_SUMMARY_FEEDBACK_TAG).assertCountEquals(0)

        fun y(tag: String): Float {
            val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
            return node.positionInRoot.y
        }
        val book = y(BOOKING_SUMMARY_BOOK_AGAIN_TAG)
        val review = y(BOOKING_SUMMARY_LEAVE_REVIEW_TAG)
        val message = y(BOOKING_SUMMARY_MESSAGE_TAG)
        assertTrue(book < review && review < message)
        rule.onNodeWithTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG).performScrollTo().performClick()
        assertEquals(listOf("leaveReview"), calls.events)
    }

    @Test
    fun noReviewUiForCancelledOrMissed() {
        assertEquals(false, loaded(SummaryReview.Unknown, "missed").showLeaveReview(emptySet()))
        setScreen(loaded(SummaryReview.Unknown, "cancelled"))
        rule.onAllNodesWithTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG).assertCountEquals(0)
        rule.onAllNodesWithTag(BOOKING_SUMMARY_FEEDBACK_TAG).assertCountEquals(0)
    }

    @Test
    fun reviewedThisSessionHidesButton() {
        setScreen(loaded(SummaryReview.NotSubmitted), reviewedIds = setOf("b1"))
        rule.waitForIdle()
        rule.onAllNodesWithTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG).assertCountEquals(0)
    }

    @Test
    fun thanksSnackbarShowsAndIsConsumed() {
        val calls = setScreen(loaded(SummaryReview.NotSubmitted), reviewThanks = true)
        rule.onNodeWithText("Thanks for your review").assertExists()
        assertEquals(listOf("thanksConsumed"), calls.events)
    }

    @Test
    fun largeFontButtonGrows() {
        setScreen(loaded(SummaryReview.NotSubmitted), fontScale = 2f)
        rule.onNodeWithTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG).performScrollTo().assertExists()
        rule.onNodeWithText("Leave a review").assertExists()
    }

    @Test
    fun largeFontShowsALongCommentInFull() {
        val longComment = "A very long comment about the session. ".repeat(20).trim()
        setScreen(loaded(SummaryReview.Submitted(4, longComment)), fontScale = 2f)
        rule.onNodeWithText(longComment).assertExists()
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
