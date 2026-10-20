package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ControlsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    // AsyncSaveButton

    @Test
    fun asyncSaveButton_idle_isClickableAndInvokesOnClick() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme { AsyncSaveButton(title = "Save changes", state = SaveState.Idle, onClick = { clicks++ }) }
        }
        rule.onNodeWithText("Save changes").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun asyncSaveButton_saving_isBlockedAndAnnounced() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme { AsyncSaveButton(title = "Save changes", state = SaveState.Saving, onClick = { clicks++ }) }
        }
        val node = rule.onNode(hasStateDescription("Saving"))
        node.assertExists()
        node.assertIsNotEnabled()
        node.performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun asyncSaveButton_saved_showsSavedTitleAndState() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme { AsyncSaveButton(title = "Save changes", state = SaveState.Saved, onClick = { clicks++ }) }
        }
        rule.onNodeWithText("Saved!").assertIsDisplayed()
        rule.onNode(hasStateDescription("Saved")).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun asyncSaveButton_idleDisabled_neverInvokesOnClick() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                AsyncSaveButton(title = "Save changes", state = SaveState.Idle, onClick = { clicks++ }, enabled = false)
            }
        }
        rule.onNodeWithText("Save changes").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    // PressScaleButton

    @Test
    fun pressScaleButton_clickableWithMinimumTouchTarget() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme { PressScaleButton(onClick = { clicks++ }) { Text("Continue") } }
        }
        val button = rule.onNode(hasClickAction())
        button.assertHeightIsAtLeast(48.dp)
        button.performClick()
        assertEquals(1, clicks)
    }

    // EmptyState / ErrorState

    @Test
    fun emptyState_showsHeadingAndBody_withoutActionHasNoButton() {
        rule.setContent {
            ClickMeTheme {
                EmptyState(icon = Icons.Outlined.Info, heading = "No bookings yet", body = "Body copy")
            }
        }
        rule.onNodeWithText("No bookings yet").assertIsDisplayed()
        rule.onNodeWithText("Body copy").assertIsDisplayed()
        rule.onAllNodesWithText("Explore experts").assertCountEquals(0)
        rule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun emptyState_actionIsDisplayedAndInvoked() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                EmptyState(
                    icon = Icons.Outlined.Info,
                    heading = "No bookings yet",
                    body = "Body copy",
                    action = EmptyStateAction("Explore experts") { clicks++ },
                )
            }
        }
        rule.onNodeWithText("Explore experts").assertIsDisplayed().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun errorState_showsCopyAndRetryInvokesCallback() {
        var retries = 0
        rule.setContent { ClickMeTheme { ErrorState(onRetry = { retries++ }) } }
        rule.onNodeWithText("Something went wrong").assertIsDisplayed()
        rule.onNodeWithText("Check your connection and try again.").assertIsDisplayed()
        rule.onNodeWithText("Retry connection").assertIsDisplayed().performClick()
        assertEquals(1, retries)
    }

    // PlaceholderTabContent

    @Test
    fun placeholderTabContent_showsTitleTwiceStubBodyAndFooter() {
        rule.setContent {
            ClickMeTheme {
                PlaceholderTabContent(
                    title = "Explore",
                    icon = Icons.Outlined.Info,
                    footer = { Column { Text("Footer content") } },
                )
            }
        }
        rule.onAllNodes(hasText("Explore")).assertCountEquals(2)
        rule.onNodeWithText("Nothing here yet.").assertIsDisplayed()
        rule.onNodeWithText("Footer content").assertIsDisplayed()
    }
}
