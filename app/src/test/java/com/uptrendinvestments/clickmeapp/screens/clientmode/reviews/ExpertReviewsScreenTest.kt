@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.reviewItem
import com.uptrendinvestments.clickmeapp.testing.reviewsPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // Review cards merge into one TalkBack node, so visible text is found in the unmerged tree.
    private fun shownText(text: String) =
        rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun serverDown() = api.httpError(500, """{"status":"error","code":"INTERNAL","message":"Server down"}""")

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
        assertTrue(shownText("Sam Lee"))
        assertTrue(shownText("Topic: Career advice"))
        assertTrue(shownText("Oct 12, 2026"))
        assertTrue(shownText("Great session"))
        assertTrue(shownText("Anonymous"))
        val topicLines = rule.onAllNodesWithText("Topic:", substring = true, useUnmergedTree = true)
        assertEquals(1, topicLines.fetchSemanticsNodes().size)
    }

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

    @Test
    fun firstLoadShowsTheSkeleton() {
        val gate = CompletableDeferred<Unit>()
        api.expertReviewsHandler = {
            gate.await()
            reviewsPayload(4.5, 1, listOf(reviewItem()))
        }
        show(vm())
        assertEquals(1, rule.onAllNodesWithContentDescription("Loading").fetchSemanticsNodes().size)
        assertEquals(4, rule.onAllNodesWithTag(SKELETON_BLOCK_TAG, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertFalse(shownText("Sam Lee"))
    }

    @Test
    fun firstLoadErrorShowsErrorStateAndRetries() {
        var calls = 0
        api.expertReviewsHandler = {
            calls++
            if (calls == 1) throw serverDown()
            reviewsPayload(4.5, 1, listOf(reviewItem()))
        }
        show(vm())
        assertTrue(shownText("Couldn't load reviews"))
        assertTrue(shownText("Server down"))
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(2, calls)
        assertTrue(shownText("Sam Lee"))
    }

    @Test
    fun zeroReviewsShowsOverallCardAndNoReviewsYet() {
        api.expertReviewsHandler = { reviewsPayload(null, 0) }
        show(vm())
        assertTrue(shownText("0.0"))
        assertTrue(shownText("0 Total Reviews"))
        assertTrue(shownText("No reviews yet"))
        assertTrue(shownText("Client reviews will appear here after their sessions."))
    }

    @Test
    fun refreshFailureKeepsContentAndShowsTheSnackbar() {
        var consumed = 0
        val state =
            ExpertReviewsUiState(
                expertId = "e1",
                titleFirstName = "Ada",
                load =
                    ExpertReviewsLoad.Loaded(
                        4.5,
                        1,
                        listOf(ExpertReviewUi("r1", "Sam Lee", null, 5, "Career advice", "Oct 12, 2026", "Great")),
                    ),
                refreshFailed = true,
            )
        rule.setContent {
            ClickMeTheme {
                ExpertReviewsContent(
                    state = state,
                    onBack = {},
                    onRetry = {},
                    onRefresh = {},
                    onRefreshFailureConsumed = { consumed++ },
                )
            }
        }
        rule.waitForIdle()
        assertTrue(shownText("Sam Lee"))
        assertTrue(shownText("Couldn't refresh. Check your connection and try again."))
        assertTrue(shownText("Retry"))
        assertEquals(1, consumed)
    }

    @Test
    fun failedStateShowsTheServerMessageFromAResource() {
        val state =
            ExpertReviewsUiState("e1", "Ada", ExpertReviewsLoad.Failed(FieldError.Text("Nope")))
        rule.setContent {
            ClickMeTheme { ExpertReviewsContent(state, onBack = {}, onRetry = {}, onRefresh = {}, {}) }
        }
        assertTrue(shownText("Nope"))
    }

    @Test
    fun reviewCardDescriptionCarriesTheFullComment() {
        val long = "word ".repeat(80).trim()
        api.expertReviewsHandler = { reviewsPayload(5.0, 1, listOf(reviewItem(comment = long))) }
        show(vm())
        rule
            .onNodeWithContentDescription(
                "Sam Lee. Rated 5 out of 5. Topic: Career advice. Oct 12, 2026. $long",
            ).assertExists()
        val layouts = mutableListOf<TextLayoutResult>()
        rule
            .onNodeWithText(long, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layouts)
        assertTrue(layouts.single().lineCount <= 2)
    }

    @Test
    fun a11yAuditPasses() {
        api.expertReviewsHandler = { reviewsPayload(4.5, 1, listOf(reviewItem())) }
        show(vm())
        rule.assertTouchTargets()
        rule.assertActionablesNamed()
    }
}
