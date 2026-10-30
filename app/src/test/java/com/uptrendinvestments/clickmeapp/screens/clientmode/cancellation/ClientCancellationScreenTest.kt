package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_COMMENT_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_KEEP_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_REASON_ROW_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CancellationConfirmStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CancellationReasonStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.label
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ClientCancellationScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val time12 = SimpleDateFormat("h:mm a", Locale.US)

    private val summary =
        CancellationSummary(
            expertName = "Ada Lovelace",
            expertTitle = "Machine Learning Coach",
            expertAvatarUrl = null,
            start = Instant.parse("2026-10-24T18:00:00Z"),
            end = Instant.parse("2026-10-24T19:00:00Z"),
        )

    private var selectedReason: CancellationReason? = null
    private var typed: String? = null
    private var continueCount = 0
    private var keepCount = 0
    private var confirmCount = 0

    // region Task 1: reason step

    private fun showReasonStep(
        reason: CancellationReason = CancellationReason.ChangeOfPlans,
        comment: String = "",
    ) {
        rule.setContent {
            ClickMeTheme {
                CancellationReasonStep(
                    reason = reason,
                    comment = comment,
                    onSelectReason = { selectedReason = it },
                    onCommentChange = { typed = it },
                    onContinue = { continueCount++ },
                )
            }
        }
    }

    @Test
    fun reasonStepShowsHeadlineAndFiveReasonsInOrder() {
        showReasonStep(reason = CancellationReason.ScheduleConflict)

        rule
            .onNodeWithText("Why are you cancelling?")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        CancellationReason.entries.forEach { entry ->
            rule.onNodeWithTag(CANCEL_REASON_ROW_TAG_PREFIX + entry.name).assertExists()
        }
        listOf("Change of plans", "Found another expert", "Schedule conflict", "Personal emergency", "Other")
            .forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithTag(CANCEL_REASON_ROW_TAG_PREFIX + "ScheduleConflict").assertIsSelected()
    }

    @Test
    fun clickingAReasonReportsIt() {
        showReasonStep()

        rule.onNodeWithText("Schedule conflict").performClick()

        assertEquals(CancellationReason.ScheduleConflict, selectedReason)
    }

    @Test
    fun reasonLabelsEqualTheWireLabels() {
        var labels: List<String> = emptyList()
        rule.setContent {
            ClickMeTheme { labels = CancellationReason.entries.map { it.label() } }
        }
        rule.waitForIdle()

        assertEquals(CancellationReason.entries.map { it.wireLabel }, labels)
    }

    @Test
    fun commentFieldShowsLabelPlaceholderAndCounter() {
        showReasonStep()

        rule.onNodeWithText("Additional Comments (Optional)").assertExists()
        rule.onNodeWithText("Help us understand better...").assertExists()
        rule.onNodeWithText("0/450").assertExists()
    }

    @Test
    fun commentCounterTracksLength() {
        showReasonStep(comment = "Moved")

        rule.onNodeWithText("5/450").assertExists()
    }

    @Test
    fun typingReportsTheComment() {
        showReasonStep()

        rule.onNodeWithTag(CANCEL_COMMENT_TAG).performScrollTo().performTextInput("Hi")

        assertEquals("Hi", typed)
    }

    @Test
    fun continueButtonReportsContinue() {
        showReasonStep()

        rule.onNodeWithText("Continue").performScrollTo().performClick()

        assertEquals(1, continueCount)
    }

    // endregion

    // region Task 1: confirm step

    private fun showConfirmStep(
        refund: RefundPreview = RefundPreview.HoldReleased,
        isSubmitting: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                CancellationConfirmStep(
                    summary = summary,
                    refund = refund,
                    isSubmitting = isSubmitting,
                    onKeepBooking = { keepCount++ },
                    onConfirm = { confirmCount++ },
                    zone = zone,
                    timeFormat = time12,
                    locale = Locale.US,
                )
            }
        }
    }

    @Test
    fun confirmStepShowsHeadingBodyExpertAndWhen() {
        showConfirmStep()

        rule
            .onNodeWithText("Confirm Cancellation?")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Please review the booking details below before confirming.").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Machine Learning Coach").assertExists()
        rule.onNodeWithText("Oct 24, 2026, 2:00 PM – 3:00 PM").assertExists()
    }

    @Test
    fun holdReleasedShowsTheHoldLineAndNoNote() {
        showConfirmStep(RefundPreview.HoldReleased)

        rule.onNodeWithText("The hold on your card will be released").assertExists()
        rule.onAllNodesWithText("Refunds typically appear", substring = true).assertCountEquals(0)
    }

    @Test
    fun fullRefundShowsAmountAndNote() {
        showConfirmStep(RefundPreview.Full(5000, "CAD"))

        rule.onNodeWithText("Full refund: CA$50.00", substring = true).assertExists()
        rule
            .onNodeWithText("Refunds typically appear in your account within 3–5 business days.")
            .assertExists()
    }

    @Test
    fun noRefundWithin24hShowsItsLine() {
        showConfirmStep(RefundPreview.NoRefundWithin24h)

        rule.onNodeWithText("No refund — cancelling within 24 hours of the session").assertExists()
        rule.onAllNodesWithText("Refunds typically appear", substring = true).assertCountEquals(0)
    }

    @Test
    fun policyFallbackShowsItsLine() {
        showConfirmStep(RefundPreview.PolicyFallback)

        rule.onNodeWithText("Any refund follows our cancellation policy.").assertExists()
    }

    @Test
    fun noneShowsNoRefundLines() {
        showConfirmStep(RefundPreview.None)

        rule.onAllNodesWithText("hold on your card", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Full refund", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("No refund", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Any refund", substring = true).assertCountEquals(0)
    }

    @Test
    fun keepAndConfirmButtonsReportClicks() {
        showConfirmStep()

        rule.onNodeWithTag(CANCEL_KEEP_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(CANCEL_CONFIRM_TAG).performScrollTo().performClick()

        assertEquals(1, keepCount)
        assertEquals(1, confirmCount)
    }

    @Test
    fun submittingDisablesBothButtonsAndAnnouncesCancelling() {
        showConfirmStep(isSubmitting = true)

        rule.onNodeWithTag(CANCEL_KEEP_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(CANCEL_CONFIRM_TAG).assertIsNotEnabled()
        rule
            .onNodeWithTag(CANCEL_CONFIRM_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Cancelling"))
    }

    @Test
    fun idleButtonsAreEnabled() {
        showConfirmStep()

        rule.onNodeWithTag(CANCEL_KEEP_TAG).assertIsEnabled()
        rule.onNodeWithTag(CANCEL_CONFIRM_TAG).assertIsEnabled()
    }

    // endregion

    // region Task 2: screen

    private var backCount = 0
    private var retryCount = 0
    private var backToReasonCount = 0
    private var dismissErrorCount = 0
    private var finishedCount = 0
    private var navBacks = 0

    private val loaded = ClientCancellationUiState(load = CancellationLoad.Loaded(summary))

    private fun showScreen(
        state: ClientCancellationUiState,
        dispatcherSink: (OnBackPressedDispatcher?) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                dispatcherSink(LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher)
                // Outer handler, like NavDisplay's: counts Back presses that reach navigation.
                BackHandler { navBacks++ }
                ClientCancellationContent(
                    state = state,
                    onBack = { backCount++ },
                    onRetry = { retryCount++ },
                    onSelectReason = { selectedReason = it },
                    onCommentChange = { typed = it },
                    onContinue = { continueCount++ },
                    onBackToReason = { backToReasonCount++ },
                    onConfirm = { confirmCount++ },
                    onDismissError = { dismissErrorCount++ },
                    onFinished = { finishedCount++ },
                    zone = zone,
                    timeFormat = time12,
                    locale = Locale.US,
                )
            }
        }
    }

    @Test
    fun appBarShowsTitleAndBackArrow() {
        showScreen(loaded)

        rule.onNodeWithText("Cancel Booking").assertExists()
        rule.onNode(hasContentDescriptionExactly("Back")).assertIsEnabled()
    }

    @Test
    fun loadingShowsLoadingDescription() {
        showScreen(ClientCancellationUiState())

        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun failedShowsMessageAndRetry() {
        showScreen(ClientCancellationUiState(load = CancellationLoad.Failed(FieldError.Text("Boom"))))

        rule.onNodeWithText("Couldn't load booking").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun cappedReasonStep() {
        showScreen(loaded)
        val field = rule.onNodeWithTag(CANCEL_COMMENT_TAG).getUnclippedBoundsInRoot()
        assertTrue("field width ${field.right - field.left}", field.right - field.left <= 600.dp)
        assertTrue("field left ${field.left}", field.left >= 150.dp)
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun cappedConfirmStep() {
        showScreen(loaded.copy(step = CancelStep.Confirm, refund = RefundPreview.HoldReleased))
        val keep = rule.onNodeWithTag(CANCEL_KEEP_TAG).getUnclippedBoundsInRoot()
        assertTrue("keep width ${keep.right - keep.left}", keep.right - keep.left <= 600.dp)
        assertTrue("keep left ${keep.left}", keep.left >= 150.dp)
    }

    @Test
    fun confirmNeverAutoConfirmsAfterRestore() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            ClickMeTheme {
                ClientCancellationContent(
                    state = loaded.copy(step = CancelStep.Confirm, refund = RefundPreview.HoldReleased),
                    onBack = { backCount++ },
                    onRetry = { retryCount++ },
                    onSelectReason = { selectedReason = it },
                    onCommentChange = { typed = it },
                    onContinue = { continueCount++ },
                    onBackToReason = { backToReasonCount++ },
                    onConfirm = { confirmCount++ },
                    onDismissError = { dismissErrorCount++ },
                    onFinished = { finishedCount++ },
                    zone = zone,
                    timeFormat = time12,
                    locale = Locale.US,
                )
            }
        }
        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        assertEquals(0, confirmCount)
        assertEquals(0, finishedCount)
        // The confirm step is back and still waits for the tap.
        rule.onNodeWithText("Confirm Cancellation?").assertExists()
        rule.onNodeWithTag(CANCEL_CONFIRM_TAG).assertIsEnabled()
    }

    @Test
    fun reasonStepIsShownForTheReasonStep() {
        showScreen(loaded)

        rule.onNodeWithText("Why are you cancelling?").assertExists()
        rule.onAllNodesWithText("Confirm Cancellation?").assertCountEquals(0)
    }

    @Test
    fun confirmStepIsShownForTheConfirmStep() {
        showScreen(loaded.copy(step = CancelStep.Confirm, refund = RefundPreview.HoldReleased))

        rule.onNodeWithText("Confirm Cancellation?").assertExists()
        rule.onAllNodesWithText("Why are you cancelling?").assertCountEquals(0)
    }

    @Test
    fun appBarBackOnReasonStepPops() {
        showScreen(loaded)

        rule.onNode(hasContentDescriptionExactly("Back")).performClick()

        assertEquals(1, backCount)
        assertEquals(0, backToReasonCount)
    }

    @Test
    fun appBarBackOnConfirmStepReturnsToReason() {
        showScreen(loaded.copy(step = CancelStep.Confirm))

        rule.onNode(hasContentDescriptionExactly("Back")).performClick()

        assertEquals(1, backToReasonCount)
        assertEquals(0, backCount)
    }

    @Test
    fun keepBookingPopsOneScreen() {
        showScreen(loaded.copy(step = CancelStep.Confirm))

        rule.onNodeWithTag(CANCEL_KEEP_TAG).performScrollTo().performClick()

        assertEquals(1, backCount)
    }

    @Test
    fun systemBackOnConfirmStepReturnsToReason() {
        var dispatcher: OnBackPressedDispatcher? = null
        showScreen(loaded.copy(step = CancelStep.Confirm)) { dispatcher = it }

        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()

        assertEquals(1, backToReasonCount)
        assertEquals(0, navBacks)
    }

    @Test
    fun systemBackOnReasonStepReachesNavigation() {
        var dispatcher: OnBackPressedDispatcher? = null
        showScreen(loaded) { dispatcher = it }

        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()

        assertEquals(1, navBacks)
        assertEquals(0, backToReasonCount)
    }

    @Test
    fun whileSubmittingBackIsSwallowedAndTheArrowIsDisabled() {
        var dispatcher: OnBackPressedDispatcher? = null
        showScreen(loaded.copy(step = CancelStep.Confirm, isSubmitting = true)) { dispatcher = it }

        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()

        assertEquals(0, backToReasonCount)
        assertEquals(0, backCount)
        assertEquals(0, navBacks)
        rule.onNode(hasContentDescriptionExactly("Back")).assertIsNotEnabled()
    }

    @Test
    fun errorDialogShowsTitleAndMessageAndDismisses() {
        showScreen(
            loaded.copy(
                step = CancelStep.Confirm,
                error = CancelErrorDialog(FieldError.Res(R.string.cancel_booking_error_not_cancellable), true),
            ),
        )

        rule.onNodeWithText("Couldn't cancel booking").assertExists()
        rule.onNodeWithText("This booking can no longer be cancelled.").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissErrorCount)
    }

    @Test
    fun finishedCallsOnFinishedExactlyOnce() {
        showScreen(loaded.copy(step = CancelStep.Confirm, isSubmitting = true, isFinished = true))
        rule.waitForIdle()

        assertEquals(1, finishedCount)
    }

    @Test
    fun notFinishedDoesNotCallOnFinished() {
        showScreen(loaded)
        rule.waitForIdle()

        assertEquals(0, finishedCount)
    }

    // endregion
}
