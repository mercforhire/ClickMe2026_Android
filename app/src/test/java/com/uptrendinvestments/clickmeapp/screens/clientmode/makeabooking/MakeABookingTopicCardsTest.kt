package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingExpertCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTopicPicker
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.TOPIC_PICKER_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.makeABookingTopicItems
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MakeABookingTopicCardsTest {
    @get:Rule
    val rule = createComposeRule()

    private val freeTopic = BookingTopicUi("t1", "Intro to ML", 60, null, PriceDisplay.Free, true)

    // Expert card

    @Test
    fun expertCard_showsNameAndTitle() {
        rule.setContent { ClickMeTheme { MakeABookingExpertCard("Ada Lovelace", "Engineer", null) } }
        rule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        rule.onNodeWithText("Engineer").assertIsDisplayed()
    }

    @Test
    fun expertCard_blankTitle_omitsTitleNode() {
        rule.setContent { ClickMeTheme { MakeABookingExpertCard("Ada Lovelace", "", null) } }
        rule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        rule.onNodeWithText("Engineer").assertDoesNotExist()
    }

    @Test
    fun expertCard_isNotClickable() {
        rule.setContent { ClickMeTheme { MakeABookingExpertCard("Ada Lovelace", "Engineer", null) } }
        rule.onNodeWithText("Ada Lovelace").assertHasNoClickAction()
    }

    // Topic picker

    @Test
    fun picker_loading_showsLoadingAndNoClickAction() {
        rule.setContent {
            ClickMeTheme { MakeABookingTopicPicker(TopicsState.Loading, null, onOpen = {}, onRetry = {}) }
        }
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onNodeWithTag(TOPIC_PICKER_TAG).assertHasNoClickAction()
    }

    @Test
    fun picker_loadedWithSelection_showsTitleDurationPriceAndOpensOnClick() {
        var opens = 0
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(
                    topics = TopicsState.Loaded(listOf(freeTopic)),
                    selectedTopic = freeTopic,
                    onOpen = { opens++ },
                    onRetry = {},
                )
            }
        }
        rule.onNodeWithText("Intro to ML").assertIsDisplayed()
        rule.onNodeWithText("60 min").assertIsDisplayed()
        rule.onNodeWithText("Free").assertIsDisplayed()
        val picker = rule.onNodeWithTag(TOPIC_PICKER_TAG)
        picker.assertHasClickAction()
        picker.assertContentDescriptionEquals("Topic of Discussion, Intro to ML, 60 minutes, Free")
        picker.performClick()
        assertEquals(1, opens)
    }

    @Test
    fun picker_loadedWithoutSelection_showsPlaceholder() {
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(TopicsState.Loaded(listOf(freeTopic)), null, onOpen = {}, onRetry = {})
            }
        }
        rule.onNodeWithText("Select a topic").assertIsDisplayed()
        rule.onNodeWithTag(TOPIC_PICKER_TAG).assertHasClickAction()
    }

    @Test
    fun picker_paidTopic_showsFormattedPrice() {
        val paid = BookingTopicUi("t2", "Deep dive", 30, null, PriceDisplay.Paid("$50.00"), false)
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(TopicsState.Loaded(listOf(paid)), paid, onOpen = {}, onRetry = {})
            }
        }
        rule.onNodeWithText("$50.00").assertIsDisplayed()
    }

    @Test
    fun picker_emptyList_showsNoTopicsAndIsNotClickable() {
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(TopicsState.Loaded(emptyList()), null, onOpen = {}, onRetry = {})
            }
        }
        rule.onNodeWithText("This expert has no topics to book yet.").assertIsDisplayed()
        rule.onNodeWithTag(TOPIC_PICKER_TAG).assertHasNoClickAction()
    }

    @Test
    fun picker_failed_showsMessageAndRetry() {
        var retries = 0
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(
                    topics = TopicsState.Failed(FieldError.Text("Expert not found")),
                    selectedTopic = null,
                    onOpen = {},
                    onRetry = { retries++ },
                )
            }
        }
        rule.onNodeWithText("Expert not found").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    // Topic sheet

    private val paidTopic =
        BookingTopicUi("t2", "Deep dive", 30, "Long session", PriceDisplay.Paid("$50.00"), false)
    private val sheetFreeTopic = BookingTopicUi("t1", "Intro", 60, null, PriceDisplay.Free, true)

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun setSheet(
        topics: List<BookingTopicUi>,
        selectedId: String?,
        onSelect: (String) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(title = "Choose a Topic") {
                    makeABookingTopicItems(topics, selectedId, onSelect)
                }
            }
        }
    }

    @Test
    fun sheet_listsEveryTopicIncludingPaidWithDetails() {
        setSheet(listOf(sheetFreeTopic, paidTopic), "t1")
        rule.onNodeWithText("Choose a Topic").assertIsDisplayed()
        rule.onNodeWithText("Intro").assertIsDisplayed()
        rule.onNodeWithText("Deep dive").assertIsDisplayed()
        rule.onNodeWithText("Free").assertIsDisplayed()
        rule.onNodeWithText("$50.00").assertIsDisplayed()
        rule.onNodeWithText("60 min").assertIsDisplayed()
        rule.onNodeWithText("30 min").assertIsDisplayed()
        rule.onNodeWithText("Long session").assertIsDisplayed()
    }

    @Test
    fun sheet_selectedCardIsSelectedRadioButton() {
        setSheet(listOf(sheetFreeTopic, paidTopic), "t1")
        val selected = rule.onNode(hasRole(Role.RadioButton) and hasText("Intro"))
        selected.assertIsSelected()
        rule.onNode(hasRole(Role.RadioButton) and hasText("Deep dive")).assertIsNotSelected()
    }

    @Test
    fun sheet_clickingCardsReportsTheirIds() {
        val selections = mutableListOf<String>()
        setSheet(listOf(sheetFreeTopic, paidTopic), "t1") { selections += it }
        rule.onNodeWithText("Deep dive").performClick()
        assertEquals(listOf("t2"), selections)
        rule.onNodeWithText("Intro").performClick()
        assertEquals(listOf("t2", "t1"), selections)
    }

    @Test
    fun sheet_blankOrNullDescription_showsNoDescriptionNode() {
        val blank = BookingTopicUi("t3", "Blank desc", 15, "   ", PriceDisplay.Free, true)
        setSheet(listOf(sheetFreeTopic, blank), null)
        rule.onNodeWithText("Intro").assertIsDisplayed()
        rule.onNodeWithText("Blank desc").assertIsDisplayed()
        rule.onNodeWithText("   ").assertDoesNotExist()
    }

    @Test
    fun topicLoadFailureShowsNamedError() {
        var retries = 0
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(
                    topics = TopicsState.Failed(FieldError.Text("Expert not found")),
                    selectedTopic = null,
                    onOpen = {},
                    onRetry = { retries++ },
                )
            }
        }
        rule.onNodeWithText("Couldn't load topics").assertIsDisplayed()
        rule.onNodeWithText("Expert not found").assertIsDisplayed()
        rule.onNodeWithText("Retry").assertHasClickAction().performClick()
        assertEquals(1, retries)
        rule.onAllNodesWithText("Retry connection").assertCountEquals(0)
    }

    @Test
    fun topicEmptyShowsEmptyCopyWithoutRetry() {
        rule.setContent {
            ClickMeTheme {
                MakeABookingTopicPicker(TopicsState.Loaded(emptyList()), null, onOpen = {}, onRetry = {})
            }
        }
        rule.onNodeWithText("This expert has no topics to book yet.").assertIsDisplayed()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
    }

    @Test
    fun topicRowsAreRadioButtonsInASelectableGroup() {
        setSheet(listOf(sheetFreeTopic, paidTopic), "t1")

        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(2)
        rule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup) and
                    hasAnyDescendant(hasRole(Role.RadioButton)),
            ).assertCountEquals(1)
    }
}
