package com.uptrendinvestments.clickmeapp.components.call

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JoinCallButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun enabled_showsLabel_clicksOnce_andHasNoStateDescription() {
        var clicks = 0
        rule.setContent { ClickMeTheme { JoinCallButton(enabled = true, onClick = { clicks++ }) } }

        rule
            .onNodeWithText("Join Call")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun disabled_isNotEnabled_andExplainsWhy() {
        var clicks = 0
        rule.setContent { ClickMeTheme { JoinCallButton(enabled = false, onClick = { clicks++ }) } }

        rule
            .onNodeWithText("Join Call")
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Available closer to the session start",
                ),
            )

        assertEquals(0, clicks)
    }
}
