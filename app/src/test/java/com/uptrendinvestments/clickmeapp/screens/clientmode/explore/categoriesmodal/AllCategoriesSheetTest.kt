package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.categoriesmodal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.PICKER_ROW_LEADING_ICON_TAG
import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AllCategoriesSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val loaded =
        CategoryCatalog.Loaded(
            listOf(CategoryItem(id = "ai", name = "AI"), CategoryItem(id = "design", name = "Design")),
        )

    private fun show(
        state: CategoriesSheetUiState,
        onToggle: (String) -> Unit = {},
        onClear: () -> Unit = {},
        onApply: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                AllCategoriesSheetContent(
                    state = state,
                    onQueryChange = {},
                    onToggle = onToggle,
                    onClear = onClear,
                    onApply = onApply,
                    onRetry = onRetry,
                )
            }
        }
    }

    @Test
    fun loadedCatalogShowsTitleSearchAndCheckboxRowsWithIcons() {
        show(CategoriesSheetUiState(catalog = loaded, draft = setOf("ai")))

        rule.onNodeWithText("All Categories").assertIsDisplayed()
        rule.onNodeWithText("Search categories...").assertIsDisplayed()
        rule.onNodeWithText("AI").assertIsOn()
        rule.onNodeWithText("Design").assertIsOff()
        val icons = rule.onAllNodesWithTag(PICKER_ROW_LEADING_ICON_TAG, useUnmergedTree = true)
        assertEquals(2, icons.fetchSemanticsNodes().size)
    }

    @Test
    fun clickingRowClearAndApplyInvokeCallbacks() {
        val toggled = mutableListOf<String>()
        var cleared = 0
        var applied = 0
        show(
            CategoriesSheetUiState(catalog = loaded),
            onToggle = { toggled += it },
            onClear = { cleared++ },
            onApply = { applied++ },
        )

        rule.onNodeWithText("Design").performClick()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithText("Apply").performClick()

        assertEquals(listOf("design"), toggled)
        assertEquals(1, cleared)
        assertEquals(1, applied)
    }

    @Test
    fun loadingCatalogShowsLoadingCaption() {
        show(CategoriesSheetUiState(catalog = CategoryCatalog.Loading))

        rule.onNodeWithText("Loading categories…").assertIsDisplayed()
    }

    @Test
    fun failedCatalogShowsErrorWithRetryAndKeepsFooter() {
        var retries = 0
        show(CategoriesSheetUiState(catalog = CategoryCatalog.Failed), onRetry = { retries++ })

        rule.onNodeWithText("Couldn't load categories").assertIsDisplayed()
        rule.onNodeWithText("Check your connection and try again.").assertIsDisplayed()
        rule.onNodeWithText("Apply").assertIsDisplayed()
        rule.onNodeWithText("Clear").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun noMatchShowsHeadingWithQueryAndBody() {
        show(CategoriesSheetUiState(catalog = loaded, query = "zzz"))

        rule.onNodeWithText("No categories match “zzz”").assertIsDisplayed()
        rule.onNodeWithText("Try a different search term.").assertIsDisplayed()
    }
}
