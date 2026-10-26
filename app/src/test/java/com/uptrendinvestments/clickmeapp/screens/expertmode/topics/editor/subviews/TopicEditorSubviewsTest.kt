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
import androidx.compose.ui.text.input.KeyboardType
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
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

    // TopicPriceField

    private fun setPrice(
        text: String = "12.50",
        helper: String? = HELPER,
        error: String? = null,
        decimal: Boolean = true,
        onChange: (String) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                TopicPriceField(
                    text = text,
                    onTextChange = onChange,
                    currencyCode = "CAD",
                    helperText = helper,
                    error = error,
                    decimalKeyboard = decimal,
                    enabled = true,
                )
            }
        }
    }

    @Test
    fun price_showsTextBadgeAndHelper() {
        setPrice()
        rule.onNodeWithText("12.50").assertIsDisplayed()
        rule.onNodeWithText("CAD").assertIsDisplayed()
        rule.onNodeWithText(HELPER).assertIsDisplayed()
        rule.onNodeWithText("Price per Session").assertIsDisplayed()
    }

    @Test
    fun price_errorReplacesHelper() {
        setPrice(error = "A paid session must be at least $5.00.")
        rule.onNodeWithText("A paid session must be at least $5.00.").assertIsDisplayed()
        rule.onNodeWithText(HELPER).assertDoesNotExist()
    }

    @Test
    fun price_withoutHelperOrErrorHasNoHelperLine() {
        setPrice(helper = null)
        rule.onNodeWithText(HELPER).assertDoesNotExist()
        rule.onNodeWithText("CAD").assertIsDisplayed()
    }

    @Test
    fun price_forwardsTyping() {
        val typed = mutableListOf<String>()
        setPrice(text = "", helper = null, onChange = { typed += it })
        rule.onNode(hasSetTextAction()).performTextInput("7")
        assertEquals(listOf("7"), typed)
    }

    @Test
    fun price_keyboardIsDecimalUnlessZeroDigitCurrency() {
        assertEquals(KeyboardType.Decimal, priceKeyboardType(decimalKeyboard = true))
        assertEquals(KeyboardType.Number, priceKeyboardType(decimalKeyboard = false))
    }

    // TopicTagPicker

    private fun setPicker(
        isLoading: Boolean = false,
        loadFailed: Boolean = false,
        hasProfileTags: Boolean = true,
        visible: List<ExpertiseTagItem> = listOf(Law, Audit),
        selected: Set<String> = setOf("law"),
        atCap: Boolean = false,
        filter: String = "",
        onFilterChange: (String) -> Unit = {},
        onToggle: (ExpertiseTagItem) -> Unit = {},
        onAdd: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                TopicTagPicker(
                    isLoading = isLoading,
                    loadFailed = loadFailed,
                    hasProfileTags = hasProfileTags,
                    visibleTags = visible,
                    selectedIds = selected,
                    atCap = atCap,
                    filter = filter,
                    onFilterChange = onFilterChange,
                    onToggle = onToggle,
                    onAddExpertise = onAdd,
                    onRetry = onRetry,
                    enabled = true,
                )
            }
        }
    }

    @Test
    fun picker_loading_showsLoadingText() {
        setPicker(isLoading = true)
        rule.onNodeWithText("Loading tags…").assertIsDisplayed()
        rule.onNodeWithText("Law").assertDoesNotExist()
    }

    @Test
    fun picker_withoutProfileTags_offersAddExpertise() {
        var adds = 0
        setPicker(hasProfileTags = false, visible = emptyList(), selected = emptySet(), onAdd = { adds++ })
        rule.onNodeWithText("You don't have any expertise tags on your profile yet.").assertIsDisplayed()
        rule.onNodeWithText("Add Expertise").performClick()
        assertEquals(1, adds)
    }

    @Test
    fun picker_loadFailed_showsRetry() {
        var retries = 0
        setPicker(loadFailed = true, onRetry = { retries++ })
        rule.onNodeWithText("Couldn't load your tags.").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun picker_loadFailed_stillShowsSelectedTagsAsRemovableChips() {
        val toggled = mutableListOf<ExpertiseTagItem>()
        setPicker(loadFailed = true, onToggle = { toggled += it })
        rule.onNodeWithContentDescription("Remove Law").assertIsDisplayed().performClick()
        assertEquals(listOf(Law), toggled)
        rule.onNodeWithText("Audit").assertDoesNotExist()
        rule.onNodeWithText("Couldn't load your tags.").assertIsDisplayed()
        rule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun picker_loaded_showsChipsAndTogglesUnselected() {
        val toggled = mutableListOf<ExpertiseTagItem>()
        setPicker(onToggle = { toggled += it })
        rule.onNodeWithContentDescription("Remove Law").assertIsDisplayed()
        rule.onNodeWithText("Audit").assertIsDisplayed().performClick()
        assertEquals(listOf(Audit), toggled)
    }

    @Test
    fun picker_atCap_disablesUnselectedAndKeepsSelectedClickable() {
        val toggled = mutableListOf<ExpertiseTagItem>()
        setPicker(atCap = true, onToggle = { toggled += it })
        rule.onNodeWithText("Audit").assertIsNotEnabled()
        rule.onNode(hasStateDescription("Limit reached")).assertExists()
        rule.onNodeWithContentDescription("Remove Law").assertIsEnabled().performClick()
        assertEquals(listOf(Law), toggled)
    }

    @Test
    fun picker_belowCap_hasNoLimitReachedState() {
        setPicker(atCap = false)
        rule.onNode(hasStateDescription("Limit reached")).assertDoesNotExist()
    }

    @Test
    fun picker_emptyFilterResult_showsNoMatch() {
        setPicker(visible = emptyList(), selected = emptySet(), filter = "zz")
        rule.onNodeWithText("No tags match “zz”").assertIsDisplayed()
    }

    @Test
    fun picker_filterTypingAndClearButton() {
        val filters = mutableListOf<String>()
        setPicker(filter = "", onFilterChange = { filters += it })
        rule.onNodeWithContentDescription("Clear filter").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("a")
        assertEquals(listOf("a"), filters)
    }

    @Test
    fun picker_clearButtonAppearsWithFilterAndClears() {
        val filters = mutableListOf<String>()
        setPicker(filter = "la", onFilterChange = { filters += it })
        rule.onNodeWithContentDescription("Clear filter").performClick()
        assertEquals(listOf(""), filters)
    }

    @Test
    fun picker_headerAddButtonOpensExpertise() {
        var adds = 0
        setPicker(onAdd = { adds++ })
        rule.onNodeWithTag("topic_editor_add_expertise").assertIsDisplayed()
        rule.onNodeWithContentDescription("Add expertise").performClick()
        assertEquals(1, adds)
    }

    private companion object {
        const val HELPER = "Leave blank or enter 0 for a free session — otherwise min $5.00"
        val Law = ExpertiseTagItem(id = "law", label = "Law")
        val Audit = ExpertiseTagItem(id = "audit", label = "Audit")
    }
}
