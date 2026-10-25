package com.uptrendinvestments.clickmeapp.screens.expertmode.settings.subviews

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A phone-sized window so the lazy list composes every row and the pinned action row.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ExpertiseEditorSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val audit = ExpertiseTagItem("t3", "Audit")
    private val law = ExpertiseTagItem("t2", "Law")
    private val tax = ExpertiseTagItem("t1", "Tax")
    private val loaded = ExpertiseCatalogState.Loaded(listOf(audit, law, tax))

    private fun show(
        state: ExpertiseEditorUiState,
        onToggle: (ExpertiseTagItem) -> Unit = {},
        onRetry: () -> Unit = {},
        onCancel: () -> Unit = {},
        onSave: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertiseEditorContent(
                    state = state,
                    onQueryChange = {},
                    onToggle = onToggle,
                    onRemove = {},
                    onRetry = onRetry,
                    onCancel = onCancel,
                    onSave = onSave,
                    onDismissDialog = {},
                )
            }
        }
    }

    private fun row(label: String) = rule.onNode(hasText(label) and isToggleable())

    @Test
    fun loadedCatalogShowsTitleChipsAndCheckedRows() {
        show(ExpertiseEditorUiState(selected = listOf(tax), catalog = loaded))

        rule.onNodeWithText("Edit Expertise").assertIsDisplayed()
        rule.onNodeWithText("Search for tags").assertIsDisplayed()
        rule.onNodeWithText("Your Tags").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove Tax").assertIsDisplayed()
        rule.onNodeWithText("The first tag is your primary tag.").assertIsDisplayed()
        rule.onNodeWithText("All Expertise").assertIsDisplayed()
        row("Tax").assertIsOn()
        row("Law").assertIsOff()
        row("Audit").assertIsOff()
        rule.onAllNodesWithText("Pick at least one expertise tag before saving.").assertCountEquals(0)
    }

    @Test
    fun clickingARowTogglesThatTag() {
        var toggled: ExpertiseTagItem? = null
        show(ExpertiseEditorUiState(selected = listOf(tax), catalog = loaded), onToggle = { toggled = it })

        row("Law").performClick()

        assertEquals(law, toggled)
    }

    @Test
    fun emptySelectionExplainsAndDisablesSave() {
        show(ExpertiseEditorUiState(catalog = loaded))

        rule.onNodeWithText("Pick at least one expertise tag before saving.").assertIsDisplayed()
        rule.onNodeWithTag(EXPERTISE_SHEET_SAVE_TAG).assertIsNotEnabled()
        rule.onAllNodesWithText("Your Tags").assertCountEquals(0)
    }

    @Test
    fun loadingShowsTheCaption() {
        show(ExpertiseEditorUiState(catalog = ExpertiseCatalogState.Loading))
        rule.onNodeWithText("Loading tags…").assertIsDisplayed()
    }

    @Test
    fun failedCatalogOffersRetry() {
        var retries = 0
        show(
            ExpertiseEditorUiState(catalog = ExpertiseCatalogState.Failed(FieldError.Text("Server is down"))),
            onRetry = { retries++ },
        )

        rule.onNodeWithText("Couldn't load tags").assertIsDisplayed()
        rule.onNodeWithText("Server is down").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun aQueryWithNoRowsShowsTheNoMatchBlock() {
        show(ExpertiseEditorUiState(query = "zz", selected = listOf(tax), catalog = loaded))

        rule.onNodeWithText("No tags match “zz”").assertIsDisplayed()
        rule.onNodeWithText("Try a different search term.").assertIsDisplayed()
    }

    @Test
    fun savingShowsTheSpinnerStateAndDisablesCancel() {
        show(ExpertiseEditorUiState(selected = listOf(tax), catalog = loaded, isSaving = true))

        rule
            .onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving expertise"),
                useUnmergedTree = true,
            ).assertExists()
        rule.onNodeWithTag(EXPERTISE_SHEET_SAVE_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(EXPERTISE_SHEET_CANCEL_TAG).assertIsNotEnabled()
    }

    @Test
    fun cancelAndSaveInvokeTheirCallbacks() {
        var cancelled = 0
        var saved = 0
        show(
            ExpertiseEditorUiState(selected = listOf(tax), catalog = loaded),
            onCancel = { cancelled++ },
            onSave = { saved++ },
        )

        rule.onNodeWithTag(EXPERTISE_SHEET_SAVE_TAG).assertIsEnabled()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Save Expertise").performClick()

        assertEquals(1, cancelled)
        assertEquals(1, saved)
    }

    @Test
    fun aSaveErrorShowsTheDialogWithTheServerMessage() {
        show(
            ExpertiseEditorUiState(
                selected = listOf(tax),
                catalog = loaded,
                dialog = FieldError.Text("Unknown tag"),
            ),
        )

        rule.onNodeWithText("Couldn't save").assertIsDisplayed()
        rule.onNodeWithText("Unknown tag").assertIsDisplayed()
    }
}
