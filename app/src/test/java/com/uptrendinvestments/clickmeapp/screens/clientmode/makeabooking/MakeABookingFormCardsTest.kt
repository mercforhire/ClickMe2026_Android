package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.BOOK_NOW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingBookButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingMeetingTypeCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingNotesCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingSummarySection
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.NOTES_FIELD_TAG
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

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

    private fun setSummary(
        topic: BookingTopicUi?,
        slotStart: Instant? = null,
        meetingType: MeetingType = MeetingType.InAppVoice,
    ) {
        rule.setContent {
            ClickMeTheme {
                MakeABookingSummarySection(
                    topic = topic,
                    selectedDate = LocalDate.of(2026, 10, 24),
                    selectedSlotStart = slotStart,
                    meetingType = meetingType,
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
    }

    private val freeTopic = BookingTopicUi("t1", "Intro", 60, null, PriceDisplay.Free, true)

    @Test
    fun summaryShowsFiveRowsWithShortDateBeforeASlotIsChosen() {
        setSummary(freeTopic)

        rule.onNodeWithText("Topic:").assertExists()
        rule.onNodeWithText("Intro").assertExists()
        rule.onNodeWithText("Date & Time:").assertExists()
        rule.onNodeWithText("Oct 24, 2026").assertExists()
        rule.onNodeWithText("Duration:").assertExists()
        rule.onNodeWithText("60 min").assertExists()
        rule.onNodeWithText("Meeting Type:").assertExists()
        rule.onNodeWithText("In-app Voice Call").assertExists()
        rule.onNodeWithText("Price:").assertExists()
        rule.onNodeWithText("Free").assertExists()
    }

    @Test
    fun summaryAddsDeviceFormatTimeOnceASlotIsChosen() {
        setSummary(freeTopic, slotStart = Instant.parse("2026-10-24T18:00:00Z"), meetingType = MeetingType.SkypeZoom)

        rule.onNodeWithText("Oct 24, 2026, 2:00 PM").assertExists()
        rule.onNodeWithText("Skype/Zoom Call").assertExists()
    }

    @Test
    fun summaryShowsPlaceholdersWithoutATopic() {
        setSummary(topic = null)

        // Topic, Duration and Price values.
        rule.onAllNodesWithText("—").assertCountEquals(3)
        rule.onNodeWithText("In-app Voice Call").assertExists()
    }

    @Test
    fun summaryRowReadsLabelThenValueAsOneNode() {
        setSummary(freeTopic)

        val texts =
            rule
                .onAllNodesWithText("Topic:")
                .fetchSemanticsNodes()
                .single()
                .config[SemanticsProperties.Text]
                .joinToString(" ") { it.text }
        assertEquals("Topic: Intro", texts)
    }

    private fun setBookButton(
        reason: BookDisabledReason?,
        submitting: Boolean = false,
        onClick: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                MakeABookingBookButton(disabledReason = reason, isSubmitting = submitting, onClick = onClick)
            }
        }
    }

    private fun stateIs(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun bookNowEnabledCallsBackOnce() {
        var clicks = 0
        setBookButton(reason = null, onClick = { clicks++ })

        rule.onNodeWithTag(BOOK_NOW_TAG).assertIsEnabled()
        rule.onNodeWithText("Book Now").assertExists()
        rule.onNodeWithTag(BOOK_NOW_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun bookNowWithoutSlotOrTopicIsDisabledWithReasonAndNoPaidNote() {
        setBookButton(reason = BookDisabledReason.NoSlot)
        rule.onNodeWithTag(BOOK_NOW_TAG).assertIsNotEnabled().assert(stateIs("Choose a time slot to continue"))
        rule.onAllNodesWithText("coming soon", substring = true).assertCountEquals(0)
    }

    @Test
    fun bookNowWithoutTopicExplainsWhy() {
        setBookButton(reason = BookDisabledReason.NoTopic)
        rule.onNodeWithTag(BOOK_NOW_TAG).assertIsNotEnabled().assert(stateIs("Choose a topic to continue"))
    }

    @Test
    fun bookNowForPaidTopicIsDisabledWithComingSoonNoteOnly() {
        setBookButton(reason = BookDisabledReason.PaidTopic)

        rule.onNodeWithTag(BOOK_NOW_TAG).assertIsNotEnabled().assert(stateIs("Paid booking is coming soon"))
        rule
            .onNodeWithText("Booking paid sessions is coming soon. Choose a free topic to book now.")
            .assertExists()
        rule.onAllNodesWithText("payment work", substring = true).assertCountEquals(0)
        rule.onAllNodesWithText("Add a Payment Method", substring = true).assertCountEquals(0)
    }

    @Test
    fun bookNowInFlightShowsProgressStaysEnabledAndIgnoresTaps() {
        var clicks = 0
        setBookButton(reason = null, submitting = true, onClick = { clicks++ })

        rule.onAllNodesWithText("Book Now").assertCountEquals(0)
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertExists()
        rule
            .onNodeWithTag(BOOK_NOW_TAG)
            .assertIsEnabled()
            .assert(stateIs("Booking"))
            .performClick()
        assertEquals(0, clicks)
    }
}
