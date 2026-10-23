package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.BOOK_NOW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.NOTES_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.TOPIC_PICKER_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.stripe.ChallengeOutcome
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeStripePaymentMethodReader
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
    private var openPaymentMethodsCount = 0
    private var dismissCount = 0
    private val pickedTopics = mutableListOf<String>()
    private val formInputs = mutableListOf<String>()

    private fun loadedState(
        selectedTopicId: String = "t1",
        isSubmitting: Boolean = false,
        dialogError: FieldError? = null,
        dialogTitle: Int = R.string.make_a_booking_error_title,
        paymentMethods: PaymentMethodsAvailability = PaymentMethodsAvailability.Unknown,
    ): MakeABookingUiState {
        val topics =
            listOf(
                topicDto("t1", isFree = true, durationMins = 60).toBookingTopicUi(),
                topicDto("t2", isFree = false, amount = 5000, currency = "CAD").toBookingTopicUi(),
                topicDto("t3", isFree = false, amount = 1500, currency = "JPY").toBookingTopicUi(),
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
            paymentMethods = paymentMethods,
            dialogError = dialogError,
            dialogTitle = dialogTitle,
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
                    onOpenPaymentMethods = { openPaymentMethodsCount++ },
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
    fun paidTopicWithoutCardsShowsAddAPaymentMethodEnabled() {
        setContent(loadedState(selectedTopicId = "t2", paymentMethods = PaymentMethodsAvailability.None))

        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Add a Payment Method").assertExists()
        composeRule.onNodeWithText("Book Now").assertDoesNotExist()
    }

    @Test
    fun paidTopicWithCardsAndASlotShowsBookNowEnabled() {
        setContent(loadedState(selectedTopicId = "t2", paymentMethods = PaymentMethodsAvailability.Available))

        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Book Now").assertExists()
    }

    @Test
    fun paidTopicWhileCheckingCardsShowsBookNowDisabled() {
        setContent(loadedState(selectedTopicId = "t2", paymentMethods = PaymentMethodsAvailability.Unknown))

        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Book Now").assertExists()
    }

    @Test
    fun aPaidJpyTopicShowsTheSummaryPriceWithoutDecimals() {
        setContent(loadedState(selectedTopicId = "t3", paymentMethods = PaymentMethodsAvailability.Available))

        composeRule.onAllNodesWithText("¥1,500", substring = true).assertCountEquals(2)
        composeRule.onAllNodesWithText("¥15.00", substring = true).assertCountEquals(0)
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
    fun aSlotPassedNoticeHasANeutralTitle() {
        setContent(
            loadedState(
                dialogError = FieldError.Res(R.string.make_a_booking_error_slot_passed),
                dialogTitle = R.string.make_a_booking_notice_slot_passed_title,
            ),
        )

        composeRule.onNodeWithText("Time slot unavailable").assertExists()
        composeRule.onNodeWithText("That time has passed. Please pick another.").assertExists()
        composeRule.onNodeWithText("Couldn't book").assertDoesNotExist()
    }

    @Test
    fun backArrowIsDisabledWhileSubmitting() {
        setContent(loadedState(isSubmitting = true))
        composeRule.onNodeWithContentDescription("Back").assertIsNotEnabled().performClick()
        assertEquals(0, backCount)
    }

    @Test
    fun backArrowIsEnabledWhenIdle() {
        setContent(loadedState())
        composeRule.onNodeWithContentDescription("Back").assertIsEnabled().performClick()
        assertEquals(1, backCount)
    }

    /**
     * Renders the form under an outer Back handler, like NavDisplay's, sends a system Back and returns how often
     * the outer handler fired.
     */
    private fun systemBackReachesNavigation(isSubmitting: Boolean): Int {
        var navBacks = 0
        var dispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            ClickMeTheme {
                dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                BackHandler { navBacks++ }
                MakeABookingContent(
                    state = loadedState(isSubmitting = isSubmitting),
                    onBack = { backCount++ },
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
                    onOpenPaymentMethods = {},
                    onDismissError = {},
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
        composeRule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        composeRule.waitForIdle()
        return navBacks
    }

    @Test
    fun systemBackIsSwallowedWhileSubmitting() {
        assertEquals(0, systemBackReachesNavigation(isSubmitting = true))
        assertEquals(0, backCount)
    }

    @Test
    fun systemBackReachesNavigationWhenIdle() {
        assertEquals(1, systemBackReachesNavigation(isSubmitting = false))
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
                        onOpenPaymentMethods = {},
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

    /** Whether the screen is in composition; flipping it off and on simulates covering and uncovering the entry. */
    private var screenShown by mutableStateOf(true)

    /** Stripe requests the fake paid host received, in order. The host consumes each one like the real host does. */
    private val hostRequests = mutableListOf<StripeRequest>()
    private val hostAccounts = mutableListOf<String?>()
    private var navBacks = 0
    private var dispatcher: OnBackPressedDispatcher? = null

    /** Stands in for [PaidFlowStripeHost]: records every request, then consumes it (consume-before-launch). */
    @Composable
    private fun RecordingHost(
        state: MakeABookingUiState,
        viewModel: MakeABookingViewModel,
    ) {
        LaunchedEffect(state.stripeRequest) {
            val request = state.stripeRequest ?: return@LaunchedEffect
            hostRequests += request
            hostAccounts += state.challengeAccount
            viewModel.onStripeRequestLaunched()
        }
    }

    private fun setScreen(viewModel: MakeABookingViewModel) {
        composeRule.setContent {
            ClickMeTheme {
                dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                BackHandler { navBacks++ }
                if (screenShown) {
                    MakeABookingScreen(
                        viewModel = viewModel,
                        onBack = {},
                        onBooked = { booked += it },
                        onOpenPaymentMethods = { openPaymentMethodsCount++ },
                        timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                        paidFlowHost = { state -> RecordingHost(state, viewModel) },
                    )
                }
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
    fun paidTopicWithCardsBooksNowOnceASlotIsPickedWithoutAnyRequest() {
        stubFreeAndPaidTopicsWithOneSlot()
        api.stubCards(cardDto("pm_1", isDefault = true))
        val vm = newViewModel(api)
        setScreen(vm)

        vm.selectTopic("t2")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(slotChipTestTag(slot20)).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Book Now").assertExists()
        assertEquals(0, api.requestBookingCalls)
    }

    @Test
    fun paidTopicWithoutCardsShowsAddAPaymentMethodWithoutAnyRequest() {
        stubFreeAndPaidTopicsWithOneSlot()
        api.stubCards()
        val vm = newViewModel(api)
        setScreen(vm)

        vm.selectTopic("t2")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Add a Payment Method").assertExists()
        assertEquals(0, api.requestBookingCalls)
    }

    // ---- D-08 / D-09: the gate opens Payment Methods; coming back reloads the cards and never books ----

    @Test
    fun addAPaymentMethodCtaOpensPaymentMethodsAndDoesNotBook() {
        setContent(loadedState(selectedTopicId = "t2", paymentMethods = PaymentMethodsAvailability.None))

        composeRule.onNodeWithText("Add a Payment Method").performScrollTo().performClick()

        assertEquals(1, openPaymentMethodsCount)
        assertEquals(0, bookCount)
    }

    @Test
    fun bookNowOnAPaidTopicWithCardsBooksAndDoesNotOpenPaymentMethods() {
        setContent(loadedState(selectedTopicId = "t2", paymentMethods = PaymentMethodsAvailability.Available))

        composeRule.onNodeWithText("Book Now").performScrollTo().performClick()

        assertEquals(1, bookCount)
        assertEquals(0, openPaymentMethodsCount)
    }

    @Test
    fun comingBackFromPaymentMethodsReloadsTheCardsKeepsTheFormAndDoesNotBook() {
        stubFreeAndPaidTopicsWithOneSlot()
        val vm = newViewModel(api)
        setScreen(vm)
        vm.selectTopic("t2")
        vm.onNotesChange("Please cover pricing")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(slotChipTestTag(slot20)).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Add a Payment Method").performScrollTo().performClick()
        assertEquals(1, openPaymentMethodsCount)
        val callsBefore = api.getPaymentMethodsCalls

        // The covered entry leaves composition and re-enters when Payment Methods is popped; one card was added.
        api.stubCards(cardDto("pm_1", isDefault = true))
        screenShown = false
        composeRule.waitForIdle()
        screenShown = true
        composeRule.waitForIdle()

        assertTrue("cards must be reloaded on return", api.getPaymentMethodsCalls > callsBefore)
        composeRule.onNodeWithText("Book Now").assertExists()
        composeRule.onNodeWithText("Add a Payment Method").assertDoesNotExist()
        assertEquals("t2", vm.uiState.value.selectedTopicId)
        assertEquals("Please cover pricing", vm.uiState.value.notes)
        assertEquals(slot20, vm.uiState.value.selectedSlotStart)
        assertEquals(0, api.paymentIntentCalls)
        assertEquals(0, api.requestBookingCalls)
        assertTrue(booked.isEmpty())
    }

    // ---- 07-13 Task 1: the paid host slot, driven by a fake host ----

    private fun requiresAction() =
        ApiException.Http(
            status = 402,
            code = ErrorCode.PaymentRequiresAction,
            rawCode = ErrorCode.PaymentRequiresAction.wire,
            serverMessage = "Authentication required",
            fieldErrors = emptyList(),
            detail = mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to "acct_1"),
        )

    private fun paidViewModel(): MakeABookingViewModel {
        paidFixture(api)
        val vm = newViewModel(api, reader = FakeStripePaymentMethodReader().apply { result = "pm_new" })
        setScreen(vm)
        vm.selectTopic("t2")
        composeRule.waitForIdle()
        return vm
    }

    @Test
    fun paidBookNowHandsExactlyOneSheetRequestToTheHostAndLocksTheScreen() {
        val vm = paidViewModel()

        pickSlotAndBook()

        assertEquals(1, hostRequests.count { it is StripeRequest.PresentSheet })
        assertEquals(1, hostRequests.size)
        assertNull(vm.uiState.value.stripeRequest)
        assertEquals(PaidStep.CollectingPaymentMethod, vm.uiState.value.paidStep)
        composeRule
            .onNodeWithTag(BOOK_NOW_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Booking"))
        composeRule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(0, navBacks)
    }

    @Test
    fun aCanceledSheetShowsNoDialogAndBookNowIsEnabledAgain() {
        val vm = paidViewModel()
        pickSlotAndBook()

        vm.onSheetOutcome(SheetOutcome.Canceled)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't book").assertDoesNotExist()
        composeRule.onNodeWithTag(BOOK_NOW_TAG).performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Book Now").assertExists()
        assertEquals(slot20, vm.uiState.value.selectedSlotStart)
        assertEquals(PaidStep.Idle, vm.uiState.value.paidStep)
    }

    @Test
    fun a402HandsTheChallengeToTheHostWithTheConnectedAccountAndBooksOnCompletion() {
        val vm = paidViewModel()
        api.confirmBookingHandler = {
            if (api.confirmBookingCalls == 1) throw requiresAction() else ConfirmBookingData("b9")
        }
        pickSlotAndBook()

        vm.onSheetOutcome(SheetOutcome.Completed)
        composeRule.waitForIdle()

        val challenge = hostRequests.last()
        assertEquals(StripeRequest.RunChallenge("pi_1_secret_x", "acct_1"), challenge)
        assertEquals("acct_1", hostAccounts.last())
        assertTrue(booked.isEmpty())

        vm.onChallengeOutcome(ChallengeOutcome.Completed)
        composeRule.waitForIdle()

        assertEquals(listOf("b9"), booked)
        assertEquals(2, api.confirmBookingCalls)
    }

    @Test
    fun recompositionNeverDeliversAConsumedRequestToTheHostAgain() {
        val vm = paidViewModel()
        pickSlotAndBook()
        vm.onSheetOutcome(SheetOutcome.Canceled)
        composeRule.waitForIdle()
        assertEquals(1, hostRequests.size)

        vm.onNotesChange("force a state change")
        composeRule.waitForIdle()
        screenShown = false
        composeRule.waitForIdle()
        screenShown = true
        composeRule.waitForIdle()

        assertEquals(1, hostRequests.size)
        assertNull(vm.uiState.value.stripeRequest)
    }
}
