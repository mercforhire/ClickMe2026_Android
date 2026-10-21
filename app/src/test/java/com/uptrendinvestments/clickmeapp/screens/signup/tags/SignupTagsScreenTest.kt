package com.uptrendinvestments.clickmeapp.screens.signup.tags

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A phone-sized window so the lazy list composes the rows and the Retry button below the search and chips.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SignupTagsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val tax = ExpertiseTagItem("t1", "Tax")
    private val law = ExpertiseTagItem("t2", "Law")
    private val audit = ExpertiseTagItem("t3", "Audit")
    private val loaded = TagCatalog.Loaded(listOf(tax, law, audit))

    private fun show(
        state: SignupTagsUiState,
        onQueryChange: (String) -> Unit = {},
        onSelect: (ExpertiseTagItem) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onRetry: () -> Unit = {},
        onSave: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                SignupTagsContent(
                    state = state,
                    onQueryChange = onQueryChange,
                    onSelect = onSelect,
                    onRemove = onRemove,
                    onRetry = onRetry,
                    onSave = onSave,
                    onBack = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    @Test
    fun emptySelectionShowsHintSuggestedHeaderAndCatalogRows() {
        show(SignupTagsUiState(catalog = loaded))

        rule.onNodeWithText("Expertise Tags").assertIsDisplayed()
        rule.onNodeWithText("Search for tags").assertIsDisplayed()
        rule.onNodeWithText("Pick at least one tag to publish.").assertIsDisplayed()
        rule.onNodeWithText("Suggested Tags").assertIsDisplayed()
        rule.onNodeWithText("Tax").assertIsDisplayed()
        rule.onNodeWithText("Law").assertIsDisplayed()
        rule.onAllNodesWithText("Selected Tags").assertCountEquals(0)
    }

    @Test
    fun selectedTagShowsChipHelperAndLeavesTheSuggestedRows() {
        show(SignupTagsUiState(selected = listOf(tax), catalog = loaded))

        rule.onNodeWithText("Selected Tags").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove Tax").assertIsDisplayed()
        rule.onNodeWithText("The first tag is your primary tag.").assertIsDisplayed()
        rule.onAllNodesWithText("Pick at least one tag to publish.").assertCountEquals(0)
        // Tax appears once, as the chip label; it is no longer a suggested row.
        rule.onAllNodesWithText("Tax", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun tappingASuggestedRowSelectsItAndTappingTheChipRemovesIt() {
        var selected: ExpertiseTagItem? = null
        var removed: String? = null
        show(
            SignupTagsUiState(selected = listOf(tax), catalog = loaded),
            onSelect = { selected = it },
            onRemove = { removed = it },
        )

        rule.onNodeWithText("Law").performClick()
        rule.onNodeWithContentDescription("Remove Tax").performClick()

        assertEquals(law, selected)
        assertEquals("t1", removed)
    }

    @Test
    fun nonEmptyQueryShowsClearSearchThatResetsTheQuery() {
        var query: String? = null
        show(SignupTagsUiState(query = "ta", catalog = loaded), onQueryChange = { query = it })

        rule.onNodeWithContentDescription("Clear search").performClick()

        assertEquals("", query)
    }

    @Test
    fun loadingShowsTheSpinnerCaption() {
        show(SignupTagsUiState(catalog = TagCatalog.Loading))

        rule.onNodeWithText("Loading tags…").assertIsDisplayed()
    }

    @Test
    fun failedLoadShowsTheErrorAndRetryCallsOnRetry() {
        var retries = 0
        show(SignupTagsUiState(catalog = TagCatalog.Failed), onRetry = { retries++ })

        rule.onNodeWithText("Couldn't load tags").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun noMatchShowsTheQueryAndHint() {
        show(SignupTagsUiState(query = "zz", catalog = loaded))

        rule.onNodeWithText("No tags match “zz”").assertIsDisplayed()
        rule.onNodeWithText("Try a different search term.").assertIsDisplayed()
        rule.onAllNodesWithText("Suggested Tags").assertCountEquals(0)
    }

    @Test
    fun saveIsEnabledWithZeroTagsAndCallsOnSave() {
        var saves = 0
        show(SignupTagsUiState(catalog = loaded), onSave = { saves++ })

        rule.onNodeWithText("Save").performClick()

        assertEquals(1, saves)
    }

    @Test
    fun savedStateShowsSavedTitle() {
        show(SignupTagsUiState(catalog = loaded, saveState = SaveState.Saved))

        rule.onNodeWithText("Saved!").assertIsDisplayed()
    }

    @Test
    fun dialogErrorShowsCouldntSave() {
        show(SignupTagsUiState(catalog = loaded, dialogError = FieldError.Res(R.string.login_error_unknown)))

        rule.onNodeWithText("Couldn't save").assertIsDisplayed()
    }
}
