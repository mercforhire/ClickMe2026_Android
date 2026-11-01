@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.availabilityOf
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.cardDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.slotDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.stubCards
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.CompletableDeferred
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

    private fun stubSlots(vararg starts: String) {
        api.expertAvailabilityHandler = { _, _ -> availabilityOf(*starts.map { slotDto(it) }.toTypedArray()) }
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

    private fun TestScope.loaded(savedState: SavedStateHandle = SavedStateHandle()): RescheduleViewModel {
        val vm = vm(savedState)
        advanceUntilIdle()
        return vm
    }

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

    // ---- Task 2: selection rules, month moves, clock, restore ----

    @Test
    fun paidBookingCapsAtSixDays() =
        runTest(main.testDispatcher) {
            stubBooking(
                bookingDetail(
                    start = bookingStart,
                    paymentStatus = "held",
                    authorizationExpiresAt = clock.instant().plus(Duration.ofDays(1)),
                ),
            )
            stubSlots("2026-11-08T13:00:00Z", "2026-11-08T13:00:01Z")
            val vm = loaded()
            val cap = clock.instant().plus(Duration.ofDays(6))
            assertEquals(cap, vm.uiState.value.latestSelectableStart)

            vm.selectDate(LocalDate.of(2026, 11, 8))
            vm.selectSlot(cap.plusSeconds(1))
            assertNull(vm.uiState.value.selectedSlotStart)
            vm.selectSlot(cap)
            assertEquals(cap, vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun freeBookingHasNoCap() =
        runTest(main.testDispatcher) {
            stubBooking(bookingDetail(start = bookingStart, isFree = true, paymentStatus = "none"))
            stubSlots("2026-11-22T15:00:00Z")
            val vm = loaded()
            assertNull(vm.uiState.value.latestSelectableStart)

            vm.selectDate(LocalDate.of(2026, 11, 22))
            vm.selectSlot(Instant.parse("2026-11-22T15:00:00Z"))
            assertEquals(Instant.parse("2026-11-22T15:00:00Z"), vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun currentSlotCannotBeSelected() =
        runTest(main.testDispatcher) {
            stubBooking(
                bookingDetail(
                    start = bookingStart,
                    authorizationExpiresAt = clock.instant().plus(Duration.ofDays(1)),
                ),
            )
            stubSlots("2026-11-06T15:00:00Z", "2026-11-06T16:00:00Z")
            val vm = loaded()

            vm.selectSlot(bookingStart)
            assertNull(vm.uiState.value.selectedSlotStart)
            vm.selectSlot(Instant.parse("2026-11-06T16:00:00Z"))
            assertEquals(Instant.parse("2026-11-06T16:00:00Z"), vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun pastAndHeldSlotsIgnored() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, _ ->
                availabilityOf(
                    slotDto("2026-11-02T12:00:00Z"),
                    slotDto("2026-11-02T18:00:00Z", held = true),
                    slotDto("2026-11-02T20:00:00Z"),
                )
            }
            val vm = loaded()
            vm.selectDate(LocalDate.of(2026, 11, 2))

            vm.selectSlot(Instant.parse("2026-11-02T12:00:00Z"))
            assertNull(vm.uiState.value.selectedSlotStart)
            vm.selectSlot(Instant.parse("2026-11-02T18:00:00Z"))
            assertNull(vm.uiState.value.selectedSlotStart)
            vm.selectSlot(Instant.parse("2026-11-02T20:00:00Z"))
            assertEquals(Instant.parse("2026-11-02T20:00:00Z"), vm.uiState.value.selectedSlotStart)
        }

    @Test
    fun changingDayClearsSlot() =
        runTest(main.testDispatcher) {
            val savedState = SavedStateHandle()
            val vm = loaded(savedState)
            vm.selectDate(LocalDate.of(2026, 11, 5))
            vm.selectSlot(slotNov5)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)
            assertTrue(savedState.contains(ReschedulePickerController.KEY_SLOT_START))

            vm.selectDate(LocalDate.of(2026, 11, 6))

            assertNull(vm.uiState.value.selectedSlotStart)
            assertFalse(savedState.contains(ReschedulePickerController.KEY_SLOT_START))
        }

    @Test
    fun monthRoundTripKeepsDate() =
        runTest(main.testDispatcher) {
            val vm = loaded()
            vm.selectDate(LocalDate.of(2026, 11, 5))
            vm.selectSlot(slotNov5)
            val date = vm.uiState.value.selectedDate

            vm.nextMonth()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.selectedDateLoaded)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)

            vm.nextMonth()
            advanceUntilIdle()
            assertEquals(YearMonth.of(2027, 1), vm.uiState.value.displayedMonth)
            assertFalse(vm.uiState.value.selectedDateLoaded)
            assertEquals(date, vm.uiState.value.selectedDate)
            assertNull(vm.uiState.value.selectedSlotStart)
            assertFalse(vm.uiState.value.canSubmit)

            vm.previousMonth()
            advanceUntilIdle()
            vm.previousMonth()
            advanceUntilIdle()
            assertEquals(YearMonth.of(2026, 11), vm.uiState.value.displayedMonth)
            assertTrue(vm.uiState.value.selectedDateLoaded)
            assertEquals(date, vm.uiState.value.selectedDate)
        }

    @Test
    fun previousMonthBlockedAtCurrentMonth() =
        runTest(main.testDispatcher) {
            val vm = loaded()
            val calls = api.expertAvailabilityCalls

            vm.previousMonth()
            advanceUntilIdle()

            assertEquals(YearMonth.of(2026, 11), vm.uiState.value.displayedMonth)
            assertEquals(calls, api.expertAvailabilityCalls)
        }

    @Test
    fun pastBookingSeedsToday() =
        runTest(main.testDispatcher) {
            stubBooking(bookingDetail(start = Instant.parse("2026-11-01T15:00:00Z")))
            val vm = loaded()

            assertEquals(LocalDate.of(2026, 11, 2), vm.uiState.value.selectedDate)
            assertEquals(YearMonth.of(2026, 11), vm.uiState.value.displayedMonth)
        }

    @Test
    fun retrySlotsReloadsAfterCentreFailure() =
        runTest(main.testDispatcher) {
            api.expertAvailabilityHandler = { _, month ->
                if (month == "2026-11") throw IOException("offline") else availabilityOf()
            }
            val vm = loaded()
            assertEquals(
                AvailabilityState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.availability,
            )

            stubSlots("2026-11-05T16:00:00Z")
            vm.retrySlots()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.availability is AvailabilityState.Loaded)
        }

    @Test
    fun refreshClockClearsStartedSlot() =
        runTest(main.testDispatcher) {
            val vm = loaded()
            vm.selectDate(LocalDate.of(2026, 11, 5))
            vm.selectSlot(slotNov5)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)

            clock.instant = slotNov5.plusSeconds(60)
            vm.refreshClock()

            assertNull(vm.uiState.value.selectedSlotStart)
            assertEquals(clock.instant, vm.uiState.value.now)
        }

    @Test
    fun messageCappedAt500() =
        runTest(main.testDispatcher) {
            val vm = loaded()
            vm.updateMessage("a".repeat(600))

            assertEquals(500, vm.uiState.value.message.length)
        }

    @Test
    fun restoresSelectionAfterProcessDeath() =
        runTest(main.testDispatcher) {
            val savedState =
                SavedStateHandle(
                    mapOf(
                        ReschedulePickerController.KEY_DISPLAYED_MONTH to "2026-11",
                        ReschedulePickerController.KEY_SELECTED_DATE to "2026-11-05",
                        ReschedulePickerController.KEY_SLOT_START to slotNov5.toEpochMilli(),
                        ReschedulePickerController.KEY_MESSAGE to "hi",
                    ),
                )
            val vm = vm(savedState)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)
            advanceUntilIdle()

            val s = vm.uiState.value
            assertEquals(YearMonth.of(2026, 11), s.displayedMonth)
            assertEquals(LocalDate.of(2026, 11, 5), s.selectedDate)
            assertEquals(slotNov5, s.selectedSlotStart)
            assertEquals("hi", s.message)
            val allowed =
                setOf(
                    ReschedulePickerController.KEY_DISPLAYED_MONTH,
                    ReschedulePickerController.KEY_SELECTED_DATE,
                    ReschedulePickerController.KEY_SLOT_START,
                    ReschedulePickerController.KEY_MESSAGE,
                    ReschedulePickerController.KEY_DISCLOSURE_OPEN,
                )
            assertTrue(savedState.keys().all { it in allowed })
        }

    @Test
    fun restoredSlotThatIsNoLongerPickableIsDropped() =
        runTest(main.testDispatcher) {
            val gone = Instant.parse("2026-11-05T18:00:00Z")
            val savedState =
                SavedStateHandle(
                    mapOf(
                        ReschedulePickerController.KEY_DISPLAYED_MONTH to "2026-11",
                        ReschedulePickerController.KEY_SELECTED_DATE to "2026-11-05",
                        ReschedulePickerController.KEY_SLOT_START to gone.toEpochMilli(),
                    ),
                )
            val vm = loaded(savedState)

            assertNull(vm.uiState.value.selectedSlotStart)
            assertFalse(savedState.contains(ReschedulePickerController.KEY_SLOT_START))
        }

    // ---- Task 3: submit guards, disclosure, saved-card outcomes, error actions ----

    private fun serverError(
        code: String,
        status: Int = 422,
    ) = api.httpError(status, """{"code":"$code","message":"Server says no"}""")

    private fun TestScope.withSlotChosen(
        savedState: SavedStateHandle = SavedStateHandle(),
        message: String = "",
    ): RescheduleViewModel {
        val vm = loaded(savedState)
        vm.selectDate(LocalDate.of(2026, 11, 5))
        vm.selectSlot(slotNov5)
        if (message.isNotEmpty()) vm.updateMessage(message)
        return vm
    }

    private fun stubDisclosureBooking(expiresAt: String? = "2026-11-05T16:45:00Z") {
        stubBooking(bookingDetail(start = bookingStart, authorizationExpiresAt = expiresAt?.let(Instant::parse)))
    }

    private fun errorOf(vm: RescheduleViewModel) = vm.uiState.value.dialog as ReschedulePickerDialog.Error

    @Test
    fun submitWithoutSlotIsLocalError() =
        runTest(main.testDispatcher) {
            val vm = loaded()

            vm.submit()

            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.reschedule_start_error_no_slot)),
                vm.uiState.value.dialog,
            )
            assertEquals(0, api.rescheduleClientBookingCalls)
        }

    @Test
    fun submitCurrentSlotIsLocalError() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.expertAvailabilityHandler = { _, _ ->
                gate.await()
                availabilityOf(slotDto("2026-11-06T15:00:00Z"))
            }
            val savedState =
                SavedStateHandle(
                    mapOf(
                        ReschedulePickerController.KEY_DISPLAYED_MONTH to "2026-11",
                        ReschedulePickerController.KEY_SELECTED_DATE to "2026-11-06",
                        ReschedulePickerController.KEY_SLOT_START to bookingStart.toEpochMilli(),
                    ),
                )
            val vm = vm(savedState)
            advanceUntilIdle()
            assertEquals(AvailabilityState.Loading, vm.uiState.value.availability)

            vm.submit()

            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.reschedule_start_error_current_slot)),
                vm.uiState.value.dialog,
            )
            assertEquals(0, api.rescheduleClientBookingCalls)
            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun liveCallBlocks() =
        runTest(main.testDispatcher) {
            val vm = withSlotChosen()
            callGuard.blocked = true

            vm.submit()
            advanceUntilIdle()

            assertEquals(0, api.rescheduleClientBookingCalls)
            assertNull(vm.uiState.value.dialog)
            assertEquals(listOf("b1" to RestrictedAction.RescheduleBooking), callGuard.modifyAttempts)
        }

    @Test
    fun singleFlight() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.rescheduleClientBookingHandler = { _, _ ->
                gate.await()
                RescheduleResponseData(bookingId = "b1", status = "pending")
            }
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isSubmitting)
            vm.submit()
            advanceUntilIdle()
            assertEquals(1, api.rescheduleClientBookingCalls)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, api.rescheduleClientBookingCalls)
            assertEquals(1, callGuard.modifyAttempts.size)
            assertTrue(vm.uiState.value.sent)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun disclosureBeforeAnyRequest() =
        runTest(main.testDispatcher) {
            stubDisclosureBooking()
            val vm = withSlotChosen(message = "hello")

            vm.submit()
            advanceUntilIdle()

            assertEquals(ReschedulePickerDialog.Disclosure(chargesSoon = false), vm.uiState.value.dialog)
            assertEquals(0, api.rescheduleClientBookingCalls)

            vm.dismissDialog()
            advanceUntilIdle()
            assertNull(vm.uiState.value.dialog)
            assertEquals(0, api.rescheduleClientBookingCalls)
            assertEquals(LocalDate.of(2026, 11, 5), vm.uiState.value.selectedDate)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)
            assertEquals("hello", vm.uiState.value.message)

            vm.submit()
            vm.confirmDisclosure()
            advanceUntilIdle()

            assertEquals(1, api.rescheduleClientBookingCalls)
            assertTrue(vm.uiState.value.sent)
        }

    @Test
    fun liveCallBlocksDisclosureContinue() =
        runTest(main.testDispatcher) {
            stubDisclosureBooking()
            val vm = withSlotChosen()
            vm.submit()
            advanceUntilIdle()
            assertEquals(ReschedulePickerDialog.Disclosure(chargesSoon = false), vm.uiState.value.dialog)

            callGuard.blocked = true
            vm.confirmDisclosure()
            advanceUntilIdle()

            assertEquals(0, api.rescheduleClientBookingCalls)
            assertNull(vm.uiState.value.dialog)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)
            assertEquals(
                listOf("b1" to RestrictedAction.RescheduleBooking, "b1" to RestrictedAction.RescheduleBooking),
                callGuard.modifyAttempts,
            )
        }

    @Test
    fun liveCallBlocksRestoredDisclosureContinue() =
        runTest(main.testDispatcher) {
            stubDisclosureBooking()
            val savedState = SavedStateHandle()
            withSlotChosen(savedState).submit()
            val restored = vm(savedState)
            advanceUntilIdle()
            assertEquals(ReschedulePickerDialog.Disclosure(chargesSoon = false), restored.uiState.value.dialog)

            callGuard.blocked = true
            restored.confirmDisclosure()
            advanceUntilIdle()

            assertEquals(0, api.rescheduleClientBookingCalls)
            assertNull(restored.uiState.value.dialog)
            assertNull(savedState.get<Boolean>(ReschedulePickerController.KEY_DISCLOSURE_OPEN))
        }

    @Test
    fun disclosureChargesSoonUnder48h() =
        runTest(main.testDispatcher) {
            clock.instant = Instant.parse("2026-11-04T10:00:00Z")
            stubDisclosureBooking()
            val vm = withSlotChosen()

            vm.submit()

            assertEquals(ReschedulePickerDialog.Disclosure(chargesSoon = true), vm.uiState.value.dialog)
            assertEquals(0, api.rescheduleClientBookingCalls)
        }

    @Test
    fun noDisclosureWithoutExpiry() =
        runTest(main.testDispatcher) {
            stubDisclosureBooking(expiresAt = null)
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertNull(vm.uiState.value.dialog)
            assertEquals(1, api.rescheduleClientBookingCalls)
        }

    @Test
    fun disclosureFlagSurvivesProcessDeath() =
        runTest(main.testDispatcher) {
            stubDisclosureBooking()
            val savedState = SavedStateHandle()
            withSlotChosen(savedState).submit()
            assertEquals(true, savedState.get<Boolean>(ReschedulePickerController.KEY_DISCLOSURE_OPEN))

            val restored = vm(savedState)
            advanceUntilIdle()

            assertEquals(ReschedulePickerDialog.Disclosure(chargesSoon = false), restored.uiState.value.dialog)
            assertEquals(slotNov5, restored.uiState.value.selectedSlotStart)
            assertEquals(0, api.rescheduleClientBookingCalls)
        }

    @Test
    fun needsPaymentMethodDialog() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("PAYMENT_METHOD_REQUIRED") }
            api.getPaymentMethodsHandler = { PaymentMethodsData(emptyList()) }
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertEquals(ReschedulePickerDialog.PaymentMethodNeeded, vm.uiState.value.dialog)
            assertFalse(vm.uiState.value.isSubmitting)
            assertFalse(vm.uiState.value.sent)
        }

    @Test
    fun cardUnusableDialog() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("PAYMENT_METHOD_REQUIRED") }
            api.stubCards(cardDto("pm_a", isDefault = true))
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertEquals(ReschedulePickerDialog.CardUnusable, vm.uiState.value.dialog)
            assertEquals(2, api.rescheduleClientBookingCalls)
        }

    @Test
    fun cardsUnavailableError() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("PAYMENT_METHOD_REQUIRED") }
            api.getPaymentMethodsHandler = { throw IOException("offline") }
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.reschedule_error_cards_unavailable)),
                vm.uiState.value.dialog,
            )
            assertEquals(1, api.rescheduleClientBookingCalls)
        }

    @Test
    fun noAutoRetryAfterPaymentMethods() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("PAYMENT_METHOD_REQUIRED") }
            api.stubCards(cardDto("pm_a", isDefault = true))
            val vm = withSlotChosen(message = "keep me")
            vm.submit()
            advanceUntilIdle()
            assertEquals(2, api.rescheduleClientBookingCalls)

            vm.dismissDialog()
            vm.refreshClock()
            advanceUntilIdle()

            assertEquals(2, api.rescheduleClientBookingCalls)
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)
            assertEquals(LocalDate.of(2026, 11, 5), vm.uiState.value.selectedDate)
            assertEquals("keep me", vm.uiState.value.message)

            vm.submit()
            advanceUntilIdle()
            assertEquals(4, api.rescheduleClientBookingCalls)
        }

    @Test
    fun tooFarAheadClearsSlotKeepsDateAndMessage() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("RESCHEDULE_TOO_FAR_AHEAD") }
            val vm = withSlotChosen(message = "keep me")

            vm.submit()
            advanceUntilIdle()

            assertEquals(ReschedulePickerDialog.Error(FieldError.Text("Server says no")), vm.uiState.value.dialog)
            assertNull(vm.uiState.value.selectedSlotStart)
            assertEquals(LocalDate.of(2026, 11, 5), vm.uiState.value.selectedDate)
            assertEquals("keep me", vm.uiState.value.message)
        }

    @Test
    fun invalidStatusLeaves() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("INVALID_STATUS", status = 409) }
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertTrue(errorOf(vm).leaveOnDismiss)
            assertFalse(vm.uiState.value.leave)
            vm.dismissDialog()
            assertTrue(vm.uiState.value.leave)
            vm.consumeLeave()
            assertFalse(vm.uiState.value.leave)
        }

    @Test
    fun expertUnavailableReloadsMonths() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("EXPERT_UNAVAILABLE", status = 409) }
            val vm = withSlotChosen()
            val before = api.expertAvailabilityCalls

            vm.submit()
            advanceUntilIdle()

            assertNull(vm.uiState.value.selectedSlotStart)
            assertEquals(before + 3, api.expertAvailabilityCalls)
            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.reschedule_error_expert_unavailable)),
                vm.uiState.value.dialog,
            )
        }

    @Test
    fun outsideAuthWindowShowsFixedCopy() =
        runTest(main.testDispatcher) {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError("RESCHEDULE_OUTSIDE_AUTH_WINDOW") }
            val vm = withSlotChosen()

            vm.submit()
            advanceUntilIdle()

            assertEquals(
                ReschedulePickerDialog.Error(FieldError.Res(R.string.reschedule_start_error_outside_window_client)),
                vm.uiState.value.dialog,
            )
            assertEquals(slotNov5, vm.uiState.value.selectedSlotStart)
        }
}
