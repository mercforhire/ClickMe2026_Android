package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.BOOK_NOW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.NOTES_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.TOPIC_PICKER_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
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
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MakeABookingScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val slot20 = Instant.parse("2026-10-22T20:00:00Z")
    private val api = FakeClickMeApi()
    private val booked = mutableListOf<String>()
    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private var backCount = 0
    private var bookCount = 0
    private var dismissCount = 0
    private val pickedTopics = mutableListOf<String>()
    private val formInputs = mutableListOf<String>()

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
            zone = zone,
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
                    onPreviousMonth = { formInputs += "previous" },
                    onNextMonth = { formInputs += "next" },
                    onSelectDate = { formInputs += "date $it" },
                    onSelectSlot = { formInputs += "slot $it" },
                    onRetryAvailability = {},
                    onSelectMeetingType = { formInputs += "meeting $it" },
                    onNotesChange = { formInputs += "notes $it" },
                    onBook = { bookCount++ },
                    onDismissError = { dismissCount++ },
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
    fun theFormIsLockedWhileSubmitting() {
        setContent(loadedState(isSubmitting = true))

        composeRule
            .onNodeWithTag(TOPIC_PICKER_TAG)
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()
        composeRule.onNodeWithText("Choose a Topic").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Next month")
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()
        composeRule
            .onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 23)))
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()
        composeRule
            .onNodeWithTag(slotChipTestTag(slot20))
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()
        composeRule
            .onNodeWithText("Skype/Zoom Call")
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()

        assertTrue("no form input may reach the ViewModel: $formInputs", formInputs.isEmpty())
    }

    @Test
    fun theFormIsEditableWhenIdle() {
        setContent(loadedState())

        composeRule
            .onNodeWithContentDescription("Next month")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule
            .onNodeWithText("Skype/Zoom Call")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf("next", "meeting ${MeetingType.SkypeZoom}"), formInputs)
    }

    @Test
    fun backCallsOnBackOnceWhenIdle() {
        setContent(loadedState())
        composeRule.onNodeWithContentDescription("Back").assertIsEnabled().performClick()
        assertEquals(1, backCount)
        assertTrue(pickedTopics.isEmpty())
    }

    // ---- CR-01: only a user drag clears focus; programmatic scrolls (bring-into-view, imePadding) do not ----

    private var viewportHeight by mutableStateOf(640.dp)

    private fun setContentInViewport(state: MakeABookingUiState) {
        composeRule.setContent {
            ClickMeTheme {
                Box(Modifier.height(viewportHeight)) {
                    MakeABookingContent(
                        state = state,
                        onBack = {},
                        onRetryTopics = {},
                        onSelectTopic = {},
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onSelectDate = {},
                        onSelectSlot = {},
                        onRetryAvailability = {},
                        onSelectMeetingType = {},
                        onNotesChange = {},
                        onBook = {},
                        onDismissError = {},
                        timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    )
                }
            }
        }
    }

    @Test
    fun notesFieldKeepsFocusWhenTheViewportShrinks() {
        setContentInViewport(loadedState())

        composeRule.onNodeWithTag(NOTES_FIELD_TAG).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NOTES_FIELD_TAG).assertIsFocused()

        // The same viewport shrink `imePadding` causes when the keyboard opens; the scroll container then
        // scrolls programmatically to keep the focused field visible.
        viewportHeight = 240.dp
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NOTES_FIELD_TAG).assertIsFocused()
    }

    @Test
    fun aUserDragClearsNotesFocus() {
        setContentInViewport(loadedState())

        composeRule.onNodeWithTag(NOTES_FIELD_TAG).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NOTES_FIELD_TAG).assertIsFocused()

        composeRule.onNodeWithTag(MAKE_A_BOOKING_SCROLL_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NOTES_FIELD_TAG).assertIsNotFocused()
    }

    // ---- Task 2: real ViewModel + FakeClickMeApi, the in-JVM stand-in for the deferred device check ----

    private fun stubFreeAndPaidTopicsWithOneSlot() {
        api.expertDetailsHandler = {
            detailsWith(
                topicDto("t1", isFree = true, durationMins = 60),
                topicDto("t2", isFree = false, amount = 5000, currency = "CAD"),
            )
        }
        api.expertAvailabilityHandler = { _, month ->
            if (month == "2026-10") {
                availabilityOf(slotDto("2026-10-22T20:00:00Z"))
            } else {
                availabilityOf()
            }
        }
        api.requestBookingHandler = { BookingRequestData(bookingId = "b1") }
    }

    private fun setScreen(viewModel: MakeABookingViewModel) {
        composeRule.setContent {
            ClickMeTheme {
                MakeABookingScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onBooked = { booked += it },
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun pickSlotAndBook() {
        composeRule.onNodeWithTag(slotChipTestTag(slot20)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun freeBookingSendsOneRequestAndCallsOnBookedOnce() {
        stubFreeAndPaidTopicsWithOneSlot()
        val vm = newViewModel(api)
        setScreen(vm)

        pickSlotAndBook()

        assertEquals(listOf("b1"), booked)
        assertEquals(1, api.requestBookingCalls)
        assertEquals(
            BookingRequestBody(
                expertId = "e1",
                topicId = "t1",
                scheduledStart = "2026-10-22T20:00:00Z",
                durationMinutes = 60,
                meetingType = "in_app_voice",
                timezone = "America/Toronto",
                clientNotes = null,
            ),
            api.requestBookingRequests.single(),
        )
        assertNull(vm.uiState.value.completedBookingId)
    }

    @Test
    fun aBookingRestoredAfterProcessDeathNavigatesToRequestSentOnce() {
        stubFreeAndPaidTopicsWithOneSlot()
        val vm = newViewModel(api, SavedStateHandle(mapOf("booking_completed_id" to "b9")))
        setScreen(vm)

        assertEquals(listOf("b9"), booked)
        assertEquals(0, api.requestBookingCalls)
        assertNull(vm.uiState.value.completedBookingId)
    }

    @Test
    fun slotTakenShowsTheDialogReloadsThreeMonthsAndOkDismisses() {
        stubFreeAndPaidTopicsWithOneSlot()
        api.requestBookingHandler = {
            throw api.httpError(409, """{"status":"error","code":"SLOT_UNAVAILABLE","message":"x"}""")
        }
        val vm = newViewModel(api)
        setScreen(vm)
        val callsBefore = api.expertAvailabilityCalls

        pickSlotAndBook()

        composeRule.onNodeWithText("Couldn't book").assertExists()
        composeRule.onNodeWithText("That time slot was just taken. Please pick another.").assertExists()
        assertEquals(callsBefore + 3, api.expertAvailabilityCalls)
        assertTrue(booked.isEmpty())

        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't book").assertDoesNotExist()
    }

    @Test
    fun paidTopicShowsTheNoteAndKeepsBookNowDisabled() {
        stubFreeAndPaidTopicsWithOneSlot()
        val vm = newViewModel(api)
        setScreen(vm)

        vm.selectTopic("t2")
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("Booking paid sessions is coming soon. Choose a free topic to book now.")
            .assertExists()
        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsNotEnabled()
        assertEquals(0, api.requestBookingCalls)
    }
}
