@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeCompletedBookingStore
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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
import java.util.TimeZone

class MakeABookingViewModelBookTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse(NOW))
    private lateinit var previousZone: TimeZone

    private val slot20 = Instant.parse("2026-10-22T20:00:00Z")
    private val slot21 = Instant.parse("2026-10-22T21:00:00Z")

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        api.expertDetailsHandler = {
            detailsWith(
                topicDto("t1", isFree = true, durationMins = 60),
                topicDto("t2", isFree = false, amount = 5000, currency = "CAD"),
            )
        }
        api.expertAvailabilityHandler = { _, _ -> twoSlots() }
        api.requestBookingHandler = { BookingRequestData(bookingId = "b1") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun twoSlots() = availabilityOf(slotDto("2026-10-22T20:00:00Z"), slotDto("2026-10-22T21:00:00Z"))

    /** A loaded view model with the free topic t1 and slot 20:00Z selected. */
    private fun TestScope.readyVm(): MakeABookingViewModel {
        val vm = newViewModel(api, clock = clock)
        runCurrent()
        vm.selectSlot(slot20)
        return vm
    }

    // ---- Task 1: happy path, body, single flight, guards ----

    @Test
    fun freeBookingSendsTheExactBody() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.onNotesChange("  Talk about ML  ")
            vm.book()
            runCurrent()
            assertEquals(1, api.requestBookingCalls)
            assertEquals(
                BookingRequestBody(
                    expertId = "e1",
                    topicId = "t1",
                    scheduledStart = "2026-10-22T20:00:00Z",
                    durationMinutes = 60,
                    meetingType = "in_app_voice",
                    timezone = "America/Toronto",
                    clientNotes = "Talk about ML",
                ),
                api.requestBookingRequests.single(),
            )
        }

    @Test
    fun blankNotesAreOmittedAndSkypeZoomUsesItsWireValue() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.onNotesChange("   ")
            vm.selectMeetingType(MeetingType.SkypeZoom)
            vm.book()
            runCurrent()
            val body = api.requestBookingRequests.single()
            assertNull(body.clientNotes)
            assertEquals("skype_zoom", body.meetingType)
        }

    @Test
    fun missingTopicDurationSendsThirty() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { detailsWith(topicDto("t1", isFree = true, durationMins = null)) }
            val vm = readyVm()
            vm.book()
            runCurrent()
            assertEquals(30, api.requestBookingRequests.single().durationMinutes)
        }

    @Test
    fun aSecondTapWhileInFlightIsIgnored() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingRequestData>()
            api.requestBookingHandler = { gate.await() }
            val vm = readyVm()
            vm.book()
            runCurrent()
            assertTrue(vm.uiState.value.isSubmitting)
            vm.book()
            runCurrent()
            assertEquals(1, api.requestBookingCalls)
            assertTrue(vm.uiState.value.isSubmitting)
            gate.complete(BookingRequestData(bookingId = "b1"))
            runCurrent()
            assertEquals("b1", vm.uiState.value.completedBookingId)
            assertEquals(1, api.requestBookingCalls)
        }

    @Test
    fun successExposesTheBookingIdOnceUntilConsumed() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.book()
            runCurrent()
            assertEquals("b1", vm.uiState.value.completedBookingId)
            // WR-01: success is terminal, so the form stays locked until the entry leaves.
            assertTrue(vm.uiState.value.isSubmitting)
            vm.consumeCompletedBooking()
            assertNull(vm.uiState.value.completedBookingId)
            assertTrue(vm.uiState.value.isSubmitting)
        }

    @Test
    fun aTapAfterSuccessAndConsumptionNeverSendsASecondRequest() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.book()
            runCurrent()
            vm.consumeCompletedBooking()
            // The outgoing entry is still composed during the push transition; another slot and tap go nowhere.
            vm.selectSlot(slot21)
            vm.book()
            runCurrent()
            assertEquals(1, api.requestBookingCalls)
            assertNull(vm.uiState.value.completedBookingId)
            assertTrue(vm.uiState.value.isSubmitting)
        }

    // ---- WR-04: selections are locked while a booking is in flight ----

    @Test
    fun selectionChangesAreIgnoredWhileInFlight() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingRequestData>()
            api.requestBookingHandler = { gate.await() }
            val vm = readyVm()
            vm.onNotesChange("Hi")
            vm.book()
            runCurrent()
            val before = vm.uiState.value
            val availabilityCalls = api.expertAvailabilityCalls

            vm.selectSlot(slot21)
            vm.selectTopic("t2")
            vm.selectDate(before.selectedDate.plusDays(1))
            vm.selectMeetingType(MeetingType.SkypeZoom)
            vm.goToNextMonth()
            vm.goToPreviousMonth()
            vm.onNotesChange("Changed")
            runCurrent()

            val after = vm.uiState.value
            assertEquals(slot20, after.selectedSlotStart)
            assertEquals("t1", after.selectedTopicId)
            assertEquals(before.selectedDate, after.selectedDate)
            assertEquals(before.displayedMonth, after.displayedMonth)
            assertEquals(MeetingType.InAppVoice, after.meetingType)
            assertEquals("Hi", after.notes)
            assertEquals(availabilityCalls, api.expertAvailabilityCalls)
            gate.complete(BookingRequestData(bookingId = "b1"))
            runCurrent()
        }

    @Test
    fun aSlotTakenAfterATapDuringFlightClearsTheRejectedSlotOnly() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.requestBookingHandler = {
                gate.await()
                throw api.httpError(409, errorJson("SLOT_UNAVAILABLE"))
            }
            val vm = readyVm()
            vm.book()
            runCurrent()
            vm.selectSlot(slot21)
            gate.complete(Unit)
            runCurrent()
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_slot_taken), s.dialogError)
            assertNull(s.selectedSlotStart)
            assertFalse(s.isSubmitting)
            // Editable again after the failure.
            vm.selectSlot(slot21)
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
        }

    // ---- WR-02: a completed booking survives process death until the screen consumes it ----

    @Test
    fun aCompletedBookingSurvivesProcessDeathUntilConsumed() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm1 = newViewModel(api, handle, clock)
            runCurrent()
            vm1.selectSlot(slot20)
            vm1.book()
            runCurrent()
            assertEquals("b1", vm1.uiState.value.completedBookingId)

            // The process dies before the screen resumes and consumes the id.
            val detailsBefore = api.expertDetailsCalls
            val availabilityBefore = api.expertAvailabilityCalls
            val vm2 = newViewModel(api, handle, clock)
            runCurrent()
            val s = vm2.uiState.value
            assertEquals("b1", s.completedBookingId)
            assertTrue(s.isSubmitting)
            assertEquals(detailsBefore, api.expertDetailsCalls)
            assertEquals(availabilityBefore, api.expertAvailabilityCalls)
            vm2.book()
            runCurrent()
            assertEquals(1, api.requestBookingCalls)

            vm2.consumeCompletedBooking()
            assertFalse(handle.contains("booking_completed_id"))
        }

    @Test
    fun aFailedBookingPersistsNoCompletedId() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val store = FakeCompletedBookingStore()
            api.requestBookingHandler = { throw IOException("offline") }
            val vm = newViewModel(api, handle, clock, store)
            runCurrent()
            vm.selectSlot(slot20)
            vm.book()
            runCurrent()
            assertFalse(handle.contains("booking_completed_id"))
            // The failed attempt is forgotten, so a restore does not wait on the durable store for it.
            assertFalse(handle.contains("booking_attempt_id"))
            assertTrue(store.records.isEmpty())
            val restored = newViewModel(api, handle, clock, store)
            assertNull(restored.uiState.value.completedBookingId)
            assertFalse(restored.uiState.value.isSubmitting)
        }

    // ---- WR-01: a success that lands after the activity saved its state survives process death ----

    /**
     * What the activity's state save hands to the system: a copy of the handle's contents at that moment. Writes to
     * the live handle after this point are memory only and are lost with the process.
     */
    private fun SavedStateHandle.savedSnapshot(): SavedStateHandle =
        SavedStateHandle(keys().associateWith { get<Any?>(it) })

    @Test
    fun aSuccessThatLandsAfterTheStateWasSavedSurvivesProcessDeath() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingRequestData>()
            api.requestBookingHandler = { gate.await() }
            val store = FakeCompletedBookingStore()
            val handle = SavedStateHandle()
            val vm1 = newViewModel(api, handle, clock, store)
            runCurrent()
            vm1.selectSlot(slot20)
            vm1.book()
            runCurrent()

            // The user leaves the app with the request in flight: onStop saves the state.
            val saved = handle.savedSnapshot()
            // The response lands in the background, after the save.
            gate.complete(BookingRequestData(bookingId = "b1"))
            runCurrent()
            assertEquals("b1", vm1.uiState.value.completedBookingId)
            // The failure the fix covers: the saved state has no completed id.
            assertFalse(saved.contains("booking_completed_id"))

            // The process is killed; a fresh ViewModel is built from the saved state only.
            val detailsBefore = api.expertDetailsCalls
            val availabilityBefore = api.expertAvailabilityCalls
            val vm2 = newViewModel(api, saved, clock, store)
            // Locked from the first frame while the durable record is read.
            assertTrue(vm2.uiState.value.isSubmitting)
            runCurrent()
            val s = vm2.uiState.value
            assertEquals("b1", s.completedBookingId)
            assertTrue(s.isSubmitting)
            // Nothing is loaded and nothing can be booked again.
            assertEquals(detailsBefore, api.expertDetailsCalls)
            assertEquals(availabilityBefore, api.expertAvailabilityCalls)
            vm2.book()
            runCurrent()
            assertEquals(1, api.requestBookingCalls)

            // Request Sent is shown: the record is consumed and can never resurface.
            vm2.consumeCompletedBooking()
            assertTrue(store.records.isEmpty())
            assertEquals(1, store.discarded.size)
            assertFalse(saved.contains("booking_completed_id"))
            assertFalse(saved.contains("booking_attempt_id"))
        }

    private val unconfirmed = FieldError.Res(R.string.make_a_booking_error_unconfirmed)

    @Test
    fun aRequestLostWithTheProcessReopensTheFormBehindACheckYourBookingsNotice() =
        runTest(main.testDispatcher) {
            api.requestBookingHandler = { CompletableDeferred<BookingRequestData>().await() }
            val store = FakeCompletedBookingStore()
            val handle = SavedStateHandle()
            val vm1 = newViewModel(api, handle, clock, store)
            runCurrent()
            vm1.selectSlot(slot20)
            vm1.book()
            runCurrent()
            val saved = handle.savedSnapshot()

            // The process dies before any response: the server may still have created the booking, so the user is
            // told to check My Bookings rather than being handed a ready-to-book form as if nothing had been sent.
            api.requestBookingHandler = { BookingRequestData(bookingId = "b2") }
            val vm2 = newViewModel(api, saved, clock, store)
            runCurrent()
            val s = vm2.uiState.value
            assertNull(s.completedBookingId)
            assertEquals(unconfirmed, s.dialogError)
            assertFalse(s.isSubmitting)
            assertTrue(s.topics is TopicsState.Loaded)
            assertEquals(slot20, s.selectedSlotStart)
            // Handled once: the attempt id is forgotten, so a later restore does not show the notice again.
            assertFalse(saved.contains("booking_attempt_id"))
            val vm3 = newViewModel(api, saved.savedSnapshot(), clock, store)
            runCurrent()
            assertNull(vm3.uiState.value.dialogError)

            // Once dismissed, the form is usable.
            vm2.dismissError()
            vm2.book()
            runCurrent()
            assertEquals("b2", vm2.uiState.value.completedBookingId)
        }

    @Test
    fun anUnreadableRecordIsNotTreatedAsNoBooking() =
        runTest(main.testDispatcher) {
            val store = FakeCompletedBookingStore()
            store.put("a1", "b1")
            store.unreadable = true
            val restored = newViewModel(api, SavedStateHandle(mapOf("booking_attempt_id" to "a1")), clock, store)
            runCurrent()
            val s = restored.uiState.value
            assertNull(s.completedBookingId)
            assertEquals(unconfirmed, s.dialogError)
            assertFalse(s.isSubmitting)
        }

    @Test
    fun aRestoredSlotThatHasStartedDoesNotReplaceTheCheckYourBookingsNotice() =
        runTest(main.testDispatcher) {
            val handle =
                SavedStateHandle(mapOf("booking_attempt_id" to "a1", "booking_slot_start" to slot20.toEpochMilli()))
            clock.instant = slot20.plusSeconds(60)
            val restored = newViewModel(api, handle, clock, FakeCompletedBookingStore())
            runCurrent()
            assertNull(restored.uiState.value.selectedSlotStart)
            assertEquals(unconfirmed, restored.uiState.value.dialogError)
        }

    @Test
    fun aRecordFromAnotherAttemptNeverResurfaces() =
        runTest(main.testDispatcher) {
            val store = FakeCompletedBookingStore()
            store.put("an-earlier-attempt", "b0")

            // A new booking flow (nothing restored) never reads the store.
            val fresh = newViewModel(api, SavedStateHandle(), clock, store)
            runCurrent()
            assertNull(fresh.uiState.value.completedBookingId)
            assertFalse(fresh.uiState.value.isSubmitting)
            assertNull(fresh.uiState.value.dialogError)

            // A restored flow only accepts the record of its own attempt; its own outcome is unknown, so it says so.
            val restored = newViewModel(api, SavedStateHandle(mapOf("booking_attempt_id" to "another")), clock, store)
            runCurrent()
            assertNull(restored.uiState.value.completedBookingId)
            assertFalse(restored.uiState.value.isSubmitting)
            assertTrue(restored.uiState.value.topics is TopicsState.Loaded)
            assertEquals(unconfirmed, restored.uiState.value.dialogError)
        }

    @Test
    fun paidTopicNeverReachesTheApi() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.selectTopic("t2")
            vm.selectSlot(slot20)
            vm.book()
            runCurrent()
            assertEquals(0, api.requestBookingCalls)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun noSlotNeverReachesTheApi() =
        runTest(main.testDispatcher) {
            val vm = newViewModel(api, clock = clock)
            runCurrent()
            vm.book()
            runCurrent()
            assertEquals(0, api.requestBookingCalls)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun aSlotThatHasStartedIsClearedAndNotSent() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            assertEquals(slot20, vm.uiState.value.selectedSlotStart)
            clock.instant = slot20
            vm.book()
            runCurrent()
            assertEquals(0, api.requestBookingCalls)
            assertNull(vm.uiState.value.selectedSlotStart)
            assertFalse(vm.uiState.value.isSubmitting)
            // IN-09: the user is told why the selection disappeared.
            assertEquals(FieldError.Res(R.string.make_a_booking_error_slot_passed), vm.uiState.value.dialogError)
            assertEquals(R.string.make_a_booking_notice_slot_passed_title, vm.uiState.value.dialogTitle)
            vm.dismissError()
            assertNull(vm.uiState.value.dialogError)
            // The other slot is untouched and still bookable.
            vm.selectSlot(slot21)
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
        }

    // ---- WR-02: a slot that time moved past is cleared; resume and Book Now explain it, a tap does not ----

    private val slotPassed = FieldError.Res(R.string.make_a_booking_error_slot_passed)

    /** The notice is titled neutrally: no booking was attempted, so it must not say "Couldn't book". */
    private fun assertSlotPassedNotice(s: MakeABookingUiState) {
        assertEquals(slotPassed, s.dialogError)
        assertEquals(R.string.make_a_booking_notice_slot_passed_title, s.dialogTitle)
    }

    @Test
    fun aResumeAfterTheSlotStartedClearsItAndSaysWhy() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm = newViewModel(api, handle, clock)
            runCurrent()
            vm.selectSlot(slot20)
            // Same day, the slot started while the app was in the background.
            clock.instant = slot20.plusSeconds(60)
            vm.refreshClock()
            val s = vm.uiState.value
            // Not stranded as selected with Book Now disabled: cleared, and the user is told why.
            assertNull(s.selectedSlotStart)
            assertFalse(handle.contains("booking_slot_start"))
            assertSlotPassedNotice(s)
            assertEquals(BookDisabledReason.NoSlot, s.bookDisabledReason)
            vm.dismissError()
            vm.selectSlot(slot21)
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun aResumeBeforeTheSlotStartsKeepsItWithoutADialog() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            clock.instant = slot20.minusSeconds(60)
            vm.refreshClock()
            assertEquals(slot20, vm.uiState.value.selectedSlotStart)
            assertNull(vm.uiState.value.dialogError)
            assertNull(vm.uiState.value.bookDisabledReason)
        }

    @Test
    fun aBookAfterMidnightClearsTheSlotAndSaysWhy() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            // 2026-10-23 01:00 in Toronto: the selected day is now in the past.
            clock.instant = Instant.parse("2026-10-23T05:00:00Z")
            vm.book()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(0, api.requestBookingCalls)
            assertFalse(s.isSubmitting)
            assertEquals(LocalDate.of(2026, 10, 23), s.selectedDate)
            assertNull(s.selectedSlotStart)
            assertSlotPassedNotice(s)
        }

    @Test
    fun aBookAfterAZoneChangeThatMovesTheSlotsDayClearsItAndSaysWhy() =
        runTest(main.testDispatcher) {
            // 2026-10-22 22:00 in Toronto, but 2026-10-23 03:00 in London.
            val late = Instant.parse("2026-10-23T02:00:00Z")
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-23T02:00:00Z")) }
            val vm = newViewModel(api, clock = clock)
            runCurrent()
            vm.selectSlot(late)
            assertEquals(late, vm.uiState.value.selectedSlotStart)

            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            vm.book()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(0, api.requestBookingCalls)
            assertFalse(s.isSubmitting)
            assertNull(s.selectedSlotStart)
            assertSlotPassedNotice(s)
        }

    @Test
    fun aResumeAfterAZoneChangeThatMovesTheSlotsDayClearsItAndSaysWhy() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-10-23T02:00:00Z")) }
            val vm = newViewModel(api, clock = clock)
            runCurrent()
            vm.selectSlot(Instant.parse("2026-10-23T02:00:00Z"))
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            vm.refreshClock()
            assertNull(vm.uiState.value.selectedSlotStart)
            assertSlotPassedNotice(vm.uiState.value)
        }

    @Test
    fun aZoneChangeThatKeepsTheSlotsDayKeepsItWithoutADialog() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            // 20:00Z is still 2026-10-22 in Vancouver.
            TimeZone.setDefault(TimeZone.getTimeZone("America/Vancouver"))
            vm.refreshClock()
            assertEquals(slot20, vm.uiState.value.selectedSlotStart)
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun aRestoredSlotThatHasStartedIsDroppedAndExplained() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm1 = newViewModel(api, handle, clock)
            runCurrent()
            vm1.selectSlot(slot20)
            // Process death, restored after the slot started.
            clock.instant = slot20.plusSeconds(60)
            val vm2 = newViewModel(api, handle, clock)
            runCurrent()
            assertNull(vm2.uiState.value.selectedSlotStart)
            assertSlotPassedNotice(vm2.uiState.value)
        }

    @Test
    fun aSlotTapAfterTheHeldSlotStartedReplacesItSilently() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            clock.instant = slot20.plusSeconds(60)
            vm.selectSlot(slot21)
            // The tap is the new choice: no dialog over the slot the user just picked.
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun aDateTapAfterTheHeldSlotStartedClearsItSilently() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm = newViewModel(api, handle, clock)
            runCurrent()
            vm.selectSlot(slot20)
            clock.instant = slot20.plusSeconds(60)
            vm.selectDate(LocalDate.of(2026, 10, 24))
            assertEquals(LocalDate.of(2026, 10, 24), vm.uiState.value.selectedDate)
            assertNull(vm.uiState.value.selectedSlotStart)
            assertFalse(handle.contains("booking_slot_start"))
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun aMonthTapAfterTheHeldSlotStartedClearsItSilently() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            clock.instant = slot20.plusSeconds(60)
            vm.goToNextMonth()
            runCurrent()
            assertNull(vm.uiState.value.selectedSlotStart)
            assertNull(vm.uiState.value.dialogError)
            vm.goToPreviousMonth()
            runCurrent()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun aTapThatRegroupsAfterAZoneChangeDropsAPassedSlotSilently() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            // 20:01Z is still 2026-10-22 in Vancouver: the zone regroup itself drops the started slot.
            clock.instant = slot20.plusSeconds(60)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Vancouver"))
            vm.selectSlot(slot21)
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun aResumeAfterAZoneChangeDropsAPassedSlotWithOneNotice() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            clock.instant = slot20.plusSeconds(60)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Vancouver"))
            vm.refreshClock()
            assertNull(vm.uiState.value.selectedSlotStart)
            assertSlotPassedNotice(vm.uiState.value)
        }

    @Test
    fun aBookingFailureIsTitledCouldNotBookEvenAfterANotice() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            clock.instant = slot20.plusSeconds(60)
            vm.refreshClock()
            assertSlotPassedNotice(vm.uiState.value)
            vm.dismissError()
            assertEquals(R.string.make_a_booking_error_title, vm.uiState.value.dialogTitle)

            api.requestBookingHandler = { throw IOException("offline") }
            vm.selectSlot(slot21)
            vm.book()
            runCurrent()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialogError)
            assertEquals(R.string.make_a_booking_error_title, vm.uiState.value.dialogTitle)
        }

    // ---- Task 2: error handling ----

    private fun errorJson(
        code: String,
        message: String = "msg",
    ) = """{"status":"error","code":"$code","message":"$message"}"""

    private fun failWith(
        status: Int,
        code: String,
        message: String = "msg",
    ) {
        api.requestBookingHandler = { throw api.httpError(status, errorJson(code, message)) }
    }

    @Test
    fun slotTakenClearsTheSlotAndReloadsInPlace() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            failWith(409, "SLOT_UNAVAILABLE", "Slot taken")
            val callsBefore = api.expertAvailabilityCalls
            val gate = CompletableDeferred<Unit>()
            api.expertAvailabilityHandler = { _, _ ->
                gate.await()
                availabilityOf(
                    slotDto("2026-10-22T20:00:00Z", available = false),
                    slotDto("2026-10-22T21:00:00Z"),
                )
            }
            vm.book()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_slot_taken), s.dialogError)
            assertNull(s.selectedSlotStart)
            assertFalse(s.isSubmitting)
            assertEquals(callsBefore + 3, api.expertAvailabilityCalls)
            // The reload is suspended, yet the slots stay on screen (no skeleton).
            assertTrue(s.availability is AvailabilityState.Loaded)
            gate.complete(Unit)
            runCurrent()
            vm.selectSlot(slot20)
            assertNull(vm.uiState.value.selectedSlotStart)
            vm.selectSlot(slot21)
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun noMeetingLinkResetsTheMeetingTypeAndKeepsTheSlot() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.selectMeetingType(MeetingType.SkypeZoom)
            failWith(422, "MEETING_LINK_UNAVAILABLE")
            vm.book()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_no_link), s.dialogError)
            assertEquals(MeetingType.InAppVoice, s.meetingType)
            assertEquals(slot20, s.selectedSlotStart)
            assertFalse(s.isSubmitting)
        }

    private fun assertKeepsSlotAndMeeting(
        status: Int,
        code: String,
        expected: FieldError,
    ) = runTest(main.testDispatcher) {
        val vm = readyVm()
        vm.selectMeetingType(MeetingType.SkypeZoom)
        failWith(status, code)
        vm.book()
        runCurrent()
        val s = vm.uiState.value
        assertEquals(expected, s.dialogError)
        assertEquals(slot20, s.selectedSlotStart)
        assertEquals(MeetingType.SkypeZoom, s.meetingType)
        assertFalse(s.isSubmitting)
    }

    @Test
    fun expertNotBookable() =
        assertKeepsSlotAndMeeting(
            422,
            "EXPERT_NOT_BOOKABLE",
            FieldError.Res(R.string.make_a_booking_error_not_bookable),
        )

    @Test
    fun cannotBookSelf() =
        assertKeepsSlotAndMeeting(422, "CANNOT_BOOK_SELF", FieldError.Res(R.string.make_a_booking_error_self))

    @Test
    fun duplicateBooking() =
        assertKeepsSlotAndMeeting(409, "DUPLICATE_BOOKING", FieldError.Res(R.string.make_a_booking_error_duplicate))

    @Test
    fun paidTopicRequiresPayment() =
        assertKeepsSlotAndMeeting(
            422,
            "PAID_TOPIC_REQUIRES_PAYMENT",
            FieldError.Res(R.string.make_a_booking_error_needs_payment),
        )

    @Test
    fun otherCodesShowTheServerMessage() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            failWith(409, "EXPERT_UNAVAILABLE", "This expert is not available for booking.")
            vm.book()
            runCurrent()
            assertEquals(FieldError.Text("This expert is not available for booking."), vm.uiState.value.dialogError)
        }

    @Test
    fun networkFailureShowsTheNetworkCopy() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            api.requestBookingHandler = { throw IOException("offline") }
            vm.book()
            runCurrent()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialogError)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun suspendedAccountShowsNoBookingDialog() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            failWith(403, "ACCOUNT_SUSPENDED")
            vm.book()
            runCurrent()
            assertNull(vm.uiState.value.dialogError)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun dismissClearsTheDialogAndABookAfterAFailureSendsAgain() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            failWith(422, "EXPERT_NOT_BOOKABLE")
            vm.book()
            runCurrent()
            assertEquals(1, api.requestBookingCalls)
            vm.dismissError()
            assertNull(vm.uiState.value.dialogError)
            api.requestBookingHandler = { BookingRequestData(bookingId = "b2") }
            vm.book()
            runCurrent()
            assertEquals(2, api.requestBookingCalls)
            assertEquals("b2", vm.uiState.value.completedBookingId)
        }
}
