package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_EMPTY_ILLUSTRATION_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_EXPERT_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_SORT_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.searchChipTag
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
    private val queries = mutableListOf<String>()
    private val categoryPicks = mutableListOf<String>()
    private val sortPicks = mutableListOf<SearchSort>()

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
                    onQueryChange = { queries += it },
                    onCategorySelected = { categoryPicks += it },
                    onSortSelected = { sortPicks += it },
                    onExpertClick = { clicked += it },
                    onRefresh = { refreshes++ },
                    onRetry = { retries++ },
                    onRefreshFailureConsumed = { refreshFailureConsumed++ },
                )
            }
        }
    }

    private fun loaded(vararg rows: SearchExpertUi) = SearchExpertUiState(results = SearchResults.Loaded(rows.toList()))

    private fun hasVisibleText(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

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
        assertTrue(!hasVisibleText("Law"))
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

    private val categories =
        listOf(
            SearchCategoryChipUi(slug = "business", name = "Business"),
            SearchCategoryChipUi(slug = "design", name = "Design"),
        )

    @Test
    fun controlsStayVisibleWhileLoadingFailedAndEmpty() {
        var state by mutableStateOf(SearchExpertUiState(categories = categories, results = SearchResults.Loading))
        rule.setContent {
            ClickMeTheme {
                SearchExpertContent(
                    state = state,
                    onQueryChange = {},
                    onCategorySelected = {},
                    onSortSelected = {},
                    onExpertClick = {},
                    onRefresh = {},
                    onRetry = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
        fun assertControls() {
            rule.onNodeWithTag(SEARCH_FIELD_TAG).assertIsDisplayed()
            rule.onNodeWithTag(searchChipTag(ALL_CATEGORIES_SLUG)).assertIsDisplayed()
            rule.onNodeWithText("Sort by").assertIsDisplayed()
        }

        assertControls()
        rule.onAllNodesWithTag(SEARCH_SKELETON_CARD_TAG, useUnmergedTree = true).assertCountEquals(3)

        rule.runOnIdle { state = state.copy(results = SearchResults.Failed(FieldError.Text("Boom"))) }
        assertControls()
        rule.onNodeWithTag(SEARCH_ERROR_STATE_TAG).assertExists()

        rule.runOnIdle { state = state.copy(results = SearchResults.Loaded(emptyList())) }
        assertControls()
        rule.onNodeWithText("No Experts Found").assertExists()
    }

    @Test
    fun headerOrderMatchesIos() {
        show(loaded(ada).copy(categories = categories))

        val field = rule.onNodeWithTag(SEARCH_FIELD_TAG).fetchSemanticsNode().boundsInRoot.top
        val chip = rule.onNodeWithTag(searchChipTag(ALL_CATEGORIES_SLUG)).fetchSemanticsNode().boundsInRoot.top
        val sort = rule.onNodeWithText("Sort by").fetchSemanticsNode().boundsInRoot.top
        val card = rule.onAllNodesWithTag(SEARCH_EXPERT_CARD_TAG).onFirst().fetchSemanticsNode().boundsInRoot.top
        assertTrue("field above chips", field < chip)
        assertTrue("chips above sort", chip < sort)
        assertTrue("sort above first card", sort < card)
    }

    @Test
    fun controlsReachTheirCallbacks() {
        show(loaded(ada).copy(categories = categories))

        rule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("ada")
        assertEquals("ada", queries.last())

        rule.onNodeWithText("Design").performClick()
        assertEquals(listOf("design"), categoryPicks)

        rule.onNodeWithTag(SEARCH_SORT_BUTTON_TAG).performClick()
        rule
            .onNode(hasText("Highest rated") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf(SearchSort.HighestRated), sortPicks)
    }

    @Test
    fun fullScreenPassesTheA11yAudit() {
        show(loaded(ada, anonymous).copy(categories = categories))
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }
}
