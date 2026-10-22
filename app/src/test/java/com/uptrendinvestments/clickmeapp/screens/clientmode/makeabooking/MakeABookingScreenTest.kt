package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.BOOK_NOW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.TOPIC_PICKER_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MakeABookingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val slot20 = Instant.parse("2026-10-22T20:00:00Z")

    private var backCount = 0
    private var bookCount = 0
    private var dismissCount = 0
    private val pickedTopics = mutableListOf<String>()

    private fun loadedState(
        selectedTopicId: String = "t1",
        isSubmitting: Boolean = false,
        dialogError: FieldError? = null,
    ): MakeABookingUiState {
        val topics =
            listOf(
                topicDto("t1", isFree = true, durationMins = 60).toBookingTopicUi(),
                topicDto("t2", isFree = false, amount = 5000, currency = "CAD").toBookingTopicUi(),
            )
        val date = LocalDate.of(2026, 10, 22)
        return MakeABookingUiState(
            expertId = "e1",
            expertName = "Ada Lovelace",
            expertTitle = "Engineer",
            expertImageUrl = null,
            today = date,
            now = Instant.parse(NOW),
            selectedDate = date,
            displayedMonth = YearMonth.of(2026, 10),
            topics = TopicsState.Loaded(topics),
            selectedTopicId = selectedTopicId,
            availability =
                AvailabilityState.Loaded(
                    mapOf(date to listOf(BookingTimeSlot(slot20, slot20.plusSeconds(1800), isAvailable = true))),
                ),
            expertTimezone = "America/Vancouver",
            selectedSlotStart = slot20,
            meetingType = MeetingType.InAppVoice,
            isSubmitting = isSubmitting,
            dialogError = dialogError,
        )
    }

    private fun setContent(state: MakeABookingUiState) {
        composeRule.setContent {
            ClickMeTheme {
                MakeABookingContent(
                    state = state,
                    onBack = { backCount++ },
                    onRetryTopics = {},
                    onSelectTopic = { pickedTopics += it },
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectDate = {},
                    onSelectSlot = {},
                    onRetryAvailability = {},
                    onSelectMeetingType = {},
                    onNotesChange = {},
                    onBook = { bookCount++ },
                    onDismissError = { dismissCount++ },
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
    }

    private fun top(text: String): Float =
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .positionInRoot.y

    private fun bookNowTop(): Float =
        composeRule
            .onNodeWithTag(BOOK_NOW_TAG)
            .fetchSemanticsNode()
            .positionInRoot.y

    @Test
    fun rendersTheEightSectionsInOrderUnderTheAppBar() {
        setContent(loadedState())

        composeRule.onNodeWithText("Book a Session").assertExists()
        val tops =
            listOf(
                top("Expert"),
                top("Topic of Discussion"),
                top("Select a Time"),
                top("Available Time Slots"),
                top("Meeting Type"),
                top("Message (optional)"),
                top("Summary"),
                bookNowTop(),
            )
        assertEquals("sections must be strictly top-to-bottom: $tops", tops.sorted(), tops)
        assertEquals(tops.size, tops.toSet().size)
    }

    @Test
    fun bookNowIsEnabledForAFreeTopicAndFiresOnBookOnce() {
        setContent(loadedState())

        composeRule
            .onNodeWithTag(BOOK_NOW_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, bookCount)
    }

    @Test
    fun paidTopicDisablesBookNowAndShowsThePaidNote() {
        setContent(loadedState(selectedTopicId = "t2"))

        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsNotEnabled()
        composeRule
            .onNodeWithText("Booking paid sessions is coming soon. Choose a free topic to book now.")
            .assertExists()
    }

    @Test
    fun topicRowOpensTheSheetAndChoosingATopicClosesIt() {
        setContent(loadedState())
        composeRule.onNodeWithText("Choose a Topic").assertDoesNotExist()

        composeRule.onNodeWithTag(TOPIC_PICKER_TAG).performScrollTo().performClick()
        composeRule.onNodeWithText("Choose a Topic").assertExists()

        composeRule.onNodeWithText("Topic t2").performClick()

        assertEquals(listOf("t2"), pickedTopics)
        composeRule.onNodeWithText("Choose a Topic").assertDoesNotExist()
    }

    @Test
    fun dialogErrorShowsTheMappedMessageAndOkDismisses() {
        setContent(loadedState(dialogError = FieldError.Res(R.string.make_a_booking_error_slot_taken)))

        composeRule.onNodeWithText("Couldn't book").assertExists()
        composeRule.onNodeWithText("That time slot was just taken. Please pick another.").assertExists()

        composeRule.onNodeWithText("OK").performClick()

        assertEquals(1, dismissCount)
    }

    @Test
    fun backIsDisabledWhileSubmittingAndEnabledOtherwise() {
        setContent(loadedState(isSubmitting = true))
        composeRule.onNodeWithContentDescription("Back").assertIsNotEnabled().performClick()
        assertEquals(0, backCount)
    }

    @Test
    fun backCallsOnBackOnceWhenIdle() {
        setContent(loadedState())
        composeRule.onNodeWithContentDescription("Back").assertIsEnabled().performClick()
        assertEquals(1, backCount)
        assertTrue(pickedTopics.isEmpty())
    }
}
