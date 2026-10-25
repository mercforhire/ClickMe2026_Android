package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopicEditorSubviewsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun hasStateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    // TopicIconPicker

    @Test
    fun iconPicker_marksSelectedChipAndDescribesIcons() {
        rule.setContent {
            ClickMeTheme {
                TopicIconPicker(options = listOf("ai", "tech"), selected = "tech", onToggle = {}, enabled = true)
            }
        }
        rule.onNodeWithTag("topic_icon_tech").assert(hasStateDescription("Selected"))
        rule.onNodeWithTag("topic_icon_ai").assert(hasStateDescription("Not selected"))
        rule.onNodeWithContentDescription("Tech icon").assertExists()
        rule.onNodeWithContentDescription("AI icon").assertExists()
    }

    @Test
    fun iconPicker_clickCallsToggleWithSlug() {
        val toggled = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                TopicIconPicker(
                    options = listOf("ai", "tech"),
                    selected = "tech",
                    onToggle = { toggled += it },
                    enabled = true,
                )
            }
        }
        rule.onNodeWithTag("topic_icon_tech").performClick()
        rule.onNodeWithTag("topic_icon_ai").performClick()
        assertEquals(listOf("tech", "ai"), toggled)
    }

    @Test
    fun iconPicker_disabledIgnoresClicks() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                TopicIconPicker(options = listOf("ai"), selected = null, onToggle = { clicks++ }, enabled = false)
            }
        }
        rule.onNodeWithTag("topic_icon_ai").performClick()
        assertEquals(0, clicks)
    }

    // TopicDurationStepper

    @Test
    fun stepper_atMinimum_disablesDecreaseOnly() {
        var increases = 0
        rule.setContent {
            ClickMeTheme {
                TopicDurationStepper(
                    minutes = 30,
                    onDecrease = {},
                    onIncrease = { increases++ },
                    enabled = true,
                )
            }
        }
        rule.onNodeWithText("30 min").assertIsDisplayed()
        rule.onNodeWithContentDescription("Decrease duration").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase duration").assertIsEnabled().performClick()
        assertEquals(1, increases)
    }

    @Test
    fun stepper_atMaximum_showsHoursAndDisablesIncrease() {
        rule.setContent {
            ClickMeTheme {
                TopicDurationStepper(minutes = 480, onDecrease = {}, onIncrease = {}, enabled = true)
            }
        }
        rule.onNodeWithText("8 hours").assertIsDisplayed()
        rule.onNodeWithContentDescription("Increase duration").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Decrease duration").assertIsEnabled()
    }

    @Test
    fun stepper_valueIsPoliteLiveRegion() {
        rule.setContent {
            ClickMeTheme {
                TopicDurationStepper(minutes = 90, onDecrease = {}, onIncrease = {}, enabled = true)
            }
        }
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNodeWithText("1 hr 30 min").assert(polite)
    }

    @Test
    fun stepper_disabledDisablesBothButtons() {
        rule.setContent {
            ClickMeTheme {
                TopicDurationStepper(minutes = 60, onDecrease = {}, onIncrease = {}, enabled = false)
            }
        }
        rule.onNodeWithContentDescription("Decrease duration").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase duration").assertIsNotEnabled()
    }

    // TopicDescriptionField

    @Test
    fun description_showsCounterAndForwardsTyping() {
        val typed = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                TopicDescriptionField(value = "abc", onValueChange = { typed += it }, enabled = true)
            }
        }
        rule.onNodeWithText("3/2000").assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextInput("d")
        assertEquals(1, typed.size)
        assertTrue(typed.single().length == 4 && typed.single().contains('d'))
    }

    @Test
    fun description_disabledIsNotEnabled() {
        rule.setContent {
            ClickMeTheme {
                TopicDescriptionField(value = "abc", onValueChange = {}, enabled = false)
            }
        }
        rule.onNodeWithTag("topic_editor_description").assertIsNotEnabled()
        rule.onNodeWithTag("topic_editor_description").assertTextContains("abc")
    }
}
