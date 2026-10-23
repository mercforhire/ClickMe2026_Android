package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.util.Money
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ClientCancellationViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val launchRequest = BookingsLaunchRequest()
    private val clock = MutableClock(Instant.parse("2026-10-23T12:00:00Z"))

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) =
        ClientCancellationViewModel(
            CancelBookingKey("b1"),
            handle,
            ClientBookingsRepository(api),
            clock,
            launchRequest,
        )

    private fun stubDetail(detail: ClientBookingDetail = bookingDetail()) {
        api.clientBookingDetailHandler = { detail }
    }

    private fun apiError(
        status: Int,
        code: String,
        message: String = "msg",
    ) = api.httpError(status, """{"status":"error","code":"$code","message":"$message"}""")

    private val start = Instant.parse("2026-10-24T18:00:00Z")

    @Test
    fun loadsTheBookingByIdAndStartsOnTheReasonStep() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ClientBookingDetail>()
            val ids = mutableListOf<String>()
            api.clientBookingDetailHandler = {
                ids += it
                gate.await()
            }
            val vm = vm()
            runCurrent()
            assertEquals(CancellationLoad.Loading, vm.uiState.value.load)
            assertEquals(listOf("b1"), ids)
            gate.complete(bookingDetail(start = start))
            runCurrent()
            val state = vm.uiState.value
            assertEquals(
                CancellationLoad.Loaded(
                    CancellationSummary("Ada Lovelace", "Engineer", null, start, start.plusSeconds(3600)),
                ),
                state.load,
            )
            assertEquals(CancelStep.Reason, state.step)
            assertEquals(CancellationReason.ChangeOfPlans, state.reason)
            assertEquals("", state.comment)
            assertEquals(1, api.clientBookingDetailCalls)
        }

    @Test
    fun aNetworkFailureEndsFailedAndRetryLoadsAgain() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.clientBookingDetailHandler = {
                if (++calls == 1) throw IOException("offline") else bookingDetail()
            }
            val vm = vm()
            runCurrent()
            assertEquals(CancellationLoad.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.load)
            vm.retry()
            assertEquals(CancellationLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)
            assertTrue(vm.uiState.value.load is CancellationLoad.Loaded)
        }

    @Test
    fun selectingAReasonUpdatesStateAndSavedState() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle = SavedStateHandle()
            val vm = vm(handle)
            runCurrent()
            vm.selectReason(CancellationReason.ScheduleConflict)
            assertEquals(CancellationReason.ScheduleConflict, vm.uiState.value.reason)
            assertEquals("ScheduleConflict", handle.get<String>("cancel_reason"))
        }

    @Test
    fun theCommentIsCappedAt450AndSaved() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle = SavedStateHandle()
            val vm = vm(handle)
            runCurrent()
            vm.updateComment("a".repeat(451))
            assertEquals("a".repeat(450), vm.uiState.value.comment)
            assertEquals("a".repeat(450), handle.get<String>("cancel_comment"))
        }

    @Test
    fun continueMovesToConfirmAndComputesTheRefundPreview() =
        runTest(main.testDispatcher) {
            stubDetail(bookingDetail(paymentStatus = "held"))
            val handle = SavedStateHandle()
            val vm = vm(handle)
            runCurrent()
            assertEquals(RefundPreview.None, vm.uiState.value.refund)
            vm.continueToConfirm()
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            assertEquals("Confirm", handle.get<String>("cancel_step"))
            assertEquals(RefundPreview.HoldReleased, vm.uiState.value.refund)
        }

    @Test
    fun aFreeBookingHasNoRefundLine() =
        runTest(main.testDispatcher) {
            stubDetail(bookingDetail(isFree = true, paymentStatus = null))
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            assertEquals(RefundPreview.None, vm.uiState.value.refund)
        }

    @Test
    fun theRefundPreviewUsesTheTimeStepTwoWasReached() =
        runTest(main.testDispatcher) {
            stubDetail(bookingDetail(paymentStatus = "captured", start = start, amount = 5000))
            val vm = vm()
            runCurrent()
            clock.instant = start.minusSeconds(48 * 3600)
            vm.continueToConfirm()
            assertEquals(RefundPreview.Full(5000, "CAD"), vm.uiState.value.refund)
            vm.backToReason()
            clock.instant = start.minusSeconds(3600)
            vm.continueToConfirm()
            assertEquals(RefundPreview.NoRefundWithin24h, vm.uiState.value.refund)
        }

    @Test
    fun backReturnsToTheReasonStep() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle = SavedStateHandle()
            val vm = vm(handle)
            runCurrent()
            vm.continueToConfirm()
            vm.backToReason()
            assertEquals(CancelStep.Reason, vm.uiState.value.step)
            assertEquals("Reason", handle.get<String>("cancel_step"))
        }

    @Test
    fun savedStepReasonAndCommentAreRestoredWithTheRefundOnceLoaded() =
        runTest(main.testDispatcher) {
            stubDetail(bookingDetail(paymentStatus = "held"))
            val handle =
                SavedStateHandle(
                    mapOf("cancel_step" to "Confirm", "cancel_reason" to "Other", "cancel_comment" to "Moved"),
                )
            val vm = vm(handle)
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            assertEquals(CancellationReason.Other, vm.uiState.value.reason)
            assertEquals("Moved", vm.uiState.value.comment)
            runCurrent()
            assertEquals(RefundPreview.HoldReleased, vm.uiState.value.refund)
        }

    @Test
    fun unknownSavedValuesFallBackToTheDefaults() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle =
                SavedStateHandle(mapOf("cancel_step" to "Nope", "cancel_reason" to "Nope"))
            val vm = vm(handle)
            runCurrent()
            assertEquals(CancelStep.Reason, vm.uiState.value.step)
            assertEquals(CancellationReason.ChangeOfPlans, vm.uiState.value.reason)
        }

    @Test
    fun aMissingEndTimeFallsBackToStartPlusTheTopicDuration() {
        val summary = bookingDetail(start = start, end = null).toCancellationSummary()
        assertEquals(start.plusSeconds(3600), summary.end)
    }

    @Test
    fun blankExpertNameAndTitleBecomeNull() {
        val summary = bookingDetail(expertName = " ", expertTitle = "").toCancellationSummary()
        assertNull(summary.expertName)
        assertNull(summary.expertTitle)
    }

    // --- Task 2: confirm, hand-off and errors ---

    @Test
    fun confirmSendsTheLabelAndTrimmedCommentOnceAndIsSingleFlight() =
        runTest(main.testDispatcher) {
            stubDetail()
            val gate = CompletableDeferred<CancelBookingData>()
            api.cancelClientBookingHandler = { _, _ -> gate.await() }
            val vm = vm()
            runCurrent()
            vm.selectReason(CancellationReason.ScheduleConflict)
            vm.updateComment("  Moved my flight ")
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            vm.confirm()
            runCurrent()
            assertTrue(vm.uiState.value.isSubmitting)
            vm.backToReason()
            vm.selectReason(CancellationReason.Other)
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            assertEquals(CancellationReason.ScheduleConflict, vm.uiState.value.reason)
            assertEquals(
                listOf("b1" to CancelBookingRequest("Schedule conflict: Moved my flight")),
                api.cancelClientBookingRequests,
            )
            gate.complete(CancelBookingData(0.0, "CAD"))
            runCurrent()
        }

    @Test
    fun withoutACommentTheReasonIsTheLabel() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> CancelBookingData(0.0, "CAD") }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(
                listOf("b1" to CancelBookingRequest("Change of plans")),
                api.cancelClientBookingRequests,
            )
        }

    @Test
    fun aRefundPostsTheFormattedAmountToThePastTab() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> CancelBookingData(12.5, "CAD") }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(
                BookingsLaunch(BookingsTab.Past, BookingsNotice.CancelledWithRefund(Money.formatMajor(12.5, "CAD"))),
                launchRequest.pending.value,
            )
            assertTrue(vm.uiState.value.isFinished)
            assertTrue(vm.uiState.value.isSubmitting)
        }

    @Test
    fun aZeroRefundIncludingTheIdempotentOkPostsPlainCancelled() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> CancelBookingData(0.0, "CAD") }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled), launchRequest.pending.value)
            assertTrue(vm.uiState.value.isFinished)
        }

    @Test
    fun aRefundWithoutACurrencyPostsPlainCancelled() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> CancelBookingData(5.0, null) }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled), launchRequest.pending.value)
        }

    @Test
    fun consumeFinishedClearsTheFlag() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> CancelBookingData(0.0, null) }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            vm.consumeFinished()
            assertFalse(vm.uiState.value.isFinished)
        }

    @Test
    fun invalidStatusShowsTheDialogThenReturnsToUpcoming() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> throw apiError(409, "INVALID_STATUS") }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(
                CancelErrorDialog(FieldError.Res(R.string.cancel_booking_error_not_cancellable), true),
                vm.uiState.value.error,
            )
            assertFalse(vm.uiState.value.isSubmitting)
            assertNull(launchRequest.pending.value)
            vm.dismissError()
            assertEquals(BookingsLaunch(BookingsTab.Upcoming, null), launchRequest.pending.value)
            assertTrue(vm.uiState.value.isFinished)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun notFoundBehavesTheSameWithItsOwnCopy() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> throw apiError(404, "NOT_FOUND") }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(
                CancelErrorDialog(FieldError.Res(R.string.cancel_booking_error_not_found), true),
                vm.uiState.value.error,
            )
            vm.dismissError()
            assertEquals(BookingsLaunch(BookingsTab.Upcoming, null), launchRequest.pending.value)
            assertTrue(vm.uiState.value.isFinished)
        }

    @Test
    fun otherErrorsShowTheServerMessageAndAllowARetry() =
        runTest(main.testDispatcher) {
            stubDetail()
            var calls = 0
            api.cancelClientBookingHandler = { _, _ ->
                if (++calls == 1) throw apiError(500, "SERVER_ERROR", "Boom") else CancelBookingData(0.0, null)
            }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertEquals(CancelErrorDialog(FieldError.Text("Boom"), false), vm.uiState.value.error)
            vm.dismissError()
            assertNull(vm.uiState.value.error)
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            assertNull(launchRequest.pending.value)
            assertFalse(vm.uiState.value.isFinished)
            vm.confirm()
            runCurrent()
            assertEquals(2, api.cancelClientBookingCalls)
            assertTrue(vm.uiState.value.isFinished)
        }

    @Test
    fun aSuspendedAccountLeavesNoDialog() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelClientBookingHandler = { _, _ -> throw apiError(403, "ACCOUNT_SUSPENDED") }
            val vm = vm()
            runCurrent()
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()
            assertNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isSubmitting)
            assertFalse(vm.uiState.value.isFinished)
        }

    @Test
    fun confirmIsIgnoredUntilTheBookingHasLoaded() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()
            vm.confirm()
            runCurrent()
            assertEquals(0, api.cancelClientBookingCalls)
        }
}
