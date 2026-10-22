package com.uptrendinvestments.clickmeapp.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class PriceTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun free_rendersFree() {
        rule.setContent { ClickMeTheme { PriceText(PriceDisplay.Free) } }
        rule.onNodeWithText("Free").assertIsDisplayed()
    }

    @Test
    fun paid_rendersFormattedText() {
        rule.setContent { ClickMeTheme { PriceText(PriceDisplay.Paid("$50.00")) } }
        rule.onNodeWithText("$50.00").assertIsDisplayed()
    }

    @Test
    fun unknown_rendersDash() {
        rule.setContent { ClickMeTheme { PriceText(PriceDisplay.Unknown) } }
        rule.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun of_explicitFreeWins() {
        assertEquals(PriceDisplay.Free, PriceDisplay.of(true, 5000, "USD", Locale.US))
        assertEquals(PriceDisplay.Free, PriceDisplay.of(true, null, null))
    }

    @Test
    fun of_paidWhenAmountAndCurrencyPresent() {
        assertEquals(PriceDisplay.Paid("$50.00"), PriceDisplay.of(false, 5000, "USD", Locale.US))
        assertEquals(PriceDisplay.Paid("$50.00"), PriceDisplay.of(null, 5000, "USD", Locale.US))
    }

    @Test
    fun of_unknownWhenAmountOrCurrencyMissing() {
        assertEquals(PriceDisplay.Unknown, PriceDisplay.of(null, null, null))
        assertEquals(PriceDisplay.Unknown, PriceDisplay.of(false, null, "USD"))
        assertEquals(PriceDisplay.Unknown, PriceDisplay.of(false, 5000, null))
    }
}
