package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.PaymentMethodRow
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.paymentMethodMenuTag
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.paymentMethodRowTag
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val visa = PaymentMethodItem("pm_1", "visa", "4242", 9, 2028, isDefault = true)
    private val master = PaymentMethodItem("pm_2", "mastercard", "4444", 12, 2031, isDefault = false)

    private val setDefaults = mutableListOf<String>()
    private val removals = mutableListOf<String>()

    private fun setRow(
        card: PaymentMethodItem,
        busy: CardOperationKind? = null,
        actionsEnabled: Boolean = true,
    ) {
        rule.setContent {
            ClickMeTheme {
                PaymentMethodRow(
                    card = card,
                    busy = busy,
                    actionsEnabled = actionsEnabled,
                    onSetDefault = { setDefaults += card.id },
                    onRemove = { removals += card.id },
                )
            }
        }
    }

    @Test
    fun showsBrandLastFourExpiryAndDefaultBadge() {
        setRow(visa)

        rule.onNodeWithTag(paymentMethodRowTag("pm_1")).assertExists()
        rule.onNodeWithText("Visa •••• 4242", substring = true).assertExists()
        rule.onNodeWithText("Expires 09/28", substring = true).assertExists()
        rule.onAllNodesWithText("Default", substring = true).assertCountEquals(1)
    }

    @Test
    fun nonDefaultRowHasNoBadge() {
        setRow(master)

        rule.onAllNodesWithText("Default", substring = true).assertCountEquals(0)
    }

    @Test
    fun rowIsOneMergedAccessibilityLabel() {
        setRow(visa)

        rule.onNodeWithContentDescription("Visa ending 4242, expires 09/28, default card").assertExists()
    }

    @Test
    fun nonDefaultRowLabelOmitsTheDefaultClause() {
        setRow(master)

        rule.onNodeWithContentDescription("Mastercard ending 4444, expires 12/31").assertExists()
    }

    @Test
    fun missingLastFourStillRendersBullets() {
        setRow(PaymentMethodItem("pm_3", "visa", null, 1, 2030, isDefault = false))

        rule.onNodeWithText("Visa ••••", substring = true).assertExists()
    }

    @Test
    fun missingExpiryOmitsTheExpiresLine() {
        setRow(PaymentMethodItem("pm_3", "visa", "1111", null, null, isDefault = false))

        rule.onAllNodesWithText("Expires", substring = true).assertCountEquals(0)
    }

    @Test
    fun defaultCardMenuOffersOnlyRemove() {
        setRow(visa)

        rule.onNodeWithTag(paymentMethodMenuTag("pm_1")).performClick()

        rule.onNodeWithText("Remove").assertExists()
        rule.onNodeWithText("Set as default").assertDoesNotExist()
    }

    @Test
    fun nonDefaultMenuSetAsDefaultCallsBackOnceAndClosesTheMenu() {
        setRow(master)

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).performClick()
        rule.onNodeWithText("Remove").assertExists()
        rule.onNodeWithText("Set as default").performClick()

        assertEquals(listOf("pm_2"), setDefaults)
        assertEquals(emptyList<String>(), removals)
        rule.onNodeWithText("Set as default").assertDoesNotExist()
    }

    @Test
    fun removeMenuItemCallsBackOnce() {
        setRow(master)

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).performClick()
        rule.onNodeWithText("Remove").performClick()

        assertEquals(listOf("pm_2"), removals)
        assertEquals(emptyList<String>(), setDefaults)
    }

    @Test
    fun busyRemoveShowsSpinnerInsteadOfMenuButton() {
        setRow(master, busy = CardOperationKind.Remove, actionsEnabled = false)

        rule.onNodeWithTag(paymentMethodMenuTag("pm_2")).assertDoesNotExist()
        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Removing"))
            .assertExists()
    }

    @Test
    fun busySetDefaultAnnouncesUpdating() {
        setRow(master, busy = CardOperationKind.SetDefault, actionsEnabled = false)

        rule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Updating"))
            .assertExists()
    }

    @Test
    fun menuButtonIsDisabledWhenAnotherRowIsBusy() {
        setRow(visa, actionsEnabled = false)

        rule.onNodeWithTag(paymentMethodMenuTag("pm_1")).assertIsNotEnabled()
    }

    @Test
    fun menuButtonIsEnabledWhenIdle() {
        setRow(visa)

        rule.onNodeWithTag(paymentMethodMenuTag("pm_1")).assertIsEnabled()
    }
}
