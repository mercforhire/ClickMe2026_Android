package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelErrorDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_COMMENT_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_CONTINUE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_REASON_ROW_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.ExpertCancelConfirmStep
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.label
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExpertCancellationScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()
    private val launchRequest = ExpertLaunchRequest()
    private val clock = MutableClock(Instant.parse("2026-10-23T12:00:00Z"))
    private var cancelledCount = 0
    private var backCount = 0

    private fun stub(
        fee: Double? = null,
        refundAmount: Double = 0.0,
        currency: String? = "USD",
    ) {
        api.expertBookingDetailsHandler = { expertBookingDetail(fee = fee) }
        api.cancelExpertBookingHandler = { _, _ -> CancelBookingData(refundAmount, currency) }
    }

    private fun showScreen(handle: SavedStateHandle = SavedStateHandle()) {
        val viewModel =
            ExpertCancellationViewModel(
                ExpertCancelKey("b1", ExpertBookingOrigin.Bookings),
                handle,
                ExpertBookingsRepository(api),
                clock,
                launchRequest,
                FakeCallGuard(),
            )
        rule.setContent {
            ClickMeTheme {
                ExpertCancellationScreen(
                    viewModel = viewModel,
                    onBack = { backCount++ },
                    onCancelled = { cancelledCount++ },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun pick(label: String) {
        rule.onNodeWithText(label).performClick()
    }

    private fun goToConfirm() {
        rule.onNodeWithTag(EXPERT_CANCEL_CONTINUE_TAG).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun confirm() {
        rule.onNodeWithTag(EXPERT_CANCEL_CONFIRM_TAG).performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test
    fun cancelEndToEnd() {
        stub()
        showScreen()

        rule.onNodeWithText("Why are you cancelling?").assertExists()
        rule.onNodeWithTag(EXPERT_CANCEL_CONTINUE_TAG).assertIsNotEnabled()

        pick("Scheduling Conflict")
        rule.onNodeWithTag(EXPERT_CANCEL_COMMENT_TAG).performScrollTo().performTextInput("Double booked")
        rule.onNodeWithTag(EXPERT_CANCEL_CONTINUE_TAG).assertIsEnabled()
        goToConfirm()

        rule.onNodeWithText("Confirm Cancellation?").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        confirm()

        assertEquals(
            listOf("b1" to CancelBookingRequest("Scheduling Conflict: Double booked")),
            api.cancelExpertBookingRequests,
        )
        assertEquals(
            BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled),
            launchRequest.bookings.consume(),
        )
        assertEquals(1, cancelledCount)
    }

    @Test
    fun labelOnlyWhenCommentBlank() {
        stub()
        showScreen()

        pick("Other")
        rule.onNodeWithTag(EXPERT_CANCEL_COMMENT_TAG).performScrollTo().performTextInput("   ")
        goToConfirm()
        confirm()

        assertEquals(listOf("b1" to CancelBookingRequest("Other")), api.cancelExpertBookingRequests)
    }

    @Test
    fun reasonsAreTheFourIosLabels() {
        stub()
        showScreen()

        listOf("Personal Emergency", "Scheduling Conflict", "Technical Issues", "Other").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        ExpertCancelReason.entries.forEach {
            rule.onNodeWithTag(EXPERT_CANCEL_REASON_ROW_TAG_PREFIX + it.name).assertIsNotSelected()
        }
    }

    // region Task 2: refund line

    private fun toConfirmStep() {
        pick("Other")
        goToConfirm()
    }

    @Test
    fun paidBookingShowsHoldReleaseLine() {
        stub(fee = 75.0)
        showScreen()

        toConfirmStep()

        rule.onNodeWithText("The client's payment hold will be released.").assertExists()
        rule.onAllNodesWithText("won't be refunded", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("full refund", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Refunds typically appear", substring = true).assertCountEquals(0)
    }

    @Test
    fun freeBookingShowsNoRefundLine() {
        stub(fee = null)
        showScreen()

        toConfirmStep()

        rule.onAllNodesWithText("payment hold", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("refund", substring = true).assertCountEquals(0)
    }

    @Test
    fun serverRefundBecomesNotice() {
        stub(fee = 75.0, refundAmount = 75.0, currency = "USD")
        showScreen()

        toConfirmStep()
        confirm()

        assertEquals(
            BookingsLaunch(BookingsTab.Past, BookingsNotice.CancelledWithRefund("$75.00")),
            launchRequest.bookings.consume(),
        )
    }

    private fun showConfirmStep(refund: ExpertRefundPreview) {
        rule.setContent {
            ClickMeTheme {
                ExpertCancelConfirmStep(
                    summary = summary,
                    refund = refund,
                    isSubmitting = false,
                    onKeepBooking = {},
                    onConfirm = {},
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
    }

    private val summary =
        ExpertCancelSummary(
            clientName = "Ada Lovelace",
            clientAvatarUrl = null,
            topic = "Algorithms",
            start = Instant.parse("2026-10-24T18:00:00Z"),
            end = Instant.parse("2026-10-24T19:00:00Z"),
        )

    @Test
    fun dormantFullRefundRowShowsAmountAndNote() {
        showConfirmStep(ExpertRefundPreview.ClientFullRefund(75.0, "USD"))

        rule.onNodeWithText("Your client will receive a full refund of $75.00", substring = true).assertExists()
        rule
            .onNodeWithText("Refunds typically appear in the client's account within 3–5 business days.")
            .assertExists()
    }

    @Test
    fun dormantNoRefundRowShowsItsLineAndNoNote() {
        showConfirmStep(ExpertRefundPreview.ClientNoRefundWithin24h)

        rule
            .onNodeWithText("Your client won't be refunded — cancelling within 24 hours of the session")
            .assertExists()
        rule.onAllNodesWithText("Refunds typically appear", substring = true).assertCountEquals(0)
    }

    @Test
    fun dormantPolicyRowNeverStatesAnAmount() {
        showConfirmStep(ExpertRefundPreview.PolicyFallback)

        rule.onNodeWithText("Any refund follows our cancellation policy.").assertExists()
    }

    // endregion

    @Test
    fun errorDialogShowsTitleAndMessageAndDismisses() {
        var dismissed = 0
        rule.setContent {
            ClickMeTheme {
                ExpertCancellationContent(
                    state =
                        ExpertCancellationUiState(
                            load = ExpertCancelLoad.Loaded(summary),
                            step = CancelStep.Confirm,
                            reason = ExpertCancelReason.Other,
                            error =
                                CancelErrorDialog(
                                    FieldError.Res(R.string.cancel_booking_error_not_cancellable),
                                    returnToBookings = true,
                                ),
                        ),
                    onBack = {},
                    onRetry = {},
                    onSelectReason = {},
                    onCommentChange = {},
                    onContinue = {},
                    onBackToReason = {},
                    onConfirm = {},
                    onDismissError = { dismissed++ },
                    onFinished = {},
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }

        rule.onNodeWithText("Couldn't cancel booking").assertExists()
        rule.onNodeWithText("This booking can no longer be cancelled.").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }

    // region Phase 15: state cells, restoration, large window

    private fun showContent(
        state: ExpertCancellationUiState,
        onRetry: () -> Unit = {},
        onConfirm: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertCancellationContent(
                    state = state,
                    onBack = { backCount++ },
                    onRetry = onRetry,
                    onSelectReason = {},
                    onCommentChange = {},
                    onContinue = {},
                    onBackToReason = {},
                    onConfirm = onConfirm,
                    onDismissError = {},
                    onFinished = {},
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
    }

    @Test
    fun stateCells() {
        showContent(ExpertCancellationUiState())
        // First load: one polite "Loading" skeleton, no step content yet.
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithText("Why are you cancelling?").assertDoesNotExist()
    }

    @Test
    fun failedLoadShowsNamedErrorWithRetry() {
        var retries = 0
        showContent(
            ExpertCancellationUiState(load = ExpertCancelLoad.Failed(FieldError.Text("Offline"))),
            onRetry = { retries++ },
        )

        rule.onNodeWithText("Couldn't load booking").assertExists()
        rule.onNodeWithText("Offline").assertExists()
        rule.onNodeWithContentDescription("Back").assertIsEnabled()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun confirmNeverAutoConfirmsAfterRestore() {
        stub(fee = 75.0)
        val handle =
            SavedStateHandle(
                mapOf(
                    ExpertCancellationViewModel.KEY_STEP to CancelStep.Confirm.name,
                    ExpertCancellationViewModel.KEY_REASON to ExpertCancelReason.Other.name,
                ),
            )
        showScreen(handle)

        // The process-death restore lands on the confirm step and waits for a tap.
        rule.onNodeWithText("Confirm Cancellation?").assertExists()
        rule.onNodeWithTag(EXPERT_CANCEL_CONFIRM_TAG).assertIsEnabled()
        assertEquals(0, api.cancelExpertBookingCalls)
        assertEquals(emptyList<Pair<String, CancelBookingRequest>>(), api.cancelExpertBookingRequests)
        assertEquals(0, cancelledCount)
        assertEquals(null, launchRequest.bookings.consume())

        confirm()
        assertEquals(1, api.cancelExpertBookingCalls)
        assertEquals(1, cancelledCount)
    }

    @Test
    fun confirmStepRecreatedDoesNotCallConfirm() {
        var confirms = 0
        val restorer = StateRestorationTester(rule)
        val state =
            ExpertCancellationUiState(
                load = ExpertCancelLoad.Loaded(summary),
                step = CancelStep.Confirm,
                reason = ExpertCancelReason.Other,
            )
        restorer.setContent {
            ClickMeTheme {
                ExpertCancellationContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onSelectReason = {},
                    onCommentChange = {},
                    onContinue = {},
                    onBackToReason = {},
                    onConfirm = { confirms++ },
                    onDismissError = {},
                    onFinished = {},
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        rule.waitForIdle()

        restorer.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule.onNodeWithText("Confirm Cancellation?").assertExists()
        assertEquals(0, confirms)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        stub()
        showScreen()

        val reasonStep = rule.onNodeWithTag(EXPERT_CANCEL_CONTINUE_TAG).getUnclippedBoundsInRoot()
        val reasonWidth = reasonStep.right.value - reasonStep.left.value
        assertTrue("Continue is $reasonWidth dp wide, expected <= 600", reasonWidth <= 600f)

        pick("Other")
        goToConfirm()
        val confirmButton = rule.onNodeWithTag(EXPERT_CANCEL_CONFIRM_TAG).getUnclippedBoundsInRoot()
        val confirmWidth = confirmButton.right.value - confirmButton.left.value
        assertTrue("Confirm is $confirmWidth dp wide, expected <= 600", confirmWidth <= 600f)
    }

    // endregion

    @Test
    fun reasonLabelsEqualTheWireLabels() {
        var labels: List<String> = emptyList()
        rule.setContent {
            ClickMeTheme { labels = ExpertCancelReason.entries.map { it.label() } }
        }
        rule.waitForIdle()

        assertEquals(ExpertCancelReason.entries.map { it.wireLabel }, labels)
    }
}
