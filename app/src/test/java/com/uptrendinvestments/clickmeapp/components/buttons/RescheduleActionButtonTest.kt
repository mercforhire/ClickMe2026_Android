package com.uptrendinvestments.clickmeapp.components.buttons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.uptrendinvestments.clickmeapp.components.buttonstyles.ActionButtonPair
import com.uptrendinvestments.clickmeapp.components.buttonstyles.RescheduleActionButton
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h800dp")
class RescheduleActionButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private var clicks = 0

    private fun pair(
        fontScale: Float = 1f,
        containerWidth: Int? = null,
        start: Boolean = true,
        end: Boolean = true,
    ) {
        rule.setContent {
            ClickMeTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    val sized = if (containerWidth != null) Modifier.width(containerWidth.dp) else Modifier
                    Box(sized) {
                        ActionButtonPair(
                            start = if (start) ({ Box(it.testTag("start")) { Text("Start") } }) else null,
                            end = if (end) ({ Box(it.testTag("end")) { Text("End") } }) else null,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Stub(
        tag: String,
        width: Int,
        modifier: Modifier,
    ) {
        Box(modifier.testTag(tag)) { Spacer(Modifier.width(width.dp).height(20.dp)) }
    }

    /** Slots are fixed-width stubs so the fit rule is tested without any font. The container is 340 dp: half = 162 dp. */
    private fun stubPair(
        startWidth: Int,
        endWidth: Int,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ) {
        rule.setContent {
            ClickMeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Box(Modifier.width(340.dp)) {
                        ActionButtonPair(
                            start = { Stub("start", startWidth, it) },
                            end = { Stub("end", endWidth, it) },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun labelAndClick() {
        rule.setContent { ClickMeTheme { RescheduleActionButton(onClick = { clicks++ }) } }

        rule.onNodeWithText("Reschedule").assertHeightIsAtLeast(56.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun contentDescriptionOverride() {
        rule.setContent {
            ClickMeTheme {
                RescheduleActionButton(
                    onClick = { clicks++ },
                    contentDescription = "Reschedule session with Ada Lovelace",
                )
            }
        }

        rule.onNodeWithContentDescription("Reschedule session with Ada Lovelace").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun disabledIgnoresClicks() {
        rule.setContent { ClickMeTheme { RescheduleActionButton(onClick = { clicks++ }, enabled = false) } }

        rule.onNodeWithText("Reschedule").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun pairSideBySideAtNormalScale() {
        pair()

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertEquals(start.top.value, end.top.value, 1f)
        assertTrue(end.left >= start.right)
        assertEquals(start.right.value - start.left.value, end.right.value - end.left.value, 1f)
    }

    @Test
    fun pairStacksAtLargeFont() {
        pair(fontScale = 1.5f)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertTrue(end.top >= start.bottom)
    }

    @Test
    fun pairStacksWhenNarrow() {
        pair(containerWidth = 300)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertTrue(end.top >= start.bottom)
    }

    @Test
    fun pairStaysARowWhenBothFitTheirHalf() {
        stubPair(100, 100)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertEquals(start.top.value, end.top.value, 1f)
        assertTrue(end.left >= start.right)
        assertEquals(start.right.value - start.left.value, end.right.value - end.left.value, 1f)
    }

    @Test
    fun pairStaysARowJustUnderTheHalf() {
        stubPair(161, 161)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertEquals(start.top.value, end.top.value, 1f)
    }

    @Test
    fun pairStacksWhenStartCannotFitItsHalf() {
        stubPair(163, 100)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertTrue(end.top >= start.bottom)
        assertEquals(340f, start.width.value, 1f)
        assertEquals(340f, end.width.value, 1f)
    }

    @Test
    fun pairStacksWhenEndCannotFitItsHalf() {
        stubPair(100, 163)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertTrue(end.top >= start.bottom)
        assertEquals(340f, start.width.value, 1f)
        assertEquals(340f, end.width.value, 1f)
    }

    @Test
    fun pairRowMirrorsInRtl() {
        stubPair(100, 100, LayoutDirection.Rtl)

        val start = rule.onNodeWithTag("start").getUnclippedBoundsInRoot()
        val end = rule.onNodeWithTag("end").getUnclippedBoundsInRoot()
        assertEquals(start.top.value, end.top.value, 1f)
        assertTrue(start.left >= end.right)
    }

    @Test
    fun singleChildFillsWidth() {
        pair(containerWidth = 360, start = false)

        rule.onNodeWithTag("end").assertWidthIsEqualTo(360.dp)
        rule.onNodeWithTag("start").assertDoesNotExist()
    }

    @Test
    fun bothNullRendersNothing() {
        pair(start = false, end = false)

        rule.onNodeWithTag("start").assertDoesNotExist()
        rule.onNodeWithTag("end").assertDoesNotExist()
    }
}
