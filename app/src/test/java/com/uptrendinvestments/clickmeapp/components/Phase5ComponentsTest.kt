package com.uptrendinvestments.clickmeapp.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase5ComponentsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun count(tag: String) = rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun showStars(rating: Double) {
        rule.setContent { ClickMeTheme { RatingStars(rating = rating) } }
    }

    // RatingStars

    @Test
    fun ratingStars_3point4_has3FilledAnd2Empty() {
        showStars(3.4)
        assertEquals(3, count(RATING_STAR_FILLED_TAG))
        assertEquals(2, count(RATING_STAR_EMPTY_TAG))
    }

    @Test
    fun ratingStars_4point6_has5Filled() {
        showStars(4.6)
        assertEquals(5, count(RATING_STAR_FILLED_TAG))
        assertEquals(0, count(RATING_STAR_EMPTY_TAG))
    }

    @Test
    fun ratingStars_negative_hasNoFilled() {
        showStars(-1.0)
        assertEquals(0, count(RATING_STAR_FILLED_TAG))
        assertEquals(5, count(RATING_STAR_EMPTY_TAG))
    }

    @Test
    fun ratingStars_overFive_clampsTo5Filled() {
        showStars(7.0)
        assertEquals(5, count(RATING_STAR_FILLED_TAG))
    }

    @Test
    fun ratingStars_areDecorativeWithoutContentDescription() {
        showStars(3.0)
        val nodes =
            rule.onAllNodesWithTag(RATING_STAR_FILLED_TAG, useUnmergedTree = true).fetchSemanticsNodes() +
                rule.onAllNodesWithTag(RATING_STAR_EMPTY_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(5, nodes.size)
        nodes.forEach { assertFalse(it.config.contains(SemanticsProperties.ContentDescription)) }
    }

    // ExpertTagChip

    @Test
    fun expertTagChip_showsTextAndHasNoClickAction() {
        rule.setContent { ClickMeTheme { ExpertTagChip(label = "Machine Learning") } }
        rule.onNodeWithText("Machine Learning").assertIsDisplayed()
        rule.onNodeWithText("Machine Learning").assertHasNoClickAction()
    }

    // SkeletonBlock / SkeletonGroup

    @Test
    fun skeletonBlock_rendersAndInfinitePulseDoesNotHangTestClock() {
        rule.setContent { ClickMeTheme { SkeletonBlock() } }
        rule.waitForIdle()
        rule.onNodeWithTag(SKELETON_BLOCK_TAG).assertExists()
    }

    @Test
    fun skeletonGroup_exposesOneMergedLoadingDescription() {
        rule.setContent {
            ClickMeTheme {
                SkeletonGroup {
                    SkeletonBlock()
                    SkeletonBlock()
                    SkeletonBlock()
                }
            }
        }
        assertEquals(1, rule.onAllNodesWithContentDescription("Loading").fetchSemanticsNodes().size)
    }

    // SectionHeader

    @Test
    fun sectionHeader_titleIsHeading_actionInvokesCallbackAndIsAtLeast48dp() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                SectionHeader(title = "Trending Categories", actionLabel = "View all", onAction = { clicks++ })
            }
        }
        rule.onNodeWithText("Trending Categories").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        rule.onNodeWithText("View all").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("View all").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun sectionHeader_withoutAction_hasNoClickableNode() {
        rule.setContent { ClickMeTheme { SectionHeader(title = "Featured Topics") } }
        rule.onNodeWithText("Featured Topics").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)).assertDoesNotExist()
    }

    // SectionError

    @Test
    fun sectionError_showsMessageAndRetryInvokesCallback() {
        var retries = 0
        rule.setContent { ClickMeTheme { SectionError(message = "Boom", onRetry = { retries++ }) } }
        rule.onNodeWithText("Boom").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
