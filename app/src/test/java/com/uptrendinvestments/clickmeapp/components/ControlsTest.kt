package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.debug.GalleryControlsSection
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ControlsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    // ValidatedField

    @Test
    fun validatedField_withError_exposesErrorSemanticsAndText() {
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "x",
                    onValueChange = {},
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Info,
                    error = "Enter a valid email address.",
                )
            }
        }
        rule.onNodeWithText("Enter a valid email address.").assertIsDisplayed()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)).assertExists()
    }

    @Test
    fun validatedField_withoutError_hasNoErrorText() {
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "x",
                    onValueChange = {},
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Info,
                    error = null,
                )
            }
        }
        rule.onAllNodesWithText("Enter a valid email address.").assertCountEquals(0)
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)).assertCountEquals(0)
    }

    @Test
    fun validatedField_secure_togglesPasswordVisibilityLabel() {
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "secret",
                    onValueChange = {},
                    placeholder = "Password",
                    leadingIcon = Icons.Outlined.Info,
                    error = null,
                    isSecure = true,
                )
            }
        }
        rule.onNodeWithContentDescription("Show password").performClick()
        rule.onNodeWithContentDescription("Hide password").assertExists()
        rule.onAllNodesWithContentDescription("Show password").assertCountEquals(0)
    }

    @Test
    fun validatedField_typing_invokesOnValueChange() {
        var latest = ""
        rule.setContent {
            ClickMeTheme {
                ValidatedField(
                    value = "",
                    onValueChange = { latest = it },
                    placeholder = "Email",
                    leadingIcon = Icons.Outlined.Info,
                    error = null,
                )
            }
        }
        rule.onNode(hasSetTextAction()).performTextInput("abc")
        assertEquals("abc", latest)
    }

    @Test
    fun galleryControlsSection_rendersAllHeadings() {
        rule.setContent { ClickMeTheme { GalleryControlsSection() } }
        rule.onNodeWithText("PressScaleButton").assertExists()
        rule.onNodeWithText("AsyncSaveButton").assertExists()
        rule.onNodeWithText("ValidatedField").assertExists()
        rule.onNodeWithText("EmptyState").assertExists()
    }

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

    @Test
    fun errorStateDefaultsUnchanged() {
        rule.setContent { ClickMeTheme { ErrorState(onRetry = {}) } }
        rule.onNodeWithText("Something went wrong").assertIsDisplayed()
        rule.onNodeWithText("Check your connection and try again.").assertIsDisplayed()
        rule.onNodeWithText("Retry connection").assertIsDisplayed()
    }

    @Test
    fun errorStateNamedHeading() {
        var retries = 0
        rule.setContent {
            ClickMeTheme {
                ErrorState(
                    onRetry = { retries++ },
                    heading = "Couldn't load bookings",
                    body = "Server says no",
                    retryLabel = "Retry",
                )
            }
        }
        rule.onNodeWithText("Couldn't load bookings").assertIsDisplayed()
        rule.onNodeWithText("Server says no").assertIsDisplayed()
        rule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun emptyStateHeadingIsHeading() {
        rule.setContent {
            ClickMeTheme {
                EmptyState(icon = Icons.Outlined.Info, heading = "No bookings yet", body = "Body copy")
            }
        }
        rule
            .onNode(hasText("No bookings yet") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertExists()
        rule
            .onNode(hasText("Body copy") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertDoesNotExist()
    }

    @Test
    fun errorStateIsPoliteLiveRegion() {
        rule.setContent {
            ClickMeTheme {
                ErrorState(
                    onRetry = {},
                    heading = "Couldn't load bookings",
                    body = "Server says no",
                    retryLabel = "Retry",
                )
            }
        }
        rule
            .onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
                useUnmergedTree = true,
            ).assert(hasAnyDescendant(hasText("Couldn't load bookings")))
            .assert(hasAnyDescendant(hasText("Server says no")))
    }

    /**
     * Robolectric measures text with a 1 px per character stand-in font, so a long string never reaches the 328 px
     * line width. The heading therefore carries two line breaks: three lines prove nothing clamps it, and the layout
     * input proves no max line count or ellipsis is set (UI Consideration "long-text: State headings and bodies").
     */
    @Test
    @Config(qualifiers = "w360dp")
    fun longHeadingWraps() {
        val heading = "Couldn't load your bookings\nbecause something went wrong\non the server side"
        rule.setContent {
            ClickMeTheme {
                EmptyState(icon = Icons.Outlined.Info, heading = heading, body = "Body copy")
                ErrorState(onRetry = {}, heading = heading)
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        rule.onAllNodes(hasText(heading)).fetchSemanticsNodes().forEach { node ->
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        }
        assertEquals(2, results.size)
        results.forEach {
            assertTrue("expected a wrapped heading, got ${it.lineCount} lines", it.lineCount > 1)
            assertEquals(Int.MAX_VALUE, it.layoutInput.maxLines)
            assertEquals(TextOverflow.Clip, it.layoutInput.overflow)
            assertTrue(it.layoutInput.softWrap)
        }
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
