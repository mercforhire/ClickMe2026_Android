package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingPaymentInfoLink
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.PAYMENT_INFO_LINK_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.PaymentInfoSheetBody
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentInfoSheetTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun body_showsIntroLifecycleStepsAndGotIt_andGotItCallsBackOnce() {
        var gotIt = 0
        rule.setContent { ClickMeTheme { PaymentInfoSheetBody(onGotIt = { gotIt++ }) } }
        rule
            .onNodeWithText(
                "You pay exactly the session price — no additional fees. Here's when your card is charged.",
            ).assertIsDisplayed()
        rule.onNodeWithText("What happens next").assertIsDisplayed()
        rule.onNodeWithText("A hold is placed on your card", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Your session runs at the scheduled time", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Your card is charged after the session", substring = true).assertIsDisplayed()
        rule.onAllNodesWithText("Done", substring = true).assertCountEquals(0)
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Got it"))
        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, gotIt)
    }

    @Test
    fun body_neverShowsAmountOrTotal() {
        rule.setContent { ClickMeTheme { PaymentInfoSheetBody(onGotIt = {}) } }
        rule.onAllNodesWithText("total", substring = true, ignoreCase = true).assertCountEquals(0)
        rule.onAllNodesWithText("$", substring = true).assertCountEquals(0)
    }

    @Test
    fun link_isButton_isTallEnough_andClicksOnce() {
        var clicks = 0
        rule.setContent { ClickMeTheme { MakeABookingPaymentInfoLink(onClick = { clicks++ }) } }
        rule.onNodeWithText("How does payment work?").assertIsDisplayed()
        rule
            .onNodeWithTag(PAYMENT_INFO_LINK_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, clicks)
    }
}
