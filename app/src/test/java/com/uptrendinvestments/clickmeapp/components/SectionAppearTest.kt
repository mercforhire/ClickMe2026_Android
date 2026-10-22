package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SectionAppearTest {
    @get:Rule
    val rule = createComposeRule()

    private val sectionTag = "section"
    private val sectionHeight = 100.dp
    private val fullAnimationMs = SECTION_APPEAR_MS.toLong() * 2

    /** A lazy list with one [SectionAppear] item near the top and enough filler to scroll it out of composition. */
    private fun showList(): LazyListState {
        lateinit var listState: LazyListState
        rule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.height(300.dp)) {
                item(key = "top") { Box(Modifier.fillMaxWidth().height(50.dp)) }
                item(key = sectionTag) {
                    SectionAppear(Modifier.testTag(sectionTag)) {
                        Box(Modifier.fillMaxWidth().height(sectionHeight))
                    }
                }
                items(count = 40, key = { "filler-$it" }) { Box(Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        return listState
    }

    @Test
    fun firstCompositionExpandsIn() {
        rule.mainClock.autoAdvance = false
        showList()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val midHeight = rule.onNodeWithTag(sectionTag).getUnclippedBoundsInRoot().height
        assertTrue("expected a partial height mid-animation, was $midHeight", midHeight < sectionHeight)

        rule.mainClock.advanceTimeBy(fullAnimationMs)
        rule.onNodeWithTag(sectionTag).assertHeightIsEqualTo(sectionHeight)
    }

    @Test
    fun scrollingBackIntoViewDoesNotReplayTheAnimation() {
        val listState = showList()
        rule.mainClock.advanceTimeBy(fullAnimationMs)
        rule.onNodeWithTag(sectionTag).assertHeightIsEqualTo(sectionHeight)

        // Scroll far enough that the item leaves composition, not just the viewport.
        rule.runOnIdle { runBlocking { listState.scrollToItem(30) } }
        rule.waitForIdle()
        rule.onNodeWithTag(sectionTag).assertDoesNotExist()

        rule.mainClock.autoAdvance = false
        rule.runOnIdle { runBlocking { listState.scrollToItem(0) } }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Restored as already shown: full height on the first frames, with no expand from zero (WR-04).
        rule.onNodeWithTag(sectionTag).assertHeightIsEqualTo(sectionHeight)
    }
}
