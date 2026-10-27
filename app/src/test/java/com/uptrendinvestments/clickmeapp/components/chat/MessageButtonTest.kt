package com.uptrendinvestments.clickmeapp.components.chat

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private var clicks = 0

    private fun setText(
        busy: Boolean = false,
        enabled: Boolean = true,
    ) {
        rule.setContent {
            ClickMeTheme {
                MessageButton(
                    label = "Message",
                    onClick = { clicks++ },
                    busy = busy,
                    enabled = enabled,
                    modifier = Modifier.testTag(BUTTON),
                )
            }
        }
    }

    private fun setIcon(
        busy: Boolean = false,
        enabled: Boolean = true,
    ) {
        rule.setContent {
            ClickMeTheme {
                MessageButton(
                    label = "Message",
                    onClick = { clicks++ },
                    busy = busy,
                    enabled = enabled,
                    variant = MessageButtonVariant.Icon,
                    contentDescription = "Message Ada",
                    iconTarget = 48.dp,
                )
            }
        }
    }

    @Test
    fun textFormClicks() {
        setText()

        rule.onNodeWithTag(BUTTON).assertHeightIsAtLeast(56.dp).performClick()
        assertEquals(1, clicks)
        rule.onAllNodesWithTag(MESSAGE_BUTTON_SPINNER_TAG).assertCountEquals(0)
    }

    @Test
    fun textFormBusyShowsSpinnerAndIgnoresTaps() {
        setText(busy = true)

        rule.onNodeWithTag(MESSAGE_BUTTON_SPINNER_TAG, useUnmergedTree = true).assertExists()
        rule
            .onNodeWithTag(BUTTON)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Opening chat"))
            .performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun iconFormHasContentDescriptionAndTarget() {
        setIcon()

        rule
            .onNodeWithContentDescription("Message Ada")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun iconFormBusyShowsSpinner() {
        setIcon(busy = true)

        rule.onNodeWithTag(MESSAGE_BUTTON_SPINNER_TAG, useUnmergedTree = true).assertExists()
        rule
            .onNodeWithContentDescription("Message Ada")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Opening chat"))
            .performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun disabledDoesNotClick() {
        setText(enabled = false)

        rule.onNodeWithTag(BUTTON).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun enabledByDefault() {
        setText()

        rule.onNodeWithTag(BUTTON).assertIsEnabled()
    }

    private companion object {
        const val BUTTON = "message_button"
    }
}
