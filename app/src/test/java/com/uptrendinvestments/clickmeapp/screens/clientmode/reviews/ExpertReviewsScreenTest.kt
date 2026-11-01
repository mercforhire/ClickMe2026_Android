@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.reviewItem
import com.uptrendinvestments.clickmeapp.testing.reviewsPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A tall window composes every lazy item at once.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class ExpertReviewsScreenTest {
    private val main = MainDispatcherRule(UnconfinedTestDispatcher())
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(main).around(composeRule)

    private val rule get() = composeRule

    private val api = FakeClickMeApi()
    private var backCount = 0

    private fun vm(name: String = "Ada Lovelace") =
        ExpertReviewsViewModel(ExpertReviewsKey("e1", name), ReviewsRepository(api))

    private fun show(viewModel: ExpertReviewsViewModel) {
        rule.setContent {
            ClickMeTheme { ExpertReviewsScreen(viewModel = viewModel, onBack = { backCount++ }) }
        }
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun titleUsesTheFirstName() {
        api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
        show(vm())
        rule.onNodeWithText("Ada's Reviews").assertExists()
        rule.assertIsHeading("Ada's Reviews")
    }

    @Test
    fun blankNameFallsBackToReviews() {
        api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
        show(vm("   "))
        rule.onNodeWithText("Reviews").assertExists()
        rule.assertIsHeading("Reviews")
    }

    @Test
    fun loadedReviewsRenderAsCards() {
        api.expertReviewsHandler = {
            reviewsPayload(
                4.5,
                2,
                listOf(
                    reviewItem(),
                    reviewItem(id = "r2", name = null, topic = null, dateLabel = null, comment = null),
                ),
            )
        }
        show(vm())
        rule.onNodeWithText("Sam Lee").assertExists()
        rule.onNodeWithText("Topic: Career advice").assertExists()
        rule.onNodeWithText("Oct 12, 2026").assertExists()
        rule.onNodeWithText("Great session").assertExists()
        rule.onNodeWithText("Anonymous").assertExists()
        assertEquals(1, rule.onAllNodesWithText("Topic:", substring = true).fetchSemanticsNodes().size)
    }

    private fun shownText(text: String) =
        rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun overallCardShowsRatingStarsAndCount() {
        api.expertReviewsHandler = { reviewsPayload(4.5, 12, listOf(reviewItem())) }
        show(vm())
        assertTrue(shownText("Overall Rating"))
        assertTrue(shownText("4.5"))
        assertTrue(shownText("12 Total Reviews"))
        val overallTop = rule.onNodeWithText("Overall Rating", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        val cardTop = rule.onNodeWithText("Sam Lee", useUnmergedTree = true).getUnclippedBoundsInRoot().top
        assertTrue(overallTop < cardTop)
    }

    @Test
    fun singularCount() {
        api.expertReviewsHandler = { reviewsPayload(5.0, 1, listOf(reviewItem())) }
        show(vm())
        assertTrue(shownText("1 Total Review"))
    }

    @Test
    fun nullOverallReadsZero() {
        api.expertReviewsHandler = { reviewsPayload(null, 0) }
        show(vm())
        assertTrue(shownText("0.0"))
        assertTrue(shownText("0 Total Reviews"))
    }

    @Test
    fun overallCardIsOneTalkBackNode() {
        api.expertReviewsHandler = { reviewsPayload(4.5, 12, listOf(reviewItem())) }
        show(vm())
        rule
            .onNodeWithContentDescription("Overall rating 4.5 out of 5, 12 Total Reviews")
            .assertContentDescriptionEquals("Overall rating 4.5 out of 5, 12 Total Reviews")
    }

    @Test
    fun backArrowCallsOnBack() {
        api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
        show(vm())
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }
}
