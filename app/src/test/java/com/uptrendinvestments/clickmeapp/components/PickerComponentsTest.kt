package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
