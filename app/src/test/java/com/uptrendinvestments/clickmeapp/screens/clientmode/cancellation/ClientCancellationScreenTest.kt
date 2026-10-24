package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_COMMENT_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_KEEP_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CANCEL_REASON_ROW_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CancellationConfirmStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.CancellationReasonStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews.label
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
}
