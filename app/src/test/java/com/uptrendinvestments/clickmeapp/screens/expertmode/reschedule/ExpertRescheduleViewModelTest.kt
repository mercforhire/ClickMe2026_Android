@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.requests.ExpertRescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.availabilityOf
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.slotDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.PickerLoad
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.ReschedulePickerDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSent
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class ExpertRescheduleViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse("2026-11-02T13:00:00Z"))
    private val sentRequest = RescheduleSentRequest()
    private val callGuard = FakeCallGuard()
    private lateinit var previousZone: TimeZone

    private val availabilityExpertIds = mutableListOf<String>()
    private val bookingStart = Instant.parse("2026-11-06T15:00:00Z")
    private val slotNov5 = Instant.parse("2026-11-05T16:00:00Z")

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        stubBooking()
        api.expertAvailabilityHandler = { expertId, _ ->
            availabilityExpertIds += expertId
            availabilityOf(slotDto("2026-11-05T16:00:00Z"), slotDto("2026-11-06T15:00:00Z"))
        }
        api.rescheduleExpertBookingHandler = { _, _ -> RescheduleResponseData(bookingId = "b1", status = "pending") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun stubBooking(detail: ExpertBookingDetail = expertBookingDetail(start = bookingStart)) {
        api.expertBookingDetailsHandler = { detail }
    }

    private fun stubSlots(vararg starts: String) {
        api.expertAvailabilityHandler = { _, _ -> availabilityOf(*starts.map { slotDto(it) }.toTypedArray()) }
    }

    private fun vm(
        userId: String? = "x1",
        opener: RescheduleOpener = RescheduleOpener.ExpertSummary,
    ) = ExpertRescheduleViewModel(
        ExpertRescheduleKey("b1", ExpertBookingOrigin.Bookings, opener),
        SavedStateHandle(),
        ExpertBookingsRepository(api),
        BookingRepository(api),
        ExpertRescheduleProposer(ExpertBookingsRepository(api)),
        sentRequest,
        callGuard,
        CurrentUserIdProvider { userId },
        clock,
    )

    private fun TestScope.loaded(): ExpertRescheduleViewModel {
        val vm = vm()
        advanceUntilIdle()
        return vm
    }

    private fun serverError(
        code: String,
        status: Int = 422,
    ) = api.httpError(status, """{"code":"$code","message":"Server says no"}""")

    private fun TestScope.withSlotChosen(): ExpertRescheduleViewModel {
        val vm = loaded()
        vm.selectDate(LocalDate.of(2026, 11, 5))
        vm.selectSlot(slotNov5)
        return vm
    }

    @Test
    fun loadsOwnAvailabilityExcludingBooking() =
        runTest(main.testDispatcher) {
            val vm = loaded()

            assertTrue(vm.uiState.value.load is PickerLoad.Loaded)
            assertEquals(3, api.expertAvailabilityCalls)
            assertEquals(listOf<String?>("b1", "b1", "b1"), api.availabilityExcludeBookingIds)
            assertEquals(setOf("x1"), availabilityExpertIds.toSet())
        }

    @Test
    fun mapsExpertDetail() =
        runTest(main.testDispatcher) {
            val detail = expertBookingDetail(start = bookingStart, clientName = null, topic = null, fee = 75.0)
            val booking = detail.toPickerBooking("x1", ZoneId.of("America/Toronto"))

            assertEquals(RescheduleViewer.Expert, booking.viewer)
            assertNull(booking.counterpartName)
            assertNull(booking.subtitle)
            assertTrue(booking.isPaid)
            assertNull(booking.authorizationExpiresAt)
            assertEquals("America/Toronto", booking.slotsTimezone)
            assertEquals("x1", booking.availabilityExpertId)
            assertEquals(bookingStart, booking.currentStart)
            assertEquals(bookingStart.plusSeconds(3600), booking.currentEnd)
        }

    @Test
    fun paidExpertBookingCapsAtSixDays() =
        runTest(main.testDispatcher) {
            stubBooking(expertBookingDetail(start = bookingStart, fee = 75.0))
            stubSlots("2026-11-08T13:00:00Z", "2026-11-08T13:00:01Z")
            val vm = loaded()
            val cap = clock.instant().plus(Duration.ofDays(6))
            assertEquals(cap, vm.uiState.value.latestSelectableStart)

            vm.selectDate(LocalDate.of(2026, 11, 8))
            vm.selectSlot(cap.plusSeconds(1))
            assertNull(vm.uiState.value.selectedSlotStart)

            stubBooking(expertBookingDetail(start = bookingStart, fee = 0.0))
            stubSlots("2026-11-22T15:00:00Z")
            val free = loaded()
            assertNull(free.uiState.value.latestSelectableStart)
            free.selectDate(LocalDate.of(2026, 11, 22))
            free.selectSlot(Instant.parse("2026-11-22T15:00:00Z"))
            assertEquals(Instant.parse("2026-11-22T15:00:00Z"), free.uiState.value.selectedSlotStart)
        }

    @Test
    fun submitSendsOneCardFreePatchAndHandsOff() =
        runTest(main.testDispatcher) {
            val vm = withSlotChosen()
            vm.updateMessage(" Conflict ")

            vm.submit()
            // Anything but a disclosure while the send is in flight or after it.
            assertFalse(vm.uiState.value.dialog is ReschedulePickerDialog.Disclosure)
            advanceUntilIdle()

            assertEquals(1, api.rescheduleExpertBookingCalls)
            assertEquals(
                ExpertRescheduleProposalRequest("2026-11-05T16:00:00Z", "America/Toronto", "Conflict"),
                api.rescheduleExpertBookingRequests.single(),
            )
            assertEquals(0, api.getPaymentMethodsCalls)
            assertNull(vm.uiState.value.dialog)
            assertEquals(RescheduleSent("b1"), sentRequest.expertSummary.pending.value)
            assertNull(sentRequest.expertBookings.pending.value)
            assertNull(sentRequest.myBookings.pending.value)
            assertNull(sentRequest.upcomingDetails.pending.value)
            assertTrue(vm.uiState.value.sent)

            vm.consumeSent()
            assertFalse(vm.uiState.value.sent)
        }

    @Test
    fun outsideAuthWindowShowsExpertCopy() =
        runTest(main.testDispatcher) {
            api.rescheduleExpertBookingHandler = { _, _ -> throw serverError("RESCHEDULE_OUTSIDE_AUTH_WINDOW") }
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.reschedule_start_error_outside_window_expert)),
                vm.uiState.value.dialog,
            )
            assertEquals(0, api.getPaymentMethodsCalls)
        }

    @Test
    fun slotConflictClearsSlot() =
        runTest(main.testDispatcher) {
            api.rescheduleExpertBookingHandler = { _, _ -> throw serverError("SLOT_UNAVAILABLE", 409) }
            val vm = withSlotChosen()
            val before = api.expertAvailabilityCalls

            vm.submit()
            advanceUntilIdle()

            assertNull(vm.uiState.value.selectedSlotStart)
            assertEquals(before + 3, api.expertAvailabilityCalls)
            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.expert_reschedule_error_slot_unavailable)),
                vm.uiState.value.dialog,
            )
            assertTrue(vm.uiState.value.availability is AvailabilityState.Loaded)
        }

    @Test
    fun missingUserIdFailsLoad() =
        runTest(main.testDispatcher) {
            val vm = vm(userId = null)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.load is PickerLoad.Failed)
            assertEquals(0, api.expertAvailabilityCalls)
        }
}
