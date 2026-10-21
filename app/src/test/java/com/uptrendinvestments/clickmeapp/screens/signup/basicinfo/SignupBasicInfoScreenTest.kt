package com.uptrendinvestments.clickmeapp.screens.signup.basicinfo

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.components.PICKER_ROW_CHECK_TAG
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Country
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.languageselection.LanguageSelectionSheetContent
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.subviews.CountryPickerSheetContent
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignupBasicInfoScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val english = LanguageItem("en", "English")
    private val french = LanguageItem("fr", "French")

    private var countrySheetOpens = 0
    private var languageSheetOpens = 0
    private var removed = mutableListOf<String>()
    private var saves = 0
    private var firstNames = mutableListOf<String>()

    private fun show(state: SignupBasicInfoUiState) {
        rule.setContent {
            ClickMeTheme {
                SignupBasicInfoContent(
                    state = state,
                    onFirstNameChange = { firstNames += it },
                    onCityChange = {},
                    onProvinceChange = {},
                    onOpenCountrySheet = { countrySheetOpens++ },
                    onOpenLanguageSheet = { languageSheetOpens++ },
                    onRemoveLanguage = { removed += it },
                    onSave = { saves++ },
                    onBack = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    @Test
    fun rendersAllCopyAndNoPrimaryLanguageHelper() {
        show(SignupBasicInfoUiState(languages = listOf(english)))

        rule.onNodeWithText("Personal Details").assertExists()
        rule.onNodeWithText("Tell us a bit about yourself.").assertExists()
        rule.onNodeWithText("First Name").assertExists()
        rule.onNodeWithText("City").assertExists()
        rule.onNodeWithText("Province/State (optional)").assertExists()
        rule.onNodeWithText("Country").assertExists()
        rule.onNodeWithText("Spoken Languages").assertExists()
        rule.onNodeWithContentDescription("Remove English").assertExists()
        rule.onNodeWithText("Add language...").assertExists()
        rule.onNode(hasText("Save") and hasClickAction()).assertExists()
        rule.onNodeWithText("The first language you pick is your primary language.").assertDoesNotExist()
    }

    @Test
    fun typingInFirstNameReportsTheChange() {
        show(SignupBasicInfoUiState())
        rule.onNodeWithText("First Name").performTextInput("Ada")
        assertEquals(listOf("Ada"), firstNames)
    }

    @Test
    fun countryPickerOpensTheCountrySheet() {
        show(SignupBasicInfoUiState(countryCode = "CAN"))
        rule.onNodeWithText("Canada").performScrollTo().performClick()
        assertEquals(1, countrySheetOpens)
    }

    @Test
    fun addLanguageOpensTheLanguageSheet() {
        show(SignupBasicInfoUiState())
        rule.onNodeWithText("Add language...").performScrollTo().performClick()
        assertEquals(1, languageSheetOpens)
    }

    @Test
    fun tappingAChipRemovesThatLanguage() {
        show(SignupBasicInfoUiState(languages = listOf(english)))
        rule.onNodeWithContentDescription("Remove English").performScrollTo().performClick()
        assertEquals(listOf("en"), removed)
    }

    @Test
    fun saveCallsOnSave() {
        show(SignupBasicInfoUiState())
        rule.onNode(hasText("Save") and hasClickAction()).performClick()
        assertEquals(1, saves)
    }

    @Test
    fun cityErrorIsShownInline() {
        show(SignupBasicInfoUiState(cityError = FieldError.Text("Bad city")))
        rule.onNodeWithText("Bad city").assertExists()
    }

    @Test
    fun savedStateShowsSaved() {
        show(SignupBasicInfoUiState(saveState = SaveState.Saved))
        rule.onNodeWithText("Saved!").assertExists()
    }

    @Test
    fun dialogErrorShowsCouldntSaveTitle() {
        show(SignupBasicInfoUiState(dialogError = FieldError.Text("Server down")))
        rule.onNodeWithText("Couldn't save").assertExists()
        rule.onNodeWithText("Server down").assertExists()
    }

    @Test
    fun countrySheetListsCountriesMarksSelectedAndSelects() {
        val picked = mutableListOf<String>()
        val queries = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                CountryPickerSheetContent(
                    countries = listOf(Country("CAN", "Canada"), Country("FRA", "France")),
                    selectedCode = "CAN",
                    query = "",
                    onQueryChange = { queries += it },
                    onSelect = { picked += it },
                )
            }
        }

        rule.onNodeWithText("Select Country").assertExists()
        rule.onNodeWithText("France").assertExists()
        rule.onNodeWithText("Search countries...").performTextInput("fr")
        rule.onNodeWithText("Canada").performClick()

        assertEquals(listOf("fr"), queries)
        assertEquals(listOf("CAN"), picked)
        rule.onAllNodesWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertCountEquals(1)
    }

    private fun showLanguageSheet(
        state: LanguageSheetState,
        retries: () -> Unit = {},
        cancels: () -> Unit = {},
        dones: () -> Unit = {},
        toggles: (LanguageItem) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                LanguageSelectionSheetContent(
                    state = state,
                    onQueryChange = {},
                    onToggle = toggles,
                    onRetry = retries,
                    onCancel = cancels,
                    onDone = dones,
                )
            }
        }
    }

    @Test
    fun languageSheetShowsLoadedRowsAndToggles() {
        val toggled = mutableListOf<String>()
        showLanguageSheet(
            LanguageSheetState(
                selection = listOf(english),
                catalog = LanguageCatalog.Loaded(listOf(english, french)),
            ),
            toggles = { toggled += it.id },
        )

        rule.onNodeWithText("Select Spoken Languages").assertExists()
        rule.onNodeWithText("Search languages...").assertExists()
        rule.onNodeWithText("French").performClick()
        assertEquals(listOf("fr"), toggled)
        rule.onAllNodesWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun languageSheetLoadingShowsSpinnerText() {
        showLanguageSheet(LanguageSheetState(catalog = LanguageCatalog.Loading))
        rule.onNodeWithText("Loading languages…").assertExists()
    }

    @Test
    fun languageSheetFailureShowsRetry() {
        var retried = 0
        showLanguageSheet(LanguageSheetState(catalog = LanguageCatalog.Failed), retries = { retried++ })

        rule.onNodeWithText("Couldn't load languages").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun languageSheetCancelAndDoneCallTheirCallbacks() {
        var cancelled = 0
        var done = 0
        showLanguageSheet(
            LanguageSheetState(catalog = LanguageCatalog.Loaded(listOf(english))),
            cancels = { cancelled++ },
            dones = { done++ },
        )

        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Done").performClick()

        assertEquals(1, cancelled)
        assertEquals(1, done)
    }
}
