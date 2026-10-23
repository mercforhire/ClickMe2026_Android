@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeCompletedBookingStore
import com.uptrendinvestments.clickmeapp.testing.FakeStripePaymentMethodReader
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.util.TimeZone
import java.util.UUID

class MakeABookingViewModelPaidTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse(NOW))
    private val reader = FakeStripePaymentMethodReader().apply { result = "pm_new" }
    private val store = FakeCompletedBookingStore()
    private var monotonicNow = 0L
    private val monotonic = MonotonicClock { monotonicNow }
    private lateinit var previousZone: TimeZone

    private val slotA = Instant.parse("2026-10-22T20:00:00Z")
    private val slotB = Instant.parse("2026-10-23T01:00:00Z")

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        paidFixture(api)
        api.requestBookingHandler = { BookingRequestData(bookingId = "b1") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun http(
        status: Int,
        code: ErrorCode?,
        message: String? = null,
        detail: Map<String, Any?>? = null,
    ) = ApiException.Http(
        status = status,
        code = code,
        rawCode = code?.wire ?: "SOMETHING_ELSE",
        serverMessage = message,
        fieldErrors = emptyList(),
        detail = detail,
    )

    /** A loaded view model with the paid topic t2 and [slot] selected and the cards known. */
    private fun TestScope.paidVm(
        slot: Instant = slotB,
        saved: SavedStateHandle = SavedStateHandle(),
    ): MakeABookingViewModel {
        val vm = newViewModel(api, saved, clock, store, reader, monotonic)
        runCurrent()
        vm.selectTopic("t2")
        vm.selectSlot(slot)
        return vm
    }

    private fun TestScope.bookAndRun(vm: MakeABookingViewModel) {
        vm.book()
        runCurrent()
    }

    /** Books, completes the sheet and runs the confirm. */
    private fun TestScope.bookThroughTheSheet(vm: MakeABookingViewModel) {
        bookAndRun(vm)
        vm.onStripeRequestLaunched()
        vm.onSheetOutcome(SheetOutcome.Completed)
        runCurrent()
    }

    private fun res(id: Int) = FieldError.Res(id)

    private fun dialog(vm: MakeABookingViewModel) = vm.uiState.value.dialogError

    // ---- Task 1: short lead ----

    @Test
    fun shortLeadSendsPaymentIntentThenSetupIntentThenRequestsTheSheet() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            bookAndRun(vm)
            val request = api.paymentIntentRequests.single()
            assertEquals(PaymentIntentRequest("e1", "t2", "2026-10-23", "01:00", 60, request.bookingAttemptId), request)
            UUID.fromString(request.bookingAttemptId)
            assertEquals(1, api.setupIntentCalls)
            val s = vm.uiState.value
            assertEquals(PaidStep.CollectingPaymentMethod, s.paidStep)
            assertTrue(s.isSubmitting)
            assertEquals(StripeRequest.PresentSheet("seti_1_secret_x", "cus_1", "ek_1"), s.stripeRequest)
            assertEquals(0, api.requestBookingCalls)
            assertEquals(0, api.confirmBookingCalls)
        }

    @Test
    fun launchingTheRequestConsumesItAndKeepsTheStep() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            bookAndRun(vm)
            vm.onStripeRequestLaunched()
            assertNull(vm.uiState.value.stripeRequest)
            assertEquals(PaidStep.CollectingPaymentMethod, vm.uiState.value.paidStep)
        }

    @Test
    fun aCompletedSheetConfirmsWithThePmAndThePiOnce() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            bookThroughTheSheet(vm)
            assertEquals(listOf("seti_1_secret_x"), reader.requestedSecrets)
            assertEquals(
                ConfirmBookingRequest("pi_1", "e1", "t2", "2026-10-23T01:00:00Z", "in_app_voice", "pm_new", null),
                api.confirmBookingRequests.single(),
            )
            val s = vm.uiState.value
            assertEquals("b9", s.completedBookingId)
            assertTrue(s.isSubmitting)
            assertEquals(PaidStep.Idle, s.paidStep)
        }

    @Test
    fun notesAreTrimmedAndTheMeetingTypeUsesItsWireValue() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            vm.onNotesChange("  Deep dive  ")
            vm.selectMeetingType(MeetingType.SkypeZoom)
            bookThroughTheSheet(vm)
            val body = api.confirmBookingRequests.single()
            assertEquals("Deep dive", body.clientNotes)
            assertEquals("skype_zoom", body.meetingType)
        }

    @Test
    fun anUnreadablePaymentMethodShowsTheDialogAndKeepsTheSlot() =
        runTest(main.testDispatcher) {
            reader.result = null
            val vm = paidVm()
            bookThroughTheSheet(vm)
            val s = vm.uiState.value
            assertEquals(res(R.string.make_a_booking_error_pm_unreadable), s.dialogError)
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
            assertEquals(slotB, s.selectedSlotStart)
            assertEquals(0, api.confirmBookingCalls)
        }

    @Test
    fun aCancelledSheetResetsSilentlyAndReusesTheAttemptIdAndTheSetupIntent() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            bookAndRun(vm)
            vm.onSheetOutcome(SheetOutcome.Canceled)
            val s = vm.uiState.value
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
            assertNull(s.dialogError)
            assertEquals(slotB, s.selectedSlotStart)
            assertEquals(0, api.confirmBookingCalls)

            bookAndRun(vm)
            assertEquals(2, api.paymentIntentCalls)
            assertEquals(
                api.paymentIntentRequests[0].bookingAttemptId,
                api.paymentIntentRequests[1].bookingAttemptId,
            )
            assertEquals(1, api.setupIntentCalls)
            assertTrue(vm.uiState.value.stripeRequest is StripeRequest.PresentSheet)
        }

    @Test
    fun aCancelledSetupIntentIsMintedAgainAfterTheReuseWindow() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            bookAndRun(vm)
            vm.onSheetOutcome(SheetOutcome.Canceled)
            monotonicNow += 11 * 60_000L
            bookAndRun(vm)
            assertEquals(2, api.setupIntentCalls)
        }

    @Test
    fun aFailedSheetShowsStripesTextOrTheFallbackAndResets() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            bookAndRun(vm)
            vm.onSheetOutcome(SheetOutcome.Failed("Card setup failed"))
            assertEquals(FieldError.Text("Card setup failed"), dialog(vm))
            assertEquals(PaidStep.Idle, vm.uiState.value.paidStep)
            assertFalse(vm.uiState.value.isSubmitting)

            vm.dismissError()
            bookAndRun(vm)
            vm.onSheetOutcome(SheetOutcome.Failed(null))
            assertEquals(res(R.string.make_a_booking_error_sheet_failed), dialog(vm))
            assertEquals(PaidStep.Idle, vm.uiState.value.paidStep)
            // A failed sheet drops the cached SetupIntent, so each book mints a new one.
            assertEquals(2, api.setupIntentCalls)
            vm.dismissError()
            bookAndRun(vm)
            assertEquals(3, api.setupIntentCalls)
        }

    @Test
    fun aSheetOutcomeWithoutAnActiveSheetIsIgnored() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            vm.onSheetOutcome(SheetOutcome.Completed)
            vm.onSheetOutcome(SheetOutcome.Failed("x"))
            runCurrent()
            assertNull(dialog(vm))
            assertEquals(0, api.confirmBookingCalls)
            assertEquals(PaidStep.Idle, vm.uiState.value.paidStep)
        }

    @Test
    fun aSecondTapWhileThePaidFlowIsInFlightIsIgnored() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<PaymentIntentResponse>()
            api.paymentIntentHandler = { gate.await() }
            val vm = paidVm()
            bookAndRun(vm)
            assertTrue(vm.uiState.value.isSubmitting)
            assertEquals(PaidStep.CreatingIntent, vm.uiState.value.paidStep)
            vm.book()
            runCurrent()
            assertEquals(1, api.paymentIntentCalls)
            // The sheet is part of the flow too.
            gate.complete(PaymentIntentResponse("pi_1", "pi_1_secret_x", "cad"))
            runCurrent()
            assertTrue(vm.uiState.value.isSubmitting)
            vm.book()
            runCurrent()
            assertEquals(1, api.paymentIntentCalls)
        }

    // ---- Task 1: attempt id (D-11, D-17) ----

    @Test
    fun theAttemptIdChangesWithTheSlotAndTheTopicButNotWithANetworkFailure() =
        runTest(main.testDispatcher) {
            val vm = paidVm(slotB)
            bookAndRun(vm)
            vm.onSheetOutcome(SheetOutcome.Canceled)

            vm.selectSlot(slotA)
            bookAndRun(vm)
            vm.onSheetOutcome(SheetOutcome.Canceled)

            vm.selectTopic("t1")
            vm.selectTopic("t2")
            vm.selectSlot(slotA)
            api.paymentIntentHandler = { throw IOException("offline") }
            bookAndRun(vm)
            assertEquals(res(R.string.login_error_network), dialog(vm))
            vm.dismissError()
            api.paymentIntentHandler = { PaymentIntentResponse("pi_1", "pi_1_secret_x", "cad") }
            bookAndRun(vm)

            val ids = api.paymentIntentRequests.map { it.bookingAttemptId }
            assertEquals(4, ids.size)
            assertNotEquals(ids[0], ids[1])
            assertNotEquals(ids[1], ids[2])
            assertEquals(ids[2], ids[3])
        }

    @Test
    fun noSecretOrAttemptIdReachesTheSavedState() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle()
            val gate = CompletableDeferred<ConfirmBookingData>()
            api.confirmBookingHandler = { gate.await() }
            val vm = paidVm(saved = saved)
            bookAndRun(vm)
            val attemptId = api.paymentIntentRequests.single().bookingAttemptId
            val forbidden = listOf(attemptId, "pi_1", "pi_1_secret_x", "seti_1_secret_x", "ek_1", "pm_new")

            fun assertClean() {
                saved.keys().forEach { key ->
                    val value = saved.get<Any?>(key).toString()
                    forbidden.forEach { secret -> assertFalse("$key holds $secret", value.contains(secret)) }
                }
            }
            assertClean()
            vm.onSheetOutcome(SheetOutcome.Completed)
            runCurrent()
            assertClean()
            gate.complete(ConfirmBookingData("b9"))
            runCurrent()
            assertClean()
            assertEquals("b9", vm.uiState.value.completedBookingId)
        }

    // ---- Task 1: long lead (PAY-04) ----

    @Test
    fun aLongLeadBookingConfirmsWithTheDefaultCardAndNoPaymentIntent() =
        runTest(main.testDispatcher) {
            api.paymentIntentHandler = { throw http(422, ErrorCode.LongLeadRequiresSavedCard) }
            val vm = paidVm()
            bookAndRun(vm)
            assertEquals(0, api.setupIntentCalls)
            assertNull(vm.uiState.value.stripeRequest)
            assertEquals(
                ConfirmBookingRequest(null, "e1", "t2", "2026-10-23T01:00:00Z", "in_app_voice", "pm_default", null),
                api.confirmBookingRequests.single(),
            )
            assertEquals("b9", vm.uiState.value.completedBookingId)
        }

    @Test
    fun aLongLeadBookingWithoutADefaultCardUsesTheFirst() =
        runTest(main.testDispatcher) {
            api.stubCards(cardDto("pm_a"), cardDto("pm_b"))
            api.paymentIntentHandler = { throw http(422, ErrorCode.LongLeadRequiresSavedCard) }
            val vm = paidVm()
            bookAndRun(vm)
            assertEquals("pm_a", api.confirmBookingRequests.single().stripePaymentMethodId)
        }

    // ---- Task 1: gates ----

    @Test
    fun aPaidTopicWithoutKnownCardsNeverReachesTheApi() =
        runTest(main.testDispatcher) {
            api.stubCards()
            val none = paidVm()
            bookAndRun(none)
            assertEquals(PaymentMethodsAvailability.None, none.uiState.value.paymentMethods)
            assertEquals(0, api.paymentIntentCalls)
            assertEquals(0, api.requestBookingCalls)
            assertFalse(none.uiState.value.isSubmitting)

            val unknown = CompletableDeferred<PaymentMethodsData>()
            api.getPaymentMethodsHandler = { unknown.await() }
            val pending = paidVm()
            bookAndRun(pending)
            assertEquals(PaymentMethodsAvailability.Unknown, pending.uiState.value.paymentMethods)
            assertEquals(0, api.paymentIntentCalls)
            assertEquals(0, api.requestBookingCalls)
        }

    @Test
    fun aFreeTopicStillUsesTheFreeRequestOnly() =
        runTest(main.testDispatcher) {
            val vm = newViewModel(api, SavedStateHandle(), clock, store, reader, monotonic)
            runCurrent()
            vm.selectSlot(slotA)
            bookAndRun(vm)
            assertEquals(1, api.requestBookingCalls)
            assertEquals(0, api.paymentIntentCalls)
        }

    // ---- Task 2: failures ----

    @Test
    fun anAttemptConflictAsksToTryAgainWithANewAttemptId() =
        runTest(main.testDispatcher) {
            api.paymentIntentHandler = { throw http(409, ErrorCode.BookingAttemptConflict) }
            val vm = paidVm()
            bookAndRun(vm)
            assertEquals(res(R.string.make_a_booking_error_try_again), dialog(vm))
            assertEquals(PaidStep.Idle, vm.uiState.value.paidStep)
            assertFalse(vm.uiState.value.isSubmitting)
            vm.dismissError()
            bookAndRun(vm)
            val ids = api.paymentIntentRequests.map { it.bookingAttemptId }
            assertNotEquals(ids[0], ids[1])
        }

    @Test
    fun aTakenSlotClearsTheSlotAndReloadsAvailabilityWithoutBlanking() =
        runTest(main.testDispatcher) {
            api.paymentIntentHandler = { throw http(409, ErrorCode.SlotUnavailable) }
            val vm = paidVm()
            val before = api.expertAvailabilityCalls
            bookAndRun(vm)
            assertEquals(res(R.string.make_a_booking_error_slot_taken), dialog(vm))
            assertNull(vm.uiState.value.selectedSlotStart)
            assertEquals(before + 3, api.expertAvailabilityCalls)
            assertTrue(vm.uiState.value.availability is AvailabilityState.Loaded)
            vm.dismissError()
            vm.selectSlot(slotB)
            bookAndRun(vm)
            val ids = api.paymentIntentRequests.map { it.bookingAttemptId }
            assertNotEquals(ids[0], ids[1])
        }

    @Test
    fun aMissingMeetingLinkResetsTheMeetingTypeAndKeepsTheSlot() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw http(422, ErrorCode.MeetingLinkUnavailable) }
            val vm = paidVm()
            vm.selectMeetingType(MeetingType.SkypeZoom)
            bookThroughTheSheet(vm)
            val s = vm.uiState.value
            assertEquals(res(R.string.make_a_booking_error_no_link), s.dialogError)
            assertEquals(MeetingType.InAppVoice, s.meetingType)
            assertEquals(slotB, s.selectedSlotStart)
        }

    @Test
    fun aDeclinedCardShowsNoPaymentReferenceAndKeepsTheAttemptId() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw http(402, ErrorCode.PaymentFailed) }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            assertEquals(res(R.string.make_a_booking_error_card_declined), dialog(vm))
            assertNull(vm.uiState.value.dialogPaymentReference)
            vm.dismissError()
            bookAndRun(vm)
            val ids = api.paymentIntentRequests.map { it.bookingAttemptId }
            assertEquals(ids[0], ids[1])
        }

    @Test
    fun anUnmappedConfirmFailureShowsTheServerTextWithThePaymentReference() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw http(500, null, "Boom") }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            assertEquals(FieldError.Text("Boom"), dialog(vm))
            assertEquals("pi_1", vm.uiState.value.dialogPaymentReference)
        }

    @Test
    fun aConfirmTimeoutKeepsTheAttemptIdShowsTheReferenceAndNeverRetries() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw IOException("timeout") }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            val s = vm.uiState.value
            assertEquals(res(R.string.login_error_network), s.dialogError)
            assertEquals("pi_1", s.dialogPaymentReference)
            assertEquals(1, api.confirmBookingCalls)
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
            assertEquals(slotB, s.selectedSlotStart)
            vm.dismissError()
            assertNull(vm.uiState.value.dialogPaymentReference)
            bookAndRun(vm)
            val ids = api.paymentIntentRequests.map { it.bookingAttemptId }
            assertEquals(ids[0], ids[1])
            assertEquals(1, api.confirmBookingCalls)
        }

    @Test
    fun aSlotConflictAfterAnUnknownOutcomeOnTheSameAttemptPointsToMyBookingsOnce() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw IOException("timeout") }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            vm.dismissError()

            api.paymentIntentHandler = { throw http(409, ErrorCode.SlotUnavailable) }
            bookAndRun(vm)
            assertEquals(res(R.string.make_a_booking_error_slot_taken_check_bookings), dialog(vm))

            vm.dismissError()
            vm.selectSlot(slotA)
            bookAndRun(vm)
            assertEquals(res(R.string.make_a_booking_error_slot_taken), dialog(vm))
        }

    @Test
    fun aSlotConflictAfterAConfirmServerErrorOnTheSameAttemptPointsToMyBookings() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw http(502, null) }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            assertEquals("pi_1", vm.uiState.value.dialogPaymentReference)
            vm.dismissError()

            api.paymentIntentHandler = { throw http(409, ErrorCode.SlotUnavailable) }
            bookAndRun(vm)
            assertEquals(res(R.string.make_a_booking_error_slot_taken_check_bookings), dialog(vm))
            val ids = api.paymentIntentRequests.map { it.bookingAttemptId }
            assertEquals(ids[0], ids[1])
        }

    @Test
    fun aLongLeadConfirmTimeoutHasNoPaymentReference() =
        runTest(main.testDispatcher) {
            api.paymentIntentHandler = { throw http(422, ErrorCode.LongLeadRequiresSavedCard) }
            api.confirmBookingHandler = { throw IOException("timeout") }
            val vm = paidVm()
            bookAndRun(vm)
            assertEquals(res(R.string.login_error_network), dialog(vm))
            assertNull(vm.uiState.value.dialogPaymentReference)
        }

    @Test
    fun aSuspendedAccountShowsNoDialogHere() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw http(403, ErrorCode.AccountSuspended) }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            val s = vm.uiState.value
            assertNull(s.dialogError)
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
        }

    @Test
    fun aThreeDsRequirementPublishesTheChallengeAndKeepsTheFlowOpen() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = {
                throw http(
                    402,
                    ErrorCode.PaymentRequiresAction,
                    detail = mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to "acct_1"),
                )
            }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            val s = vm.uiState.value
            assertEquals(PaidStep.Confirming, s.paidStep)
            assertTrue(s.isSubmitting)
            assertEquals(StripeRequest.RunChallenge("pi_1_secret_x", "acct_1"), s.stripeRequest)
            assertNull(s.dialogError)
        }

    // ---- Task 2: recovery record and dialog ----

    @Test
    fun theRecoveryIdIsWrittenBeforeConfirmAndRecordedOnSuccess() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle()
            val gate = CompletableDeferred<ConfirmBookingData>()
            api.confirmBookingHandler = { gate.await() }
            val vm = paidVm(saved = saved)
            bookThroughTheSheet(vm)
            val recoveryId = saved.get<String>("booking_attempt_id")
            UUID.fromString(recoveryId)
            assertNotEquals(api.paymentIntentRequests.single().bookingAttemptId, recoveryId)
            gate.complete(ConfirmBookingData("b9"))
            runCurrent()
            assertEquals(mapOf(recoveryId to "b9"), store.records)
            assertEquals("b9", saved.get<String>("booking_completed_id"))
        }

    @Test
    fun theRecoveryIdIsRemovedAfterAFailure() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle()
            api.confirmBookingHandler = { throw http(402, ErrorCode.PaymentFailed) }
            val vm = paidVm(saved = saved)
            bookThroughTheSheet(vm)
            assertFalse(saved.contains("booking_attempt_id"))
        }

    @Test
    fun theRecoveryIdIsRemovedAfterAThreeDsRequirement() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle()
            api.confirmBookingHandler = {
                throw http(
                    402,
                    ErrorCode.PaymentRequiresAction,
                    detail = mapOf("client_secret" to "pi_1_secret_x", "stripe_account" to "acct_1"),
                )
            }
            val vm = paidVm(saved = saved)
            bookThroughTheSheet(vm)
            assertFalse(saved.contains("booking_attempt_id"))
        }

    @Test
    fun dismissingTheDialogClearsTheMessageAndTheReference() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw http(500, null, "Boom") }
            val vm = paidVm()
            bookThroughTheSheet(vm)
            vm.dismissError()
            assertNull(vm.uiState.value.dialogError)
            assertNull(vm.uiState.value.dialogPaymentReference)
        }
}
