@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
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
            api.requestBookingHandler = { throw IOException("offline") }
            val vm = newViewModel(api, handle, clock)
            runCurrent()
            vm.selectSlot(slot20)
            vm.book()
            runCurrent()
            assertFalse(handle.contains("booking_completed_id"))
            assertNull(newViewModel(api, handle, clock).uiState.value.completedBookingId)
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
            // The other slot is untouched and still bookable.
            vm.selectSlot(slot21)
            assertEquals(slot21, vm.uiState.value.selectedSlotStart)
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
