package com.uptrendinvestments.clickmeapp.screens.splash

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * BUG-11: while the mark fades in, its alpha layer rendered offscreen and clipped the glow to the 190 dp box, so the
 * glow was a hard-edged square until the fade ended. The glow reaches 160 dp from the center, so a pixel 125 dp out is
 * inside the glow and outside that box: it must be tinted during the fade, as it is at full alpha.
 *
 * `w400dp-h900dp` is mdpi, so 1 px = 1 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp")
class SplashGlowRegressionTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val logoAlpha = mutableFloatStateOf(1f)
    private val glowAlpha = mutableFloatStateOf(1f)

    private fun show() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val motion =
                SplashMotionValues(
                    logoAlphaState = logoAlpha,
                    logoScaleState = mutableFloatStateOf(1f),
                    textAlphaState = mutableFloatStateOf(0f),
                    glowAlphaState = glowAlpha,
                    glowScaleState = mutableFloatStateOf(1f),
                )
            ClickMeTheme { SplashContent(motion) }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    /** The pixel 125 dp to the right of the logo center, outside the 190 dp box and inside the 160 dp glow. */
    private fun pixelBesideTheBox(
        logo: Float,
        glow: Float,
    ): Color {
        logoAlpha.floatValue = logo
        glowAlpha.floatValue = glow
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val box = rule.onNodeWithTag(SPLASH_LOGO_TAG, useUnmergedTree = true).getBoundsInRoot()
        val x = (box.left.value + box.right.value) / 2f + 125f
        val y = (box.top.value + box.bottom.value) / 2f
        val image = rule.onNodeWithTag(SPLASH_SCREEN_TAG).captureToImage().toPixelMap()
        return image[x.toInt(), y.toInt()]
    }

    private fun greenDifference(
        a: Color,
        b: Color,
    ) = abs(a.green - b.green) * 255f

    @Test
    fun theGlowIsNotClippedToTheLogoBoxWhileTheMarkFadesIn() {
        show()
        val background = pixelBesideTheBox(logo = 1f, glow = 0f)
        val atFullAlpha = pixelBesideTheBox(logo = 1f, glow = 1f)
        val midFade = pixelBesideTheBox(logo = 0.5f, glow = 1f)

        assertTrue("the glow tints the pixel at full alpha", greenDifference(atFullAlpha, background) >= 8f)
        assertTrue(
            "the glow still tints it at half alpha: $midFade vs background $background",
            greenDifference(midFade, background) >= 4f,
        )
        assertEquals(
            "half alpha is about half the tint",
            greenDifference(atFullAlpha, background) / 2f,
            greenDifference(midFade, background),
            3f,
        )
    }
}
