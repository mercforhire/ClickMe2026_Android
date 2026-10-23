package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileMenuRowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun row_showsTitle_isButton_isTallEnough_andClicksOnce() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                ProfileMenuRow(title = "Payment Methods", icon = Icons.Outlined.CreditCard, onClick = { clicks++ })
            }
        }
        rule
            .onNodeWithText("Payment Methods")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(56.dp)
            .performClick()
        assertEquals(1, clicks)
    }
}
