package com.uptrendinvestments.clickmeapp.screens.login.passwordchanged

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val HEADLINE = "Password\nUpdated!"

/** Phase 15 UI-SPEC Global rule 8: Password Changed announces its headline first. */
@RunWith(RobolectricTestRunner::class)
class PasswordChangedScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun headlineFocusedOnEntry() {
        rule.setContent { ClickMeTheme { PasswordChangedScreen(onGoToLogin = {}) } }
        rule.waitForIdle()
        rule.onNodeWithText(HEADLINE).assertIsFocused()
        val config = rule.onNodeWithText(HEADLINE).fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.Heading))
    }

    @Test
    fun goToLoginStillWorks() {
        var taps = 0
        rule.setContent { ClickMeTheme { PasswordChangedScreen(onGoToLogin = { taps++ }) } }
        rule.waitForIdle()
        rule.onNodeWithText("Go to Login").performClick()
        rule.waitForIdle()
        assertEquals(1, taps)
    }
}
