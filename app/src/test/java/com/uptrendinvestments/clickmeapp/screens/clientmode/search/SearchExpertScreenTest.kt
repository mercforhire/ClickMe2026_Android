package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_EMPTY_ILLUSTRATION_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_EXPERT_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.searchExpertUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class SearchExpertScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val clicked = mutableListOf<SearchExpertUi>()
    private var refreshes = 0
    private var retries = 0
    private var refreshFailureConsumed = 0

    private val ada =
        searchExpertUi(
            id = "e1",
            name = "Ada Lovelace",
            headline = "Engineer",
            rating = 4.5,
            tags = listOf("AI", "Math", "Tax", "Law"),
        )
    private val anonymous =
        searchExpertUi(id = "e2", name = null, headline = "", rating = 0.0, tags = emptyList())

    private fun show(state: SearchExpertUiState) {
        rule.setContent {
            ClickMeTheme {
                SearchExpertContent(
                    state = state,
                    onExpertClick = { clicked += it },
                    onRefresh = { refreshes++ },
                    onRetry = { retries++ },
                    onRefreshFailureConsumed = { refreshFailureConsumed++ },
                )
            }
        }
    }

    private fun loaded(vararg rows: SearchExpertUi) = SearchExpertUiState(results = SearchResults.Loaded(rows.toList()))

    private fun hasText(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun loadedRowsRenderAsIosCards() {
        show(loaded(ada, anonymous))

        rule.onAllNodesWithTag(SEARCH_EXPERT_CARD_TAG, useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("4.5").assertExists()
        rule.onNodeWithText("Engineer").assertExists()
        rule.onNodeWithText("AI").assertExists()
        rule.onNodeWithText("Math").assertExists()
        rule.onNodeWithText("Tax").assertExists()
        assertTrue(!hasText("Law"))
        rule.onNodeWithText("Expert").assertExists()
        rule.onNodeWithText("0.0").assertExists()
    }

    @Test
    fun bioShownOnlyWhenNonBlank() {
        show(loaded(searchExpertUi(id = "e1", bio = "Teaches ML"), searchExpertUi(id = "e2", bio = "")))

        rule.onNodeWithText("Teaches ML").assertExists()
        assertEquals(1, rule.onAllNodesWithText("Teaches ML").fetchSemanticsNodes().size)
    }

    @Test
    fun cardTapReportsTheRow() {
        show(loaded(ada, anonymous))

        rule.onAllNodesWithTag(SEARCH_EXPERT_CARD_TAG).onFirst().performClick()

        assertEquals(listOf(ada), clicked)
    }

    @Test
    fun cardIsOneNamedButton() {
        show(loaded(ada))

        val card = rule.onAllNodesWithTag(SEARCH_EXPERT_CARD_TAG).onFirst()
        card.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        card.assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        val config = card.fetchSemanticsNode().config
        val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        listOf("Ada Lovelace", "Engineer", "Rated 4.5", "Expertise: AI, Math, Tax").forEach {
            assertTrue("'$it' missing from '$description'", description.contains(it))
        }
    }

    @Test
    fun loadingShowsThreeSkeletonCards() {
        show(SearchExpertUiState(results = SearchResults.Loading))

        rule.onAllNodesWithTag(SEARCH_SKELETON_CARD_TAG, useUnmergedTree = true).assertCountEquals(3)
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithTag(SEARCH_EXPERT_CARD_TAG).assertCountEquals(0)
    }

    @Test
    fun emptyResultsShowCopyAndNoButtons() {
        show(loaded())

        rule.onNodeWithText("No Experts Found").assertExists()
        rule.onNodeWithText("Try adjusting your search or picking a different category.").assertExists()
        rule
            .onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(SEARCH_EMPTY_STATE_TAG)))
            .assertCountEquals(0)
    }

    @Test
    fun failedShowsInlineErrorWithRetry() {
        show(SearchExpertUiState(results = SearchResults.Failed(FieldError.Text("Boom"))))

        rule.onNodeWithText("Couldn't load results").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun failedWithResourceMessageShowsTheResourceText() {
        show(SearchExpertUiState(results = SearchResults.Failed(FieldError.Res(R.string.error_state_body))))

        rule.onNodeWithText("Check your connection and try again.").assertExists()
    }

    @Test
    fun pullToRefreshReachesOnRefreshFromTheList() {
        show(loaded(ada, anonymous))

        rule.onNodeWithTag(SEARCH_RESULTS_LIST_TAG).performTouchInput { swipeDown() }
        rule.waitForIdle()

        assertTrue(refreshes > 0)
    }

    @Test
    fun pullToRefreshReachesOnRefreshFromTheEmptyState() {
        show(loaded())

        val scrollable = rule.onNodeWithTag(SEARCH_EMPTY_STATE_TAG).onParent()
        scrollable.assert(hasScrollAction())
        scrollable.performTouchInput { swipeDown() }
        rule.waitForIdle()

        assertTrue(refreshes > 0)
    }

    @Test
    fun emptyResultsShowTheGlowingIllustrationAboveTheCopy() {
        show(loaded())

        val illustration = rule.onNodeWithTag(SEARCH_EMPTY_ILLUSTRATION_TAG, useUnmergedTree = true)
        val node = illustration.fetchSemanticsNode()
        val title = rule.onNodeWithText("No Experts Found").fetchSemanticsNode()
        assertTrue(node.boundsInRoot.top < title.boundsInRoot.top)
        assertTrue(node.config.getOrNull(SemanticsProperties.ContentDescription) == null)
        assertTrue(node.config.getOrNull(SemanticsActions.OnClick) == null)
    }

    @Test
    fun refreshFailureShowsSnackbarAndConsumesOnce() {
        show(loaded(ada).copy(refreshFailed = true))
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").assertExists()
        assertEquals(1, refreshFailureConsumed)
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, refreshes)
        rule.onAllNodesWithTag(SEARCH_EXPERT_CARD_TAG).assertCountEquals(1)
    }

    @Test
    fun loadedAndFailedPassTheA11yAudit() {
        show(loaded(ada, anonymous))
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun failedPassesTheA11yAudit() {
        show(SearchExpertUiState(results = SearchResults.Failed(FieldError.Text("Boom"))))
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }
}
