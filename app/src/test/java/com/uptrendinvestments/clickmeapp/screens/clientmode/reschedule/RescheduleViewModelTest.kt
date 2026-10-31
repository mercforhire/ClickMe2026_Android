@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.availabilityOf
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.slotDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.TimeZone

class RescheduleViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse("2026-11-02T13:00:00Z"))
    private val sentRequest = RescheduleSentRequest()
    private val callGuard = FakeCallGuard()
    private lateinit var previousZone: TimeZone

    private val bookingStart = Instant.parse("2026-11-06T15:00:00Z")
    private val slotNov5 = Instant.parse("2026-11-05T16:00:00Z")

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        stubBooking()
        api.expertAvailabilityHandler = { _, _ ->
            availabilityOf(slotDto("2026-11-05T16:00:00Z"), slotDto("2026-11-06T15:00:00Z"))
        }
        api.rescheduleClientBookingHandler = { _, _ -> RescheduleResponseData(bookingId = "b1", status = "pending") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun stubBooking(detail: ClientBookingDetail = bookingDetail(start = bookingStart)) {
        api.clientBookingDetailHandler = { detail }
    }

    private fun vm(
        savedState: SavedStateHandle = SavedStateHandle(),
        opener: RescheduleOpener = RescheduleOpener.UpcomingDetails,
    ) = RescheduleViewModel(
        RescheduleKey("b1", opener),
        savedState,
        ClientBookingsRepository(api),
        BookingRepository(api),
        ClientRescheduleProposer(ClientBookingsRepository(api), PaymentMethodsRepository(api)),
        sentRequest,
        callGuard,
        clock,
    )

    // ---- Task 1: tracer ----

    @Test
    fun loadSeedsMonthAndDayFromBooking() =
        runTest(main.testDispatcher) {
            val vm = vm()
            advanceUntilIdle()

            val s = vm.uiState.value
            assertTrue(s.load is PickerLoad.Loaded)
            assertEquals(YearMonth.of(2026, 11), s.displayedMonth)
            assertEquals(LocalDate.of(2026, 11, 6), s.selectedDate)
            assertNull(s.selectedSlotStart)
        }

    @Test
    fun availabilityExcludesThisBooking() =
        runTest(main.testDispatcher) {
            vm()
            advanceUntilIdle()

            assertEquals(listOf("2026-10", "2026-11", "2026-12"), api.availabilityMonths.sorted())
            assertEquals(listOf<String?>("b1", "b1", "b1"), api.availabilityExcludeBookingIds)
        }

    @Test
    fun happyPathSendsOnceAndHandsOff() =
        runTest(main.testDispatcher) {
            val vm = vm(opener = RescheduleOpener.MyBookings)
            advanceUntilIdle()

            vm.selectDate(LocalDate.of(2026, 11, 5))
            vm.selectSlot(slotNov5)
            vm.updateMessage("  Running late  ")
            vm.submit()
            advanceUntilIdle()

            assertEquals(1, api.rescheduleClientBookingCalls)
            assertEquals(
                RescheduleProposalRequest("2026-11-05T16:00:00Z", "America/Toronto", "Running late", null),
                api.rescheduleClientBookingRequests.single(),
            )
            assertEquals(RescheduleSent("b1"), sentRequest.slotFor(RescheduleOpener.MyBookings).pending.value)
            assertNull(sentRequest.slotFor(RescheduleOpener.UpcomingDetails).pending.value)
            assertNull(sentRequest.slotFor(RescheduleOpener.ExpertSummary).pending.value)
            assertNull(sentRequest.slotFor(RescheduleOpener.ExpertBookings).pending.value)
            assertTrue(vm.uiState.value.sent)
            assertFalse(vm.uiState.value.isSubmitting)

            vm.consumeSent()
            assertFalse(vm.uiState.value.sent)
        }

    @Test
    fun loadFailureShowsFailedAndRetryRecovers() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { throw IOException("offline") }
            val vm = vm()
            advanceUntilIdle()

            assertEquals(
                PickerLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )

            stubBooking()
            vm.retryLoad()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.load is PickerLoad.Loaded)
        }

    @Test
    fun toPickerBookingMapsClientDetail() =
        runTest(main.testDispatcher) {
            val detail =
                bookingDetail(
                    start = bookingStart,
                    end = null,
                    expertTitle = null,
                    paymentStatus = "none",
                    isFree = false,
                )
            val booking = detail.toPickerBooking()!!

            assertEquals(RescheduleViewer.Client, booking.viewer)
            assertEquals("Algorithms", booking.subtitle)
            assertEquals(bookingStart.plus(Duration.ofMinutes(60)), booking.currentEnd)
            assertTrue(booking.isPaid)
            assertEquals("e1", booking.availabilityExpertId)

            assertNull(bookingDetail(expertId = null).toPickerBooking())
            stubBooking(bookingDetail(start = bookingStart, expertId = null))
            val vm = vm()
            advanceUntilIdle()
            assertEquals(
                PickerLoad.Failed(FieldError.Res(R.string.login_error_unknown)),
                vm.uiState.value.load,
            )
        }
}
