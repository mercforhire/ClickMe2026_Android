package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientStartTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientTabs
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertStartTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertTabs
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BUG-12: at font scale 1.3 the selected expert tab label broke as "Dashboar" and "d". A tab label is one word and must
 * stay on one line and inside its tab at every font scale, down to 200% (Phase 15 font rule: no clipped text).
 *
 * Native graphics, so text is measured with the real font. `w360dp-h800dp` is the test phone, five tabs of 72 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class HomeTabScaffoldLabelRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun entry(key: NavKey): NavEntry<NavKey> = NavEntry(key = key) { Text("screen:$key") }

    private fun show(
        expert: Boolean,
        fontScale: Float,
    ) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = fontScale)) {
                ClickMeTheme {
                    val tabs = if (expert) expertTabs else clientTabs
                    val start = if (expert) expertStartTab else clientStartTab
                    val state = rememberNavigationState(start, tabs.map { it.key }.toSet())
                    val nav = remember { Navigator(state) }
                    HomeTabScaffold(tabs = tabs, navigationState = state, navigator = nav, entryProvider = ::entry)
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The label's text layout and the width of the node that holds it, in px. */
    private fun layoutOf(label: String): Pair<TextLayoutResult, Int> {
        // The label sits in the bar; the screen body only holds "screen:..." text, so an exact label match is the tab.
        val node =
            composeRule
                .onAllNodes(hasText(label), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .first { it.config.contains(SemanticsActions.GetTextLayoutResult) }
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.first() to node.size.width
    }

    /** One line, and that line fits the node: the word is whole and not drawn past its tab. */
    private fun problem(
        label: String,
        scale: Float,
    ): String? {
        val (layout, width) = layoutOf(label)
        val textWidth = layout.getLineRight(0) - layout.getLineLeft(0)
        return if (layout.lineCount != 1 || textWidth > width + 1f) {
            "$label at $scale: ${layout.lineCount} line(s), text ${textWidth}px in ${width}px"
        } else {
            null
        }
    }

    private fun assertLabelsStayWhole(
        labels: List<String>,
        scale: Float,
        expert: Boolean,
    ) {
        show(expert, scale)
        val results = labels.mapNotNull { problem(it, scale) }
        assertEquals("labels broke or overflowed: $results", emptyList<String>(), results)
    }

    @Test
    fun expertLabelsStayOnOneLineAtFontScale1Point3() {
        assertLabelsStayWhole(ExpertLabels, 1.3f, expert = true)
    }

    @Test
    fun expertLabelsStayOnOneLineAtTwoHundredPercent() {
        assertLabelsStayWhole(ExpertLabels, 2f, expert = true)
    }

    @Test
    fun clientLabelsStayOnOneLineAtTwoHundredPercent() {
        assertLabelsStayWhole(ClientLabels, 2f, expert = false)
    }

    @Test
    fun expertLabelsAreUnchangedAtDefaultFontScale() {
        show(expert = true, fontScale = 1f)
        assertEquals(null, problem("Dashboard", 1f))
        val style = layoutOf("Dashboard").first.layoutInput.style
        assertEquals(12f, style.fontSize.value, 0.01f)
    }

    private companion object {
        val ExpertLabels = listOf("Dashboard", "Bookings", "Topics", "Chats", "Profile")
        val ClientLabels = listOf("Explore", "Bookings", "Chats", "Profile")
    }
}
