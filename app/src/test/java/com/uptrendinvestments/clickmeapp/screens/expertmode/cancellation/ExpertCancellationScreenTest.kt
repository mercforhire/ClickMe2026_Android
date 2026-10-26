package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_COMMENT_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_CONTINUE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.EXPERT_CANCEL_REASON_ROW_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.label
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

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

    private fun showScreen() {
        val viewModel =
            ExpertCancellationViewModel(
                ExpertCancelKey("b1", ExpertBookingOrigin.Bookings),
                SavedStateHandle(),
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
