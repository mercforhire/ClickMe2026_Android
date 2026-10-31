package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_CLEAR_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SEARCH_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews.SearchExpertSearchBar
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
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
}
