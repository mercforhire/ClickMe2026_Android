package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_CLEAR_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_SORT_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertCategoryChips
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertSearchBar
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertSortRow
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.searchChipTag
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Search tab header controls (16.1-05): the search field, the category chip row and the sort row. */
@RunWith(RobolectricTestRunner::class)
class SearchExpertControlsTest {
    @get:Rule
    val compose = createComposeRule()

    // region Search field

    @Test
    fun typingReportsEveryChange() {
        val changes = mutableListOf<String>()
        compose.setContent {
            ClickMeTheme {
                var q by remember { mutableStateOf("") }
                SearchExpertSearchBar(
                    query = q,
                    onQueryChange = {
                        q = it
                        changes += it
                    },
                )
            }
        }

        compose.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("ada")

        compose.onNodeWithTag(SEARCH_FIELD_TAG).assertTextContains("ada")
        assertEquals("ada", changes.last())
    }

    @Test
    fun placeholderShownWhenEmpty() {
        compose.setContent { ClickMeTheme { SearchExpertSearchBar(query = "", onQueryChange = {}) } }

        compose.onNodeWithText("Search for experts").assertExists()
    }

    @Test
    fun clearOnlyWithTextAndClears() {
        val changes = mutableListOf<String>()
        var query by mutableStateOf("")
        compose.setContent {
            ClickMeTheme { SearchExpertSearchBar(query = query, onQueryChange = { changes += it }) }
        }
        compose.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        compose.onNodeWithTag(SEARCH_CLEAR_TAG).assertDoesNotExist()

        compose.runOnIdle { query = "ada" }
        compose.onNodeWithContentDescription("Clear search").assertExists()
        compose.onNodeWithTag(SEARCH_CLEAR_TAG).performClick()

        assertEquals(listOf(""), changes)
    }

    @Test
    fun imeSearchHidesTheKeyboard() {
        compose.setContent {
            ClickMeTheme {
                var q by remember { mutableStateOf("") }
                // A second focusable, as on the real tab: with the field as the only focusable, Compose hands the
                // focus straight back to it once the field lets go.
                Column {
                    Button(onClick = {}) { Text("Other focusable") }
                    SearchExpertSearchBar(query = q, onQueryChange = { q = it })
                }
            }
        }

        compose.onNodeWithTag(SEARCH_FIELD_TAG).performClick()
        compose.onNodeWithTag(SEARCH_FIELD_TAG).assertIsFocused()

        compose.onNodeWithTag(SEARCH_FIELD_TAG).performImeAction()
        compose.waitForIdle()

        compose.onNodeWithTag(SEARCH_FIELD_TAG).assertIsNotFocused()
    }

    @Test
    fun searchFieldPassesTheA11yAudit() {
        compose.setContent { ClickMeTheme { SearchExpertSearchBar(query = "ada", onQueryChange = {}) } }

        compose.assertActionablesNamed()
        compose.assertTouchTargets()
    }

    // endregion

    // region Category chips

    private val categories =
        listOf(
            SearchCategoryChipUi(slug = "business", name = "Business"),
            SearchCategoryChipUi(slug = "design", name = "Design"),
        )

    @Test
    fun allChipFirstThenCategoriesInOrder() {
        compose.setContent {
            ClickMeTheme {
                Box(Modifier.requiredWidth(800.dp)) {
                    SearchExpertCategoryChips(
                        categories = categories,
                        selectedSlug = ALL_CATEGORIES_SLUG,
                        onSelect = {},
                    )
                }
            }
        }

        val allLeft = compose.onNodeWithText("All").getUnclippedBoundsInRoot().left
        val businessLeft = compose.onNodeWithText("Business").getUnclippedBoundsInRoot().left
        val designLeft = compose.onNodeWithText("Design").getUnclippedBoundsInRoot().left
        assertTrue("All before Business", allLeft < businessLeft)
        assertTrue("Business before Design", businessLeft < designLeft)
        compose.onNodeWithTag(searchChipTag(ALL_CATEGORIES_SLUG)).assertIsSelected()
        compose.onNodeWithTag(searchChipTag("business")).assertIsNotSelected()
        compose.onNodeWithTag(searchChipTag("design")).assertIsNotSelected()
    }

