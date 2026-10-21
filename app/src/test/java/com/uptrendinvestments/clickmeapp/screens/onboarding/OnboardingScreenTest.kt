package com.uptrendinvestments.clickmeapp.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val WELCOME_TITLE = "Welcome to ClickMe"
private const val WELCOME_SUBTITLE = "Connect with top experts in your field for personalized advice and guidance."
private const val DISCOVER_TITLE = "Discover Top Experts"
private const val LAST_TITLE = "Seamless Communication"

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp")
class OnboardingScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var finishes = 0

    private fun setCarousel() {
        rule.setContent { ClickMeTheme { OnboardingContent(onFinish = { finishes++ }) } }
    }

    private fun pressSystemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun pageOne_showsCopyNextSkipAndDots() {
        setCarousel()
        rule.onNodeWithText(WELCOME_TITLE).assertIsDisplayed()
        rule.onNodeWithText(WELCOME_SUBTITLE).assertIsDisplayed()
        rule.onNodeWithText("Next").assertIsDisplayed()
        rule.onNodeWithText("Skip").assertIsDisplayed()
        rule.onNodeWithContentDescription("Page 1 of 4").assertExists()
    }

    @Test
    fun next_advancesToPageTwo() {
        setCarousel()
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(DISCOVER_TITLE).assertIsDisplayed()
        rule.onNodeWithContentDescription("Page 2 of 4").assertExists()
    }

    @Test
    fun lastPage_showsGetStartedAndStillShowsSkip() {
        setCarousel()
        repeat(3) {
            rule.onNodeWithText("Next").performClick()
            rule.waitForIdle()
        }
        rule.onNodeWithText(LAST_TITLE).assertIsDisplayed()
        rule.onNodeWithText("Get Started").assertIsDisplayed()
        rule.onNodeWithText("Skip").assertIsDisplayed()
        rule.onNodeWithContentDescription("Page 4 of 4").assertExists()
    }

    @Test
    fun getStarted_onLastPage_finishesOnce() {
        setCarousel()
        repeat(3) {
            rule.onNodeWithText("Next").performClick()
            rule.waitForIdle()
        }
        rule.onNodeWithText("Get Started").performClick()
        assertEquals(1, finishes)
    }

    @Test
    fun skip_onFirstPage_finishesOnce() {
        setCarousel()
        rule.onNodeWithText("Skip").performClick()
        assertEquals(1, finishes)
    }

    @Test
    fun systemBack_onPageTwo_returnsToPageOneWithoutFinishing() {
        setCarousel()
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText(DISCOVER_TITLE).assertIsDisplayed()
        pressSystemBack()
        rule.onNodeWithText(WELCOME_TITLE).assertIsDisplayed()
        assertEquals(0, finishes)
    }

    @Test
    fun systemBack_onPageOne_isNotHandledByCarousel() {
        setCarousel()
        rule.waitForIdle()
        var hasEnabledCallbacks = true
        rule.runOnUiThread { hasEnabledCallbacks = rule.activity.onBackPressedDispatcher.hasEnabledCallbacks() }
        assertFalse(hasEnabledCallbacks)
        assertEquals(0, finishes)
    }

    @Test
    fun swipeLeft_movesToPageTwo() {
        setCarousel()
        rule.onNodeWithTag(ONBOARDING_PAGER_TAG).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithText(DISCOVER_TITLE).assertIsDisplayed()
        rule.onNodeWithContentDescription("Page 2 of 4").assertExists()
    }
}
