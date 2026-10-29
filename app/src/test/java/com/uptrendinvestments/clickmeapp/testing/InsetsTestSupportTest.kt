package com.uptrendinvestments.clickmeapp.testing

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class InsetsTestSupportTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private fun footerBounds() = rule.onNodeWithTag(FOOTER).getUnclippedBoundsInRoot()

    private fun footerBottom() = footerBounds().bottom.value

    @Test
    fun imeMovesImePaddedFooter() {
        rule.setContent {
            view = LocalView.current
            Box(Modifier.fillMaxSize().imePadding()) {
                Box(Modifier.align(Alignment.BottomStart).height(50.dp).testTag(FOOTER))
            }
        }
        assertEquals(800f, footerBottom(), TOLERANCE)

        rule.dispatchInsets(view, imeInsets(bottomPx = 300))

        assertEquals(500f, footerBottom(), TOLERANCE)
    }

    @Test
    fun cutoutPadsHorizontally() {
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            ) {
                Box(Modifier.fillMaxSize().testTag(FOOTER))
            }
        }

        rule.dispatchInsets(view, cutoutInsets(leftPx = 40, rightPx = 0))

        assertEquals(40f, footerBounds().left.value, TOLERANCE)
    }

    @Test
    fun combinedKeepsBoth() {
        rule.setContent {
            view = LocalView.current
            Box(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
                Box(Modifier.align(Alignment.BottomStart).height(50.dp).testTag(FOOTER))
            }
        }

        rule.dispatchInsets(view, combinedInsets(navigationBarInsets(bottomPx = 48), imeInsets(bottomPx = 300)))

        assertEquals(500f, footerBottom(), TOLERANCE)
    }

    private companion object {
        const val FOOTER = "footer"
        const val TOLERANCE = 0.5f
    }
}
