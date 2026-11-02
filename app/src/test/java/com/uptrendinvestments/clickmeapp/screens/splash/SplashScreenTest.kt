package com.uptrendinvestments.clickmeapp.screens.splash

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp")
class SplashScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsTheMarkWordmarkAndTagline() {
        rule.setContent { ClickMeTheme { SplashScreen(onComposed = {}) } }

        rule.onNodeWithText("ClickMe").assertIsDisplayed()
        rule.onNodeWithText("Book time with the best minds.").assertIsDisplayed()
    }

    @Test
    fun reportsComposedOnce() {
        var composed = 0
        var recompositions by mutableIntStateOf(0)
        rule.setContent {
            ClickMeTheme {
                // Reading the state forces the parent to recompose with a fresh lambda each time.
                val tick = recompositions
                SplashScreen(onComposed = { if (tick >= 0) composed++ })
            }
        }
        rule.waitForIdle()
        assertEquals(1, composed)

        rule.runOnUiThread { recompositions++ }
        rule.waitForIdle()

        assertEquals(1, composed)
    }
}
