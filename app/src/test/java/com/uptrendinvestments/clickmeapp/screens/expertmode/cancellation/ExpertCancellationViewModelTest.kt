package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelErrorDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
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
class ExpertCancellationViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()
    private val launchRequest = ExpertLaunchRequest()
    private val clock = MutableClock(Instant.parse("2026-10-23T12:00:00Z"))

    private fun vm(handle: SavedStateHandle = SavedStateHandle()) =
        ExpertCancellationViewModel(
            ExpertCancelKey("b1", ExpertBookingOrigin.Bookings),
            handle,
            ExpertBookingsRepository(api),
            clock,
            launchRequest,
            guard,
        )

    private fun stubDetail(fee: Double? = 75.0) {
        api.expertBookingDetailsHandler = { expertBookingDetail(fee = fee) }
    }

    private fun apiError(
        status: Int,
        code: String,
        message: String = "msg",
    ) = api.httpError(status, """{"status":"error","code":"$code","message":"$message"}""")

    private fun pending() = launchRequest.bookings.pending.value

    @Test
    fun loadsTheBookingByIdAndStartsOnTheReasonStepWithNothingSelected() =
        runTest(main.testDispatcher) {
            val ids = mutableListOf<String>()
            api.expertBookingDetailsHandler = {
                ids += it
                expertBookingDetail()
            }
            val vm = vm()
            runCurrent()

            val state = vm.uiState.value
            assertEquals(listOf("b1"), ids)
            assertTrue(state.load is ExpertCancelLoad.Loaded)
            assertEquals(CancelStep.Reason, state.step)
            assertNull(state.reason)
            assertEquals("", state.comment)
        }

    @Test
    fun aNetworkFailureEndsFailedAndRetryLoadsAgain() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.expertBookingDetailsHandler = {
                if (++calls == 1) throw IOException("offline") else expertBookingDetail()
            }
            val vm = vm()
            runCurrent()
            assertEquals(ExpertCancelLoad.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.load)

            vm.retry()
            runCurrent()

            assertEquals(2, calls)
            assertTrue(vm.uiState.value.load is ExpertCancelLoad.Loaded)
        }

    @Test
    fun continueNeedsAReasonAndTheCommentIsCappedAt450() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle = SavedStateHandle()
            val vm = vm(handle)
            runCurrent()

            vm.continueToConfirm()
            assertEquals(CancelStep.Reason, vm.uiState.value.step)

            vm.selectReason(ExpertCancelReason.TechnicalIssues)
            vm.updateComment("x".repeat(500))
            assertEquals(450, vm.uiState.value.comment.length)
            assertEquals("TechnicalIssues", handle.get<String>(ExpertCancellationViewModel.KEY_REASON))
            vm.continueToConfirm()
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            assertEquals(ExpertRefundPreview.HoldReleased, vm.uiState.value.refund)

            vm.backToReason()
            assertEquals(CancelStep.Reason, vm.uiState.value.step)
        }

    @Test
    fun aFreeBookingHasNoRefundLine() =
        runTest(main.testDispatcher) {
            stubDetail(fee = null)
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)

            vm.continueToConfirm()

            assertEquals(ExpertRefundPreview.None, vm.uiState.value.refund)
        }

    @Test
    fun confirmSendsTheMergedReasonAndPostsThePastTabHandOff() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelExpertBookingHandler = { _, _ -> CancelBookingData(0.0, "USD") }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.SchedulingConflict)
            vm.updateComment("  Double booked ")
            vm.continueToConfirm()

            vm.confirm()
            runCurrent()

            assertEquals(
                listOf("b1" to CancelBookingRequest("Scheduling Conflict: Double booked")),
                api.cancelExpertBookingRequests,
            )
            assertEquals(BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled), pending())
            assertTrue(vm.uiState.value.isFinished)
            vm.consumeFinished()
            assertFalse(vm.uiState.value.isFinished)
        }

    @Test
    fun aServerReportedRefundBecomesTheNotice() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelExpertBookingHandler = { _, _ -> CancelBookingData(75.0, "USD") }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()

            vm.confirm()
            runCurrent()

            assertEquals(
                BookingsLaunch(BookingsTab.Past, BookingsNotice.CancelledWithRefund(Money.formatMajor(75.0, "USD"))),
                pending(),
            )
        }

    @Test
    fun alreadyCancelledIsSuccess() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelExpertBookingHandler = { _, _ -> throw apiError(409, "ALREADY_CANCELLED") }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()

            vm.confirm()
            runCurrent()

            assertEquals(BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled), pending())
            assertTrue(vm.uiState.value.isFinished)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun invalidStatusReturnsToBookings() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelExpertBookingHandler = { _, _ -> throw apiError(409, "INVALID_STATUS") }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()

            assertEquals(
                CancelErrorDialog(FieldError.Res(R.string.cancel_booking_error_not_cancellable), true),
                vm.uiState.value.error,
            )
            assertFalse(vm.uiState.value.isSubmitting)
            assertNull(pending())

            vm.dismissError()

            assertEquals(BookingsLaunch(BookingsTab.Upcoming), pending())
            assertTrue(vm.uiState.value.isFinished)
            assertNull(vm.uiState.value.error)
        }

    @Test
    fun notFoundBehavesTheSameWithItsOwnCopy() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelExpertBookingHandler = { _, _ -> throw apiError(404, "NOT_FOUND") }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()

            assertEquals(
                CancelErrorDialog(FieldError.Res(R.string.cancel_booking_error_not_found), true),
                vm.uiState.value.error,
            )
            vm.dismissError()
            assertEquals(BookingsLaunch(BookingsTab.Upcoming), pending())
            assertTrue(vm.uiState.value.isFinished)
        }

    @Test
    fun otherErrorStaysOnConfirm() =
        runTest(main.testDispatcher) {
            stubDetail()
            var calls = 0
            api.cancelExpertBookingHandler = { _, _ ->
                when (++calls) {
                    1 -> throw apiError(500, "SERVER_ERROR", "Boom")
                    2 -> throw IOException("offline")
                    else -> CancelBookingData(0.0, null)
                }
            }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()

            vm.confirm()
            runCurrent()
            assertEquals(CancelErrorDialog(FieldError.Text("Boom"), false), vm.uiState.value.error)
            assertFalse(vm.uiState.value.isSubmitting)
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            vm.dismissError()
            assertNull(vm.uiState.value.error)
            assertNull(pending())
            assertFalse(vm.uiState.value.isFinished)

            vm.confirm()
            runCurrent()
            assertEquals(
                CancelErrorDialog(FieldError.Res(R.string.login_error_network), false),
                vm.uiState.value.error,
            )
            vm.dismissError()

            vm.confirm()
            runCurrent()
            assertEquals(3, api.cancelExpertBookingCalls)
            assertTrue(vm.uiState.value.isFinished)
        }

    @Test
    fun aSuspendedAccountShowsNothing() =
        runTest(main.testDispatcher) {
            stubDetail()
            api.cancelExpertBookingHandler = { _, _ -> throw apiError(403, "ACCOUNT_SUSPENDED") }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()

            vm.confirm()
            runCurrent()

            assertNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isSubmitting)
            assertFalse(vm.uiState.value.isFinished)
        }

    @Test
    fun singleFlight() =
        runTest(main.testDispatcher) {
            stubDetail()
            val gate = CompletableDeferred<CancelBookingData>()
            api.cancelExpertBookingHandler = { _, _ -> gate.await() }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()

            vm.confirm()
            runCurrent()
            vm.confirm()
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitting)
            assertEquals(1, api.cancelExpertBookingCalls)
            gate.complete(CancelBookingData(0.0, null))
            runCurrent()
            assertTrue(vm.uiState.value.isFinished)
        }

    @Test
    fun guardBlocksTheCall() =
        runTest(main.testDispatcher) {
            stubDetail()
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.Other)
            vm.continueToConfirm()
            guard.blocked = true

            vm.confirm()
            runCurrent()

            assertEquals(0, api.cancelExpertBookingCalls)
            assertFalse(vm.uiState.value.isSubmitting)
            assertEquals(listOf("b1" to RestrictedAction.CancelBooking), guard.modifyAttempts)
        }

    @Test
    fun confirmNeedsAReasonAndALoadedBooking() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()

            vm.confirm()
            runCurrent()

            assertEquals(0, api.cancelExpertBookingCalls)
            assertTrue(guard.modifyAttempts.isEmpty())
        }

    @Test
    fun savedStateRestoresStepReasonComment() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle =
                SavedStateHandle(
                    mapOf(
                        ExpertCancellationViewModel.KEY_STEP to "Confirm",
                        ExpertCancellationViewModel.KEY_REASON to "TechnicalIssues",
                        ExpertCancellationViewModel.KEY_COMMENT to "Mic broke",
                    ),
                )
            val vm = vm(handle)
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            assertEquals(ExpertCancelReason.TechnicalIssues, vm.uiState.value.reason)
            assertEquals("Mic broke", vm.uiState.value.comment)
            assertEquals(ExpertRefundPreview.None, vm.uiState.value.refund)

            runCurrent()

            assertEquals(ExpertRefundPreview.HoldReleased, vm.uiState.value.refund)
        }

    @Test
    fun anUnknownSavedReasonRestoresToNoSelection() =
        runTest(main.testDispatcher) {
            stubDetail()
            val handle =
                SavedStateHandle(
                    mapOf(
                        ExpertCancellationViewModel.KEY_STEP to "Confirm",
                        ExpertCancellationViewModel.KEY_REASON to "NotAReason",
                    ),
                )

            val vm = vm(handle)

            assertNull(vm.uiState.value.reason)
            assertEquals(CancelStep.Reason, vm.uiState.value.step)
        }

    @Test
    fun editsAreIgnoredWhileSubmitting() =
        runTest(main.testDispatcher) {
            stubDetail()
            val gate = CompletableDeferred<CancelBookingData>()
            api.cancelExpertBookingHandler = { _, _ -> gate.await() }
            val vm = vm()
            runCurrent()
            vm.selectReason(ExpertCancelReason.TechnicalIssues)
            vm.updateComment("Mic broke")
            vm.continueToConfirm()
            vm.confirm()
            runCurrent()

            vm.selectReason(ExpertCancelReason.Other)
            vm.updateComment("changed")
            vm.backToReason()

            assertEquals(ExpertCancelReason.TechnicalIssues, vm.uiState.value.reason)
            assertEquals("Mic broke", vm.uiState.value.comment)
            assertEquals(CancelStep.Confirm, vm.uiState.value.step)
            gate.complete(CancelBookingData(0.0, null))
            runCurrent()
        }

    @Test
    fun theMergedReasonIsCappedAt500() {
        val merged = expertCancelReason(ExpertCancelReason.SchedulingConflict, "y".repeat(450))

        assertTrue(merged.length <= 500)
        assertTrue(merged.startsWith("Scheduling Conflict: "))
        assertEquals("Other", expertCancelReason(ExpertCancelReason.Other, "   "))
    }
}
