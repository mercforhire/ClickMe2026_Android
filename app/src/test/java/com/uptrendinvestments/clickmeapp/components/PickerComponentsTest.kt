package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PickerComponentsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    // PickerField

    @Test
    fun pickerField_withoutValue_showsPlaceholder() {
        rule.setContent {
            ClickMeTheme {
                PickerField(
                    value = null,
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = {},
                )
            }
        }
        rule.onNodeWithText("Country").assertIsDisplayed()
    }

    @Test
    fun pickerField_withValue_showsValueAndHasButtonRoleWithMergedDescription() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                PickerField(
                    value = "Canada",
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = { clicks++ },
                )
            }
        }
        rule.onNodeWithText("Canada").assertIsDisplayed()
        rule.onNode(hasRole(Role.Button)).assertExists()
        rule.onNodeWithContentDescription("Country, Canada").assertExists()
        rule.onNodeWithText("Canada").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun pickerField_disabled_isNotEnabledAndIgnoresClicks() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                PickerField(
                    value = null,
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = { clicks++ },
                    enabled = false,
                )
            }
        }
        rule.onNode(hasRole(Role.Button)).assertIsNotEnabled()
        rule.onNodeWithText("Country").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun pickerField_withError_showsErrorTextWithErrorSemantics() {
        rule.setContent {
            ClickMeTheme {
                PickerField(
                    value = null,
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = {},
                    error = "Required",
                )
            }
        }
        rule.onNodeWithText("Required").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)).assertExists()
    }

    // SelectableChip

    @Test
    fun selectableChip_selected_exposesRemoveDescriptionAndMinHeightAndClicks() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                SelectableChip(
                    label = "English",
                    selected = true,
                    onClick = { clicks++ },
                    removeContentDescription = "Remove English",
                )
            }
        }
        rule.onNodeWithContentDescription("Remove English").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Remove English").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun selectableChip_unselected_showsLabelWithoutRemoveDescription() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                SelectableChip(label = "French", selected = false, onClick = { clicks++ })
            }
        }
        rule.onNodeWithText("French").assertIsDisplayed()
        rule.onNodeWithContentDescription("Remove French").assertDoesNotExist()
        rule.onNodeWithText("French").performClick()
        assertEquals(1, clicks)
    }

    // PickerSheetContent

    @Test
    fun sheetContent_showsHeadingTitleAndSearchPlaceholder_noClearWhenEmpty() {
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(
                    title = "Select Country",
                    searchQuery = "",
                    searchPlaceholder = "Search countries...",
                    content = {},
                )
            }
        }
        rule.onNodeWithText("Select Country").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        rule.onNodeWithText("Search countries...").assertIsDisplayed()
        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun sheetContent_clearButton_callsOnSearchQueryChangeWithEmpty() {
        val changes = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(
                    title = "Select Country",
                    searchQuery = "can",
                    searchPlaceholder = "Search countries...",
                    onSearchQueryChange = { changes += it },
                    content = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Clear search").performClick()
        assertEquals(listOf(""), changes)
    }

    @Test
    fun sheetContent_typing_reportsNewText() {
        val changes = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(
                    title = "Select Country",
                    searchQuery = "",
                    searchPlaceholder = "Search countries...",
                    onSearchQueryChange = { changes += it },
                    content = {},
                )
            }
        }
        rule.onNode(hasSetTextAction()).performTextInput("fra")
        assertEquals(listOf("fra"), changes)
    }

    @Test
    fun sheetContent_nullSearchQuery_rendersNoSearchField() {
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(title = "Select Timezone", searchQuery = null, content = {})
            }
        }
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun sheetContent_rendersListContentAndFooter() {
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(
                    title = "Select Languages",
                    footer = { Text("Footer content") },
                    content = {
                        item { PickerRow(label = "Canada", selected = false, onClick = {}) }
                    },
                )
            }
        }
        rule.onNodeWithText("Canada").assertIsDisplayed()
        rule.onNodeWithText("Footer content").assertIsDisplayed()
    }

    // PickerRow

    @Test
    fun pickerRow_selected_isSelectedWithCheckAndClicks() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                PickerRow(label = "Canada", selected = true, onClick = { clicks++ })
            }
        }
        rule.onNodeWithText("Canada").assertIsSelected()
        rule.onNodeWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Canada").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun pickerRow_unselected_isNotSelectedAndHasNoCheck() {
        rule.setContent {
            ClickMeTheme {
                PickerRow(label = "Canada", selected = false, onClick = {})
            }
        }
        rule.onNodeWithText("Canada").assertIsNotSelected()
        rule.onNodeWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun pickerRow_minHeight56_isAtLeast56dp() {
        rule.setContent {
            ClickMeTheme {
                PickerRow(label = "Toronto", selected = false, onClick = {}, minHeight = 56.dp)
            }
        }
        rule.onNodeWithText("Toronto").assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun pickerRow_checkboxRole_selected_isOnWithLeadingIconAndCheckAndClicks() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                PickerRow(
                    label = "AI",
                    selected = true,
                    onClick = { clicks++ },
                    leadingIcon = Icons.Outlined.Memory,
                    role = Role.Checkbox,
                )
            }
        }
        rule.onNodeWithText("AI").assertIsOn()
        rule.onNode(hasRole(Role.Checkbox)).assertExists()
        rule.onNodeWithTag(PICKER_ROW_LEADING_ICON_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("AI").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun pickerRow_checkboxRole_unselected_isOffWithNoCheck() {
        rule.setContent {
            ClickMeTheme {
                PickerRow(
                    label = "AI",
                    selected = false,
                    onClick = {},
                    leadingIcon = Icons.Outlined.Memory,
                    role = Role.Checkbox,
                )
            }
        }
        rule.onNodeWithText("AI").assertIsOff()
        rule.onNodeWithTag(PICKER_ROW_CHECK_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun pickerRow_defaults_keepRadioSemanticsAndHaveNoLeadingIcon() {
        rule.setContent {
            ClickMeTheme {
                PickerRow(label = "Canada", selected = true, onClick = {})
            }
        }
        rule.onNodeWithText("Canada").assertIsSelected()
        rule.onNode(hasRole(Role.RadioButton)).assertExists()
        rule.onNodeWithTag(PICKER_ROW_LEADING_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    // PickerLoading

    @Test
    fun pickerLoading_showsTextAndProgressIndicator() {
        rule.setContent {
            ClickMeTheme {
                PickerLoading(text = "Loading languages\u2026")
            }
        }
        rule.onNodeWithText("Loading languages\u2026").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertExists()
    }
}
