@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
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
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun successExposesTheBookingIdOnceUntilConsumed() =
        runTest(main.testDispatcher) {
            val vm = readyVm()
            vm.book()
            runCurrent()
            assertEquals("b1", vm.uiState.value.completedBookingId)
            assertFalse(vm.uiState.value.isSubmitting)
            vm.consumeCompletedBooking()
            assertNull(vm.uiState.value.completedBookingId)
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
}
