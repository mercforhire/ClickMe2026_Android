@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileAddress
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.taxProfile
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpertTaxProfileScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()

    private fun showWithViewModel(): ExpertTaxProfileViewModel {
        val viewModel = ExpertTaxProfileViewModel(TaxProfileRepository(api))
        rule.setContent {
            ClickMeTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ExpertTaxProfileContent(
                    state = state,
                    actions =
                        TaxProfileActions(
                            onFieldChange = viewModel::onFieldChange,
                            onDobPicked = viewModel::onDobPicked,
                            save = viewModel::save,
                            retry = viewModel::retry,
                            dismissDialog = viewModel::dismissDialog,
                            consumeSaved = viewModel::consumeSaved,
                            consumeFocusTin = viewModel::consumeFocusTin,
                        ),
                    onBack = {},
                )
            }
        }
        return viewModel
    }

    private fun tinInput() = hasSetTextAction() and hasAnyAncestor(hasTestTag(TAX_TIN_TAG))

    private fun serverProfile() {
        api.taxProfileHandler = {
            taxProfile(
                legalName = "Ana Lee",
                dob = "1990-01-02",
                address =
                    TaxProfileAddress(
                        line1 = "1 Main",
                        city = "Toronto",
                        region = "ON",
                        postalCode = "M1M1M1",
                        country = "CA",
                    ),
            )
        }
    }

    @Test
    fun formAndSave() {
        serverProfile()
        api.saveTaxProfileHandler = { SaveTaxProfileData(saved = true) }
        showWithViewModel()
        rule.waitForIdle()

        rule.onNodeWithText("Tax & Payout Info").assertExists()
        rule.onNodeWithText("Required by Stripe", substring = true).assertExists()
        listOf("Legal identity", "Address", "Tax identification").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        listOf(
            "Legal name",
            "Date of birth",
            "Street address",
            "Apt / suite (optional)",
            "City",
            "Province / region",
            "Postal code",
            "Country",
            "Financial account (optional)",
            "TIN (SIN / BN)",
        ).forEach { label -> rule.onAllNodesWithText(label).onFirst().assertExists() }
        rule.onNodeWithText("Jan 2, 1990").assertExists()
        rule.onNodeWithTag(TAX_SAVE_TAG).assertIsEnabled()

        rule.onNode(tinInput()).performScrollTo().performTextInput("123456789")
        rule.onNodeWithTag(TAX_SAVE_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Tax profile saved.").assertExists()
        rule.onNodeWithText("TIN on file").assertExists()
        rule.onAllNodesWithText("Replace TIN (optional)").onFirst().assertExists()
        rule.onNodeWithText("123456789").assertDoesNotExist()
    }

    @Test
    fun saveStaysOffUntilTheRequiredFieldsAreFilled() {
        api.taxProfileHandler = { taxProfile() }
        showWithViewModel()
        rule.waitForIdle()

        rule.onNodeWithTag(TAX_SAVE_TAG).assertIsNotEnabled()
    }
}