    @Test
    fun selectedSlugSelectsExactlyThatChip() {
        compose.setContent {
            ClickMeTheme {
                SearchExpertCategoryChips(categories = categories, selectedSlug = "design", onSelect = {})
            }
        }

        compose.onNodeWithTag(searchChipTag("design")).assertIsSelected()
        compose.onNodeWithTag(searchChipTag("business")).assertIsNotSelected()
        compose.onNodeWithTag(searchChipTag(ALL_CATEGORIES_SLUG)).assertIsNotSelected()
    }

    @Test
    fun tappingAChipReportsItsSlug() {
        val selected = mutableListOf<String>()
        compose.setContent {
            ClickMeTheme {
                SearchExpertCategoryChips(
                    categories = categories,
                    selectedSlug = "business",
                    onSelect = { selected += it },
                )
            }
        }

        compose.onNodeWithTag(searchChipTag("design")).performClick()
        compose.onNodeWithTag(searchChipTag(ALL_CATEGORIES_SLUG)).performClick()

        assertEquals(listOf("design", ""), selected)
    }

    @Test
    fun noCategoriesShowsOnlyAll() {
        compose.setContent {
            ClickMeTheme {
                SearchExpertCategoryChips(
                    categories = emptyList(),
                    selectedSlug = ALL_CATEGORIES_SLUG,
                    onSelect = {},
                )
            }
        }

        compose.onNodeWithText("All").assertExists()
        compose.onNodeWithText("Business").assertDoesNotExist()
        compose.onNodeWithText("Design").assertDoesNotExist()
    }

    @Test
    fun chipsPassTheA11yAudit() {
        compose.setContent {
            ClickMeTheme {
                SearchExpertCategoryChips(categories = categories, selectedSlug = "business", onSelect = {})
            }
        }

        compose.assertActionablesNamed()
        compose.assertTouchTargets()
    }

    // endregion

    // region Sort row

    /** A menu item is the only node with a text and a `selected` flag; the trigger shows the same text unflagged. */
    private fun menuItem(text: String) = hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun sortRowShowsLabelAndCurrentOption() {
        compose.setContent {
            ClickMeTheme { SearchExpertSortRow(sort = SearchSort.MostPopular, onSortSelected = {}) }
        }

        compose.onNodeWithText("Sort by").assertExists()
        compose.onNodeWithText("Most popular").assertExists()
        compose
            .onNodeWithTag(SEARCH_SORT_BUTTON_TAG)
            .assertHasClickAction()
            .assertContentDescriptionEquals("Sort by Most popular")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun menuListsBothOptionsInIosOrderAndMarksTheCurrent() {
        compose.setContent {
            ClickMeTheme { SearchExpertSortRow(sort = SearchSort.MostPopular, onSortSelected = {}) }
        }

        compose.onNodeWithTag(SEARCH_SORT_BUTTON_TAG).performClick()

        val popularTop = compose.onNode(menuItem("Most popular")).getUnclippedBoundsInRoot().top
        val ratedTop = compose.onNode(menuItem("Highest rated")).getUnclippedBoundsInRoot().top
        assertTrue("Most popular above Highest rated", popularTop < ratedTop)
        compose.onNode(menuItem("Most popular")).assertIsSelected()
        compose.onNode(menuItem("Highest rated")).assertIsNotSelected()
    }

    @Test
    fun pickingAnOptionReportsItAndClosesTheMenu() {
        val picked = mutableListOf<SearchSort>()
        compose.setContent {
            ClickMeTheme {
                SearchExpertSortRow(sort = SearchSort.MostPopular, onSortSelected = { picked += it })
            }
        }
        compose.onNodeWithTag(SEARCH_SORT_BUTTON_TAG).performClick()

        compose.onNode(menuItem("Highest rated")).performClick()
        compose.waitForIdle()

        assertEquals(listOf(SearchSort.HighestRated), picked)
        compose.onNodeWithText("Highest rated").assertDoesNotExist()
    }

    // endregion
}
