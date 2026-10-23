package com.uptrendinvestments.clickmeapp.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentLifecycleCardTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private fun setCard(
        stage: PaymentLifecycleStage,
        sessionWhen: String?,
    ) {
        rule.setContent { ClickMeTheme { PaymentLifecycleCard(stage = stage, sessionWhen = sessionWhen) } }
    }

    @Test
    fun holdPlaced_marksOnlyFirstStepDone() {
        setCard(PaymentLifecycleStage.HoldPlaced, "Oct 24, 2:00 PM")
        rule
            .onNodeWithText("What happens next")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule
            .onNodeWithTag(paymentLifecycleStepTag(0), useUnmergedTree = false)
            .assert(hasStateDescription("Done"))
            .assert(hasText("A hold is placed on your card", substring = true))
            .assert(hasText("No charge yet — funds are just reserved.", substring = true))
        rule
            .onNodeWithTag(paymentLifecycleStepTag(1))
            .assert(hasText("Your session runs on Oct 24, 2:00 PM", substring = true))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        rule
            .onNodeWithTag(paymentLifecycleStepTag(2))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun upcoming_marksNoStepDone() {
        setCard(PaymentLifecycleStage.Upcoming, null)
        for (index in 0..2) {
            rule
                .onNodeWithTag(paymentLifecycleStepTag(index))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        }
    }

    @Test
    fun blankSessionWhen_usesFallbackTitle() {
        setCard(PaymentLifecycleStage.Upcoming, "  ")
        rule.onNodeWithText("Your session runs at the scheduled time", substring = true).assertIsDisplayed()
    }

    @Test
    fun chargeStep_showsTitleAndSubtitle() {
        setCard(PaymentLifecycleStage.Upcoming, null)
        rule
            .onNodeWithTag(paymentLifecycleStepTag(2))
            .assert(hasText("Your card is charged after the session", substring = true))
            .assert(hasText("Usually within 15 minutes of the session ending.", substring = true))
        rule.onNodeWithTag(PAYMENT_LIFECYCLE_CARD_TAG).assertIsDisplayed()
    }
}
