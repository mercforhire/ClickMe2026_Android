package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.uptrendinvestments.clickmeapp.screens.login.passwordchanged.PasswordChangedScreen
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val HEADLINE = "Password\nUpdated!"
private const val BODY = "Your password has been successfully updated. You can now use your new password to log in."
private const val ENTRANCE_DONE_MS = 1_500L

@RunWith(RobolectricTestRunner::class)
class AuthStaticScreensTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun passwordChanged_rendersHeadlineBodyAndButton() {
        rule.mainClock.autoAdvance = false
        rule.setContent { ClickMeTheme { PasswordChangedScreen(onGoToLogin = {}) } }
        rule.mainClock.advanceTimeBy(ENTRANCE_DONE_MS)
        rule.onNodeWithText(HEADLINE).assertIsDisplayed()
        rule.onNodeWithText(BODY).assertIsDisplayed()
        rule.onNodeWithText("Go to Login").assertIsDisplayed()
    }

    @Test
    fun passwordChanged_headlineIsAHeading() {
        rule.setContent { ClickMeTheme { PasswordChangedScreen(onGoToLogin = {}) } }
        rule.waitForIdle()
        val config = rule.onNodeWithText(HEADLINE).fetchSemanticsNode().config
        assertTrue(config.contains(SemanticsProperties.Heading))
    }

    @Test
    fun passwordChanged_hasNoBackArrow() {
        rule.setContent { ClickMeTheme { PasswordChangedScreen(onGoToLogin = {}) } }
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }

    @Test
    fun passwordChanged_goToLogin_callsCallbackOnce() {
        var taps = 0
        rule.setContent { ClickMeTheme { PasswordChangedScreen(onGoToLogin = { taps++ }) } }
        rule.waitForIdle()
        rule.onNodeWithText("Go to Login").performClick()
        rule.waitForIdle()
        assertEquals(1, taps)
    }

    @Test
    fun passwordChanged_entrance_runsOnceAndDoesNotReplayAfterRestoration() {
        var entrances = 0
        val restoration = StateRestorationTester(rule)
        rule.mainClock.autoAdvance = false
        restoration.setContent {
            ClickMeTheme { PasswordChangedScreen(onGoToLogin = {}, onEntranceStarted = { entrances++ }) }
        }
        rule.mainClock.advanceTimeBy(ENTRANCE_DONE_MS)
        assertEquals(1, entrances)

        restoration.emulateSavedInstanceStateRestore()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(HEADLINE).assertIsDisplayed()
        rule.onNodeWithText("Go to Login").assertIsDisplayed()
        assertEquals(1, entrances)
    }
}
