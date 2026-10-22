package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingMeetingTypeCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingNotesCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.NOTES_FIELD_TAG
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MakeABookingFormCardsTest {
    @get:Rule
    val rule = createComposeRule()

    private val radioRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    private fun setMeetingCard(
        selected: MeetingType = MeetingType.InAppVoice,
        onSelect: (MeetingType) -> Unit = {},
    ) {
        rule.setContent { ClickMeTheme { MakeABookingMeetingTypeCard(selected = selected, onSelect = onSelect) } }
    }

    @Test
    fun meetingCardListsInAppFirstThenSkypeZoomAsRadioRows() {
        setMeetingCard()

        val inApp = rule.onNodeWithText("In-app Voice Call")
        val skype = rule.onNodeWithText("Skype/Zoom Call")
        inApp.assert(radioRole).assertIsSelected()
        skype.assert(radioRole).assertIsNotSelected()
        val inAppTop = inApp.fetchSemanticsNode().boundsInRoot.top
        val skypeTop = skype.fetchSemanticsNode().boundsInRoot.top
        assertTrue("In-app Voice Call must be above Skype/Zoom Call", inAppTop < skypeTop)
    }

    @Test
    fun clickingSkypeZoomSelectsItOnce() {
        val picked = mutableListOf<MeetingType>()
        setMeetingCard(onSelect = { picked += it })

        rule.onNodeWithText("Skype/Zoom Call").performClick()

        assertEquals(listOf(MeetingType.SkypeZoom), picked)
    }

    @Test
    fun emptyNotesShowPlaceholderAndZeroCounter() {
        rule.setContent { ClickMeTheme { MakeABookingNotesCard(notes = "", onNotesChange = {}) } }

        rule
            .onNodeWithText("Add context for the expert — what you'd like to focus on, questions you have, etc.")
            .assertExists()
        rule.onNodeWithText("0/500").assertExists()
    }

    @Test
    fun filledNotesShowCounterWithCharacterCountDescription() {
        rule.setContent { ClickMeTheme { MakeABookingNotesCard(notes = "Hello", onNotesChange = {}) } }

        rule.onNodeWithText("5/500").assertExists()
        rule.onNodeWithContentDescription("5 of 500 characters").assertExists()
    }

    @Test
    fun typingIntoNotesReportsTheEditedText() {
        var latest = ""
        rule.setContent { ClickMeTheme { MakeABookingNotesCard(notes = "", onNotesChange = { latest = it }) } }

        rule.onNodeWithTag(NOTES_FIELD_TAG).performTextInput("Hi")

        assertTrue("expected text ending in Hi but was '$latest'", latest.endsWith("Hi"))
    }
}
