package com.uptrendinvestments.clickmeapp.screens.splash

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private lateinit var motion: SplashMotionValues

    private fun showMotion(animationsEnabled: Boolean) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ClickMeTheme {
                motion = rememberSplashMotion(animationsEnabled)
                SplashContent(motion)
            }
        }
    }

    private fun advance(ms: Long) = rule.mainClock.advanceTimeBy(ms)

    @Test
    fun logoEntranceTimings() {
        showMotion(animationsEnabled = true)

        assertEquals(0f, motion.logoAlpha, 0.001f)
        assertEquals(SPLASH_LOGO_START_SCALE, motion.logoScale, 0.001f)

        advance(350)
        assertTrue("alpha ${motion.logoAlpha}", motion.logoAlpha > 0.05f && motion.logoAlpha < 0.95f)
        assertTrue("scale ${motion.logoScale}", motion.logoScale > SPLASH_LOGO_START_SCALE && motion.logoScale < 1f)

        advance(400)
        assertEquals(1f, motion.logoAlpha, 0.001f)
        assertEquals(1f, motion.logoScale, 0.001f)
    }

    @Test
    fun textWaitsThenFades() {
        showMotion(animationsEnabled = true)

        advance(SPLASH_TEXT_DELAY_MS.toLong() - 1)
        assertEquals(0f, motion.textAlpha, 0.001f)

        advance(100)
        assertTrue("alpha ${motion.textAlpha}", motion.textAlpha > 0f)

        advance(600)
        assertEquals(1f, motion.textAlpha, 0.001f)
    }

    @Test
    fun glowPulsesInsideItsRange() {
        showMotion(animationsEnabled = true)

        val alphas = mutableListOf<Float>()
        val scales = mutableListOf<Float>()
        repeat(17) {
            alphas += motion.glowAlpha
            scales += motion.glowScale
            advance(200)
        }

        assertTrue(alphas.all { it in SPLASH_GLOW_ALPHA_MIN - 0.001f..SPLASH_GLOW_ALPHA_MAX + 0.001f })
        assertTrue(scales.all { it in SPLASH_GLOW_SCALE_MIN - 0.001f..SPLASH_GLOW_SCALE_MAX + 0.001f })
        assertTrue("alphas $alphas", alphas.min() < 0.30f && alphas.max() > 0.43f)
        assertTrue("scales $scales", scales.min() < 0.97f && scales.max() > 1.03f)
    }

    @Test
    fun reducedMotionShowsTheFinalFrame() {
        showMotion(animationsEnabled = false)

        fun assertFinal() {
            assertEquals(1f, motion.logoAlpha, 0.001f)
            assertEquals(1f, motion.logoScale, 0.001f)
            assertEquals(1f, motion.textAlpha, 0.001f)
            assertEquals(SPLASH_GLOW_ALPHA_STATIC, motion.glowAlpha, 0.001f)
            assertEquals(1f, motion.glowScale, 0.001f)
        }
        assertFinal()
        advance(3_200)
        assertFinal()
    }

    private fun animationsEnabledWith(scale: Float?): Boolean {
        if (scale != null) {
            Settings.Global.putFloat(rule.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        }
        var enabled: Boolean? = null
        rule.setContent { enabled = rememberAnimationsEnabled() }
        rule.waitForIdle()
        return checkNotNull(enabled)
    }

    @Test
    fun animatorScaleZeroIsReducedMotion() {
        assertFalse(animationsEnabledWith(0f))
    }

    @Test
    fun animatorScaleOneKeepsAnimationsOn() {
        assertTrue(animationsEnabledWith(1f))
    }

    @Test
    fun missingAnimatorScaleKeepsAnimationsOn() {
        assertTrue(animationsEnabledWith(null))
    }

    @Test
    fun recreationDoesNotReplayTheEntrance() {
        rule.mainClock.autoAdvance = false
        val tester = StateRestorationTester(rule)
        tester.setContent {
            ClickMeTheme {
                motion = rememberSplashMotion(true)
                SplashContent(motion)
            }
        }
        advance(800)
        val textBefore = motion.textAlpha
        assertTrue("text should be mid-fade, was $textBefore", textBefore > 0f && textBefore < 1f)

        tester.emulateSavedInstanceStateRestore()
        advance(16)

        assertEquals(1f, motion.logoAlpha, 0.001f)
        assertTrue(motion.textAlpha >= textBefore)
    }
}
