@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.TimeZone

class MakeABookingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private lateinit var previousZone: TimeZone

    private val notFound = """{"status":"error","code":"NOT_FOUND","message":"Expert not found"}"""

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        api.expertDetailsHandler = { detailsWith(topicDto("t1", isFree = true)) }
        api.expertAvailabilityHandler = { _, _ -> availabilityOf() }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun vm(savedState: SavedStateHandle = SavedStateHandle()) = newViewModel(api, savedState)

    // ---- Task 1: topics and initial state ----

    @Test
    fun initialStateComesFromTheKeyAndTheClock() =
        runTest(main.testDispatcher) {
            val vm = vm()
            val s = vm.uiState.value
            assertEquals("e1", s.expertId)
            assertEquals("Ada Lovelace", s.expertName)
            assertEquals("Engineer", s.expertTitle)
            assertNull(s.expertImageUrl)
            assertEquals(LocalDate.of(2026, 10, 22), s.today)
            assertEquals(LocalDate.of(2026, 10, 22), s.selectedDate)
            assertEquals(YearMonth.of(2026, 10), s.displayedMonth)
            assertEquals(MeetingType.InAppVoice, s.meetingType)
            assertEquals("", s.notes)
            assertEquals(Instant.parse(NOW), s.now)
        }

    @Test
    fun firstFreeTopicIsPreselected() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(
                    topicDto("t1", isFree = false, amount = 5000, currency = "CAD"),
                    topicDto("t2", isFree = true),
                    topicDto("t3", isFree = true),
                )
            }
            val vm = vm()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(3, s.topicList.size)
            assertEquals("t2", s.selectedTopicId)
        }

    @Test
    fun firstTopicIsTheFallbackWhenNoneIsFree() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(
                    topicDto("t1", isFree = false, amount = 5000, currency = "CAD"),
                    topicDto("t2", isFree = false, amount = 6000, currency = "CAD"),
                )
            }
            val vm = vm()
            runCurrent()
            assertEquals("t1", vm.uiState.value.selectedTopicId)
        }

    @Test
    fun noTopicsLeavesNothingSelected() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { detailsWith() }
            val vm = vm()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(TopicsState.Loaded(emptyList()), s.topics)
            assertNull(s.selectedTopicId)
            assertEquals(BookDisabledReason.NoTopic, s.bookDisabledReason)
        }

    @Test
    fun topicsFailureShowsTheServerMessageAndRetryRecovers() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { throw api.httpError(404, notFound) }
            val vm = vm()
            runCurrent()
            assertEquals(TopicsState.Failed(FieldError.Text("Expert not found")), vm.uiState.value.topics)

            api.expertDetailsHandler = { detailsWith(topicDto("t1", isFree = true)) }
            vm.retryTopics()
            assertEquals(TopicsState.Loading, vm.uiState.value.topics)
            runCurrent()
            assertTrue(vm.uiState.value.topics is TopicsState.Loaded)
            assertEquals(2, api.expertDetailsCalls)
        }

    @Test
    fun topicsNetworkFailureShowsTheNetworkCopy() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()
            assertEquals(
                TopicsState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.topics,
            )
        }

    @Test
    fun savedTopicOverridesTheDefaultWhenPresent() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(topicDto("t1", isFree = true), topicDto("t2", isFree = true), topicDto("t3", isFree = true))
            }
            val vm = vm(SavedStateHandle(mapOf("booking_topic_id" to "t3")))
            runCurrent()
            assertEquals("t3", vm.uiState.value.selectedTopicId)
        }

    @Test
    fun unknownSavedTopicFallsBackToTheDefault() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(
                    topicDto("t1", isFree = false, amount = 100, currency = "CAD"),
                    topicDto("t2", isFree = true),
                )
            }
            val vm = vm(SavedStateHandle(mapOf("booking_topic_id" to "gone")))
            runCurrent()
            assertEquals("t2", vm.uiState.value.selectedTopicId)
        }

    // ---- Task 2: availability, navigation, selection, persistence ----

    private val today = LocalDate.of(2026, 10, 22)
    private val future = Instant.parse("2026-10-22T20:00:00Z")
    private val past = Instant.parse("2026-10-22T14:00:00Z")

    private fun loadedSlots(vm: MakeABookingViewModel) = (vm.uiState.value.availability as AvailabilityState.Loaded)

    @Test
    fun initLoadsThreeMonthsWithoutATopicId() =
        runTest(main.testDispatcher) {
            vm()
            runCurrent()
            assertEquals(3, api.expertAvailabilityCalls)
            assertEquals(setOf("2026-09", "2026-10", "2026-11"), api.availabilityMonths.toSet())
            assertTrue(api.availabilityTopicIds.all { it == null })
        }

    @Test
    fun centerSlotsAreGroupedByLocalDateAndOpenDaysIgnorePastSlots() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ ->
                availabilityOf(slotDto("2026-10-22T20:00:00Z"), slotDto("2026-10-22T14:00:00Z"))
            }
            val vm = vm()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(listOf(past, future), s.slotsForSelectedDate.map { it.start })
            assertTrue(today in s.openDays)
            assertEquals("America/Vancouver", s.expertTimezone)
        }

    @Test
    fun daysWithOnlyPastOrUnavailableSlotsAreNotOpen() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ ->
                availabilityOf(
                    slotDto("2026-10-22T14:00:00Z"),
                    slotDto("2026-10-24T18:00:00Z", available = false),
                    slotDto("2026-10-25T18:00:00Z", held = true),
                )
            }
            val vm = vm()
            runCurrent()
            assertTrue(
                vm.uiState.value.openDays
                    .isEmpty(),
            )
        }

    @Test
    fun centerFailureFailsTheSectionAndRetryRecovers() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, month ->
                if (month == "2026-10") throw IOException("offline") else availabilityOf()
            }
            val vm = vm()
            runCurrent()
            assertEquals(
                AvailabilityState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.availability,
            )
            api.expertAvailabilityHandler = { _, _ -> availabilityOf() }
            vm.retryAvailability()
            assertEquals(AvailabilityState.Loading, vm.uiState.value.availability)
            runCurrent()
            assertTrue(vm.uiState.value.availability is AvailabilityState.Loaded)
        }

    @Test
    fun neighborFailureIsSilent() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, month ->
                if (month == "2026-09") {
                    throw IOException("offline")
                } else {
                    availabilityOf(slotDto("2026-10-22T20:00:00Z"))
                }
            }
            val vm = vm()
            runCurrent()
            assertEquals(listOf(future), loadedSlots(vm).slotsByDate[today]?.map { it.start })
        }

    @Test
    fun nextMonthLoadsThreeNewMonthsClearsTheSlotAndKeepsTheDay() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-22T20:00:00Z")) }
            val vm = vm()
            runCurrent()
            vm.selectSlot(future)
            assertEquals(future, vm.uiState.value.selectedSlotStart)

            vm.goToNextMonth()
            val s = vm.uiState.value
            assertEquals(YearMonth.of(2026, 11), s.displayedMonth)
            assertEquals(AvailabilityState.Loading, s.availability)
            assertEquals(today, s.selectedDate)
            assertNull(s.selectedSlotStart)
            runCurrent()
            assertEquals(
                setOf("2026-09", "2026-10", "2026-11", "2026-12"),
                api.availabilityMonths.toSet(),
            )
            assertEquals(6, api.expertAvailabilityCalls)
        }

    @Test
    fun staleResponseNeverOverwritesTheNewerMonth() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ExpertAvailabilityData>()
            var first = true
            api.expertAvailabilityHandler = { _, month ->
                if (month == "2026-10" && first) {
                    first = false
                    gate.await()
                } else {
                    availabilityOf()
                }
            }
            val vm = vm()
            runCurrent()
            vm.goToNextMonth()
            runCurrent()
            val marker = Instant.parse("2026-10-30T20:00:00Z")
            gate.complete(availabilityOf(slotDto("2026-10-30T20:00:00Z")))
            runCurrent()
            assertEquals(YearMonth.of(2026, 11), vm.uiState.value.displayedMonth)
            assertTrue(
                loadedSlots(vm)
                    .slotsByDate.values
                    .flatten()
                    .none { it.start == marker },
            )
        }

    @Test
    fun previousMonthIsIgnoredOnTheCurrentMonthAndWorksAfterNext() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            vm.goToPreviousMonth()
            runCurrent()
            assertEquals(YearMonth.of(2026, 10), vm.uiState.value.displayedMonth)
            assertEquals(3, api.expertAvailabilityCalls)

            vm.goToNextMonth()
            runCurrent()
            val calls = api.expertAvailabilityCalls
            vm.goToPreviousMonth()
            runCurrent()
            assertEquals(YearMonth.of(2026, 10), vm.uiState.value.displayedMonth)
            assertEquals(calls + 3, api.expertAvailabilityCalls)
        }

    @Test
    fun pastDaysCannotBeSelectedAndADayChangeClearsTheSlot() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-22T20:00:00Z")) }
            val vm = vm()
            runCurrent()
            vm.selectSlot(future)
            vm.selectDate(LocalDate.of(2026, 10, 21))
            assertEquals(today, vm.uiState.value.selectedDate)
            assertEquals(future, vm.uiState.value.selectedSlotStart)

            vm.selectDate(LocalDate.of(2026, 10, 24))
            assertEquals(LocalDate.of(2026, 10, 24), vm.uiState.value.selectedDate)
            assertNull(vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun unselectableSlotsAreIgnored() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ ->
                availabilityOf(
                    slotDto("2026-10-22T20:00:00Z"),
                    slotDto("2026-10-22T14:00:00Z"),
                    slotDto("2026-10-22T21:00:00Z", available = false),
                    slotDto("2026-10-22T22:00:00Z", held = true),
                )
            }
            val vm = vm()
            runCurrent()
            vm.selectSlot(past)
            vm.selectSlot(Instant.parse("2026-10-22T21:00:00Z"))
            vm.selectSlot(Instant.parse("2026-10-22T22:00:00Z"))
            vm.selectSlot(Instant.parse("2026-10-22T23:00:00Z"))
            assertNull(vm.uiState.value.selectedSlotStart)
            vm.selectSlot(future)
            assertEquals(future, vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun topicChangeClearsTheSlotButMeetingTypeAndNotesDoNot() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { detailsWith(topicDto("t1", isFree = true), topicDto("t2", isFree = true)) }
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-22T20:00:00Z")) }
            val vm = vm()
            runCurrent()
            vm.selectSlot(future)

            vm.selectTopic("t1")
            assertEquals(future, vm.uiState.value.selectedSlotStart)
            vm.selectMeetingType(MeetingType.SkypeZoom)
            vm.onNotesChange("Hi")
            assertEquals(future, vm.uiState.value.selectedSlotStart)
            assertEquals(MeetingType.SkypeZoom, vm.uiState.value.meetingType)
            assertEquals("Hi", vm.uiState.value.notes)

            vm.selectTopic("t2")
            assertEquals("t2", vm.uiState.value.selectedTopicId)
            assertNull(vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun notesAreCappedAtFiveHundredCharacters() =
        runTest(main.testDispatcher) {
            val vm = vm()
            vm.onNotesChange("a".repeat(501))
            assertEquals(500, vm.uiState.value.notes.length)
        }

    @Test
    fun selectionSurvivesProcessDeath() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(topicDto("t1", isFree = true), topicDto("t2", isFree = true), topicDto("t3", isFree = true))
            }
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-24T18:00:00Z")) }
            val handle = SavedStateHandle()
            val vm1 = vm(handle)
            runCurrent()
            val slot = Instant.parse("2026-10-24T18:00:00Z")
            vm1.selectTopic("t3")
            vm1.selectDate(LocalDate.of(2026, 10, 24))
            vm1.selectSlot(slot)
            vm1.selectMeetingType(MeetingType.SkypeZoom)
            vm1.onNotesChange("Hello")

            val vm2 = vm(handle)
            runCurrent()
            val s = vm2.uiState.value
            assertEquals("t3", s.selectedTopicId)
            assertEquals(LocalDate.of(2026, 10, 24), s.selectedDate)
            assertEquals(slot, s.selectedSlotStart)
            assertEquals(MeetingType.SkypeZoom, s.meetingType)
            assertEquals("Hello", s.notes)
        }

    @Test
    fun restoredSlotThatIsNoLongerAvailableIsDropped() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-24T18:00:00Z")) }
            val handle = SavedStateHandle()
            val vm1 = vm(handle)
            runCurrent()
            vm1.selectDate(LocalDate.of(2026, 10, 24))
            vm1.selectSlot(Instant.parse("2026-10-24T18:00:00Z"))

            api.expertAvailabilityHandler = { _, _ ->
                availabilityOf(slotDto("2026-10-24T18:00:00Z", available = false))
            }
            val vm2 = vm(handle)
            runCurrent()
            assertNull(vm2.uiState.value.selectedSlotStart)
            assertFalse(handle.contains("booking_slot_start"))
        }

    @Test
    fun displayedMonthSurvivesProcessDeath() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm1 = vm(handle)
            runCurrent()
            vm1.goToNextMonth()
            runCurrent()
            val vm2 = vm(handle)
            runCurrent()
            assertEquals(YearMonth.of(2026, 11), vm2.uiState.value.displayedMonth)
        }

    // ---- WR-03: "today" and the viewer's zone are refreshed, not frozen at creation ----

    @Test
    fun theZoneIsPartOfTheState() =
        runTest(main.testDispatcher) {
            assertEquals(ZoneId.of("America/Toronto"), vm().uiState.value.zone)
        }

    @Test
    fun afterMidnightYesterdayIsNoLongerSelectable() =
        runTest(main.testDispatcher) {
            val clock = MutableClock(Instant.parse(NOW))
            val vm = newViewModel(api, clock = clock)
            runCurrent()
            // 2026-10-23 10:00 in Toronto.
            clock.instant = Instant.parse("2026-10-23T14:00:00Z")
            vm.selectDate(LocalDate.of(2026, 10, 22))
            val s = vm.uiState.value
            assertEquals(LocalDate.of(2026, 10, 23), s.today)
            // The day that became past moved to today rather than staying selected.
            assertEquals(LocalDate.of(2026, 10, 23), s.selectedDate)
        }

    @Test
    fun aMonthRolloverOnResumeMovesToTheCurrentMonthAndBlocksThePastOne() =
        runTest(main.testDispatcher) {
            val clock = MutableClock(Instant.parse(NOW))
            val handle = SavedStateHandle()
            val vm = newViewModel(api, handle, clock)
            runCurrent()
            val callsBefore = api.expertAvailabilityCalls
            // 2026-11-01 12:00 in Toronto.
            clock.instant = Instant.parse("2026-11-01T17:00:00Z")
            vm.refreshClock()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(LocalDate.of(2026, 11, 1), s.today)
            assertEquals(LocalDate.of(2026, 11, 1), s.selectedDate)
            assertEquals(YearMonth.of(2026, 11), s.displayedMonth)
            assertFalse(s.canGoToPreviousMonth)
            assertEquals(callsBefore + 3, api.expertAvailabilityCalls)
            assertEquals("2026-11", handle.get<String>("booking_displayed_month"))
            vm.goToPreviousMonth()
            assertEquals(YearMonth.of(2026, 11), vm.uiState.value.displayedMonth)
        }

    @Test
    fun aZoneChangeRegroupsTheLoadedSlotsWithoutARequest() =
        runTest(main.testDispatcher) {
            // 2026-10-22 22:00 in Toronto, but 2026-10-23 03:00 in London.
            val late = Instant.parse("2026-10-23T02:00:00Z")
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-23T02:00:00Z")) }
            val vm = vm()
            runCurrent()
            vm.selectSlot(late)
            assertEquals(late, vm.uiState.value.selectedSlotStart)
            val callsBefore = api.expertAvailabilityCalls

            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            vm.refreshClock()
            runCurrent()

            val s = vm.uiState.value
            assertEquals(ZoneId.of("Europe/London"), s.zone)
            assertEquals(callsBefore, api.expertAvailabilityCalls)
            assertTrue(s.slotsForSelectedDate.isEmpty())
            assertEquals(listOf(late), loadedSlots(vm).slotsByDate[LocalDate.of(2026, 10, 23)]?.map { it.start })
            // The held slot is no longer on the selected day in the new zone, so it is dropped.
            assertNull(s.selectedSlotStart)
        }

    @Test
    fun aBookAfterAZoneChangeSendsTheNewZone() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-22T20:00:00Z")) }
            api.requestBookingHandler = { BookingRequestData(bookingId = "b1") }
            val vm = vm()
            runCurrent()
            vm.selectSlot(Instant.parse("2026-10-22T20:00:00Z"))
            // 20:00Z is still 2026-10-22 in Vancouver, so the slot survives the regroup.
            TimeZone.setDefault(TimeZone.getTimeZone("America/Vancouver"))
            vm.book()
            runCurrent()
            assertEquals("America/Vancouver", api.requestBookingRequests.single().timezone)
        }
}
