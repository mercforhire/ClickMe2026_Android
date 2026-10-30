package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallResumeNotice
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleSubmitting
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ExpertBookingSummaryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val start = Instant.parse("2026-10-24T18:00:00Z")
    private val clock = MutableClock(start.minusSeconds(1800))
    private val now = clock.instant
    private val callStarter = FakeCallStarter()
    private val callGuard = FakeCallGuard()
    private val notice = CallResumeNotice()

    private val repository = ExpertBookingsRepository(api)

    private fun vm(joinScope: CoroutineScope = CoroutineScope(Job() + main.testDispatcher)) =
        ExpertBookingSummaryViewModel(
            ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Bookings),
            repository,
            ExpertRescheduleResponder(repository),
            realtime,
            clock,
            callStarter,
            callGuard,
            JoinClock(clock, joinScope),
            notice,
        )

    private fun loaded(vm: ExpertBookingSummaryViewModel): ExpertSummaryDetails =
        (vm.uiState.value.load as ExpertSummaryLoad.Loaded).details

    private fun detail(
        statusCode: String = "confirmed",
        notes: String? = null,
    ) = expertBookingDetail(start = start, statusCode = statusCode, notes = notes)

    // ---- mapping ----

    @Test
    fun mapsTheDetailForDisplay() {
        val details = detail(notes = "  Please review my CV  ").toSummaryDetails(now, Locale.US)

        assertEquals("B1", details.shortId)
        assertEquals("confirmed", details.statusCode)
        assertEquals(BookingStatusLabel.Confirmed, details.status.label)
        assertEquals("c1", details.clientId)
        assertEquals(60, details.durationMinutes)
        assertEquals(SummaryFee.Amount("\$75.00"), details.fee)
        assertEquals("Please review my CV", details.preparationNotes)
        assertTrue(details.canCancel)
        assertTrue(details.showsJoin)
    }

    @Test
    fun blankNotesMapToNull() {
        assertNull(detail(notes = "   ").toSummaryDetails(now, Locale.US).preparationNotes)
        assertNull(detail(notes = null).toSummaryDetails(now, Locale.US).preparationNotes)
    }

    @Test
    fun showsJoinFollowsTheStatusAndTheEndTime() {
        listOf("confirmed", "in_progress", "pending_reschedule").forEach {
            assertTrue(it, detail(it).toSummaryDetails(now, Locale.US).showsJoin)
        }
        listOf("cancelled", "declined", "missed", "expired", "pending_approval").forEach {
            assertFalse(it, detail(it).toSummaryDetails(now, Locale.US).showsJoin)
        }
        assertTrue(detail("completed").toSummaryDetails(now, Locale.US).showsJoin)
        assertFalse(detail("completed").toSummaryDetails(start.plusSeconds(7200), Locale.US).showsJoin)
    }

    // ---- loading ----

    @Test
    fun loadsDetailsForKey() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ExpertBookingDetail>()
            val ids = mutableListOf<String>()
            api.expertBookingDetailsHandler = {
                ids += it
                gate.await()
            }
            val vm = vm()
            runCurrent()
            assertEquals(ExpertSummaryLoad.Loading, vm.uiState.value.load)
            assertEquals(listOf("b1"), ids)

            gate.complete(detail())
            runCurrent()
            assertEquals("b1", loaded(vm).bookingId)
            assertEquals(1, api.expertBookingDetailsCalls)
            assertEquals("b1", vm.loadedDetail?.bookingId)
        }

    @Test
    fun networkFailureEndsFailedAndRetryLoadsAgain() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.expertBookingDetailsHandler = {
                calls++
                if (calls == 1) throw IOException("offline") else detail()
            }
            val vm = vm()
            runCurrent()
            assertEquals(
                ExpertSummaryLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )

            vm.retry()
            assertEquals(ExpertSummaryLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(2, api.expertBookingDetailsCalls)
            assertTrue(vm.uiState.value.load is ExpertSummaryLoad.Loaded)
        }

    // ---- quiet reloads ----

    @Test
    fun refreshQuietlyWhileCurrentLoadDoesNothing() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ExpertBookingDetail>()
            api.expertBookingDetailsHandler = { gate.await() }
            val vm = vm()
            runCurrent()

            vm.refreshQuietly()
            runCurrent()
            assertEquals(1, api.expertBookingDetailsCalls)

            gate.complete(detail())
            runCurrent()
            assertEquals(1, api.expertBookingDetailsCalls)
        }

    @Test
    fun refreshQuietlyWhenLoadedReloadsWithoutShowingLoading() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()
            val seen = mutableListOf<ExpertSummaryLoad>()

            api.expertBookingDetailsHandler = { detail("in_progress") }
            vm.refreshQuietly()
            seen += vm.uiState.value.load
            runCurrent()
            seen += vm.uiState.value.load

            assertEquals(2, api.expertBookingDetailsCalls)
            assertTrue(seen.all { it is ExpertSummaryLoad.Loaded })
            assertEquals(BookingStatusLabel.InProgress, loaded(vm).status.label)
        }

    @Test
    fun quietFailureKeepsLoaded() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value

            api.expertBookingDetailsHandler = { throw IOException("offline") }
            vm.refreshQuietly()
            runCurrent()

            assertEquals(2, api.expertBookingDetailsCalls)
            assertEquals(before, vm.uiState.value)
        }

    @Test
    fun refreshQuietlyAfterTheScreenWasHiddenRestartsTheLoad() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            val gate = CompletableDeferred<ExpertBookingDetail>()
            api.expertBookingDetailsHandler = { gate.await() }
            vm.refreshQuietly()
            runCurrent()
            assertEquals(2, api.expertBookingDetailsCalls)

            vm.refreshQuietly()
            runCurrent()
            assertEquals(2, api.expertBookingDetailsCalls)

            vm.onScreenHidden()
            vm.refreshQuietly()
            runCurrent()
            assertEquals(3, api.expertBookingDetailsCalls)
        }

    @Test
    fun reloadQuietlyWaitsForTheReload() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            api.expertBookingDetailsHandler = { detail("completed") }
            val job = launch { vm.reloadQuietly() }
            runCurrent()

            assertTrue(job.isCompleted)
            assertEquals(2, api.expertBookingDetailsCalls)
            assertEquals(BookingStatusLabel.Completed, loaded(vm).status.label)
        }

    // ---- realtime ----

    @Test
    fun realtimeForThisBookingReloadsQuietly() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            api.expertBookingDetailsHandler = { detail("completed") }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "completed"))
            assertTrue(vm.uiState.value.load is ExpertSummaryLoad.Loaded)
            runCurrent()

            assertEquals(2, api.expertBookingDetailsCalls)
            assertEquals(BookingStatusLabel.Completed, loaded(vm).status.label)
        }

    @Test
    fun realtimeForAnotherBookingDoesNotReload() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            realtime.emit(RealtimeEvent.BookingUpdate("other", "completed"))
            runCurrent()

            assertEquals(1, api.expertBookingDetailsCalls)
            assertEquals(BookingStatusLabel.Confirmed, loaded(vm).status.label)
        }

    // ---- call resume note (D-11) ----

    @Test
    fun noteShownOnceWhenNoticePosted() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            notice.post("b1")
            val first = vm()
            runCurrent()
            val second = vm()
            runCurrent()

            assertTrue(first.uiState.value.showCallResumeNote)
            assertFalse(second.uiState.value.showCallResumeNote)
        }

    @Test
    fun noNoteForOtherBooking() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            notice.post("b2")
            val vm = vm()
            runCurrent()

            assertFalse(vm.uiState.value.showCallResumeNote)
        }

    @Test
    fun noteSurvivesReloads() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            notice.post("b1")
            val vm = vm()
            runCurrent()

            vm.refreshQuietly()
            runCurrent()

            assertTrue(vm.uiState.value.showCallResumeNote)
        }

    // ---- failed refresh with content (D-05) ----

    @Test
    fun realtimeFailureKeepsContentAndFlags() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()
            val before = loaded(vm)

            api.expertBookingDetailsHandler = { throw IOException("offline") }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "completed"))
            runCurrent()

            assertEquals(before, loaded(vm))
            assertTrue(vm.uiState.value.refreshFailed)

            vm.onRefreshFailureConsumed()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun aQuietResumeReloadFailureStaysSilent() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            api.expertBookingDetailsHandler = { throw IOException("offline") }
            vm.refreshQuietly()
            runCurrent()

            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun refreshKeepsContentWhileRunning() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()
            val before = loaded(vm)

            val gate = CompletableDeferred<ExpertBookingDetail>()
            api.expertBookingDetailsHandler = { gate.await() }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(before, loaded(vm))

            gate.complete(detail("in_progress"))
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertFalse(vm.uiState.value.refreshFailed)
            assertEquals(BookingStatusLabel.InProgress, loaded(vm).status.label)
        }

    @Test
    fun refreshFailureFlagsAndClearsTheRunningState() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()
            val before = loaded(vm)

            api.expertBookingDetailsHandler = { throw IOException("offline") }
            vm.refresh()
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.refreshFailed)
            assertEquals(before, loaded(vm))
        }

    // ---- unavailable (UI-SPEC Detail rule 5) ----

    @Test
    fun aNotFoundFirstLoadIsUnavailableNotFailed() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
            }
            val vm = vm()
            runCurrent()

            assertEquals(ExpertSummaryLoad.Unavailable, vm.uiState.value.load)
        }

    // ---- join and cancel ----

    @Test
    fun joinBeforeLoadIsNoOp() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ExpertBookingDetail>()
            api.expertBookingDetailsHandler = { gate.await() }
            val vm = vm()
            runCurrent()

            vm.join()

            assertTrue(callStarter.requests.isEmpty())
        }

    @Test
    fun joinStartsTheCallWithTheClientAsPeer() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            vm.join()

            val request = callStarter.requests.single()
            assertEquals("b1", request.bookingId)
            assertEquals("c1", request.peerId)
            assertEquals("Ada Lovelace", request.peerName)
            assertEquals(start, request.scheduledStart)
        }

    @Test
    fun attemptCancelGoesThroughTheCallGuard() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { detail() }
            val vm = vm()
            runCurrent()

            assertTrue(vm.attemptCancel())
            callGuard.blocked = true
            assertFalse(vm.attemptCancel())

            assertEquals(
                listOf("b1" to RestrictedAction.CancelBooking, "b1" to RestrictedAction.CancelBooking),
                callGuard.modifyAttempts,
            )
        }

    // ---- reschedule response (11-13) ----

    private fun proposal(initiatedBy: String = "client") =
        RescheduleDto(
            proposedStart = start.plusSeconds(86_400),
            proposedEnd = start.plusSeconds(86_400 + 3600),
            initiatedBy = initiatedBy,
            note = "  Can we move it?  ",
        )

    private fun pending(initiatedBy: String = "client") =
        expertBookingDetail(start = start, statusCode = "pending_reschedule", reschedule = proposal(initiatedBy))

    private fun error(
        status: Int,
        code: String,
        message: String = "server says no",
    ) = api.httpError(status, """{"status":"error","code":"$code","message":"$message"}""")

    private val accepted = RescheduleResponseData(bookingId = "b1", status = "confirmed")

    @Test
    fun clientProposalMapsToAnActionableProposalWithoutPaymentText() {
        val ui = pending("client").toSummaryDetails(now, Locale.US).proposal!!

        assertFalse(ui.initiatedByExpert)
        assertEquals(start, ui.currentStart)
        assertEquals(start.plusSeconds(86_400), ui.proposedStart)
        assertEquals("Can we move it?", ui.note)
        assertFalse(ui.showsPaymentDisclosure)
        assertFalse(ui.chargesSoon)
    }

    @Test
    fun expertProposalMapsToAReadOnlyProposal() {
        assertTrue(pending("expert").toSummaryDetails(now, Locale.US).proposal!!.initiatedByExpert)
    }

    @Test
    fun noProposalOutsidePendingRescheduleOrWithoutATime() {
        assertNull(detail("confirmed").toSummaryDetails(now, Locale.US).proposal)
        val noTime = pending().copy(reschedule = RescheduleDto(initiatedBy = "client"))
        assertNull(noTime.toSummaryDetails(now, Locale.US).proposal)
    }

    @Test
    fun acceptSendsAnEmptyBodyGuardedAndReloads() =
        runTest(main.testDispatcher) {
            var current = pending()
            api.expertBookingDetailsHandler = { current }
            api.acceptExpertRescheduleHandler = { _, _ ->
                current = detail("confirmed")
                accepted
            }
            val vm = vm()
            runCurrent()
            assertEquals("pending_reschedule", loaded(vm).statusCode)

            vm.acceptReschedule()
            runCurrent()

            assertEquals(listOf("b1" to RestrictedAction.RespondToReschedule), callGuard.modifyAttempts)
            assertEquals(listOf("b1" to AcceptRescheduleRequest()), api.acceptExpertRescheduleRequests)
            assertEquals(2, api.expertBookingDetailsCalls)
            assertEquals("confirmed", loaded(vm).statusCode)
            assertNull(loaded(vm).proposal)
            assertEquals(RescheduleNotice.Accepted, vm.rescheduleState.value.notice)
            assertFalse(vm.rescheduleState.value.isSubmitting)
        }

    @Test
    fun declineSendsTrimmedReasonAndReloads() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.declineExpertRescheduleHandler = { _, _ -> accepted }
            val vm = vm()
            runCurrent()

            vm.declineReschedule("  Busy  ")
            runCurrent()

            assertEquals(listOf("b1" to DeclineRescheduleRequest("Busy")), api.declineExpertRescheduleRequests)
            assertEquals(listOf("b1" to RestrictedAction.RespondToReschedule), callGuard.modifyAttempts)
            assertEquals(2, api.expertBookingDetailsCalls)
            assertEquals(RescheduleNotice.Declined, vm.rescheduleState.value.notice)
        }

    @Test
    fun blankDeclineReasonSendsNull() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.declineExpertRescheduleHandler = { _, _ -> accepted }
            val vm = vm()
            runCurrent()

            vm.declineReschedule("   ")
            runCurrent()

            assertEquals(listOf("b1" to DeclineRescheduleRequest(null)), api.declineExpertRescheduleRequests)
        }

    @Test
    fun aLiveCallOnThisBookingBlocksBothResponses() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            val vm = vm()
            runCurrent()
            callGuard.blocked = true

            vm.acceptReschedule()
            vm.declineReschedule("no")
            runCurrent()

            assertEquals(0, api.acceptExpertRescheduleCalls)
            assertEquals(0, api.declineExpertRescheduleCalls)
            assertFalse(vm.rescheduleState.value.isSubmitting)
        }

    @Test
    fun slotUnavailableShowsDialogAndReloadsWithoutAcceptingTheReschedule() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.acceptExpertRescheduleHandler = { _, _ -> throw error(409, "SLOT_UNAVAILABLE") }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()

            val state = vm.rescheduleState.value
            assertEquals(
                RescheduleDialog.Error(FieldError.Res(R.string.expert_reschedule_error_slot_unavailable)),
                state.dialog,
            )
            assertNull(state.notice)
            assertEquals(2, api.expertBookingDetailsCalls)
            // The booking is whatever the server says now: still waiting on the client's proposal, never shown as moved.
            assertEquals("pending_reschedule", loaded(vm).statusCode)
            assertEquals(start, loaded(vm).start)
            assertTrue(loaded(vm).proposal != null)
        }

    @Test
    fun invalidStatusShowsNotPendingAndReloads() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.acceptExpertRescheduleHandler = { _, _ -> throw error(409, "INVALID_STATUS") }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()

            assertEquals(
                RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_not_pending)),
                vm.rescheduleState.value.dialog,
            )
            assertEquals(2, api.expertBookingDetailsCalls)
            assertNull(vm.rescheduleState.value.notice)
        }

    @Test
    fun serverTextErrorsReload() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.acceptExpertRescheduleHandler = { _, _ -> throw error(409, "CLIENT_UNAVAILABLE", "Client is busy") }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()

            assertEquals(RescheduleDialog.Error(FieldError.Text("Client is busy")), vm.rescheduleState.value.dialog)
            assertEquals(2, api.expertBookingDetailsCalls)
        }

    @Test
    fun idempotentRepeatIsSuccess() =
        runTest(main.testDispatcher) {
            // The booking is already confirmed; the server answers a repeat accept with 200.
            api.expertBookingDetailsHandler = { detail("confirmed") }
            api.acceptExpertRescheduleHandler = { _, _ -> accepted }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()

            assertEquals(RescheduleNotice.Accepted, vm.rescheduleState.value.notice)
            assertNull(vm.rescheduleState.value.dialog)
        }

    @Test
    fun singleFlight() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<RescheduleResponseData>()
            api.expertBookingDetailsHandler = { pending() }
            api.acceptExpertRescheduleHandler = { _, _ -> gate.await() }
            api.declineExpertRescheduleHandler = { _, _ -> accepted }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()
            assertEquals(RescheduleSubmitting.Accepting, vm.rescheduleState.value.submitting)

            vm.acceptReschedule()
            vm.declineReschedule("no")
            runCurrent()
            assertEquals(1, api.acceptExpertRescheduleCalls)
            assertEquals(0, api.declineExpertRescheduleCalls)

            gate.complete(accepted)
            runCurrent()
            assertEquals(RescheduleSubmitting.None, vm.rescheduleState.value.submitting)
            assertEquals(RescheduleNotice.Accepted, vm.rescheduleState.value.notice)
        }

    @Test
    fun notFoundPopsAfterDismiss() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.acceptExpertRescheduleHandler = { _, _ -> throw error(404, "NOT_FOUND") }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()

            assertEquals(
                RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_not_found), popOnDismiss = true),
                vm.rescheduleState.value.dialog,
            )
            vm.dismissRescheduleDialog()
            assertNull(vm.rescheduleState.value.dialog)
        }

    @Test
    fun noticeIsConsumedOnce() =
        runTest(main.testDispatcher) {
            api.expertBookingDetailsHandler = { pending() }
            api.acceptExpertRescheduleHandler = { _, _ -> accepted }
            val vm = vm()
            runCurrent()

            vm.acceptReschedule()
            runCurrent()
            vm.consumeRescheduleNotice()

            assertNull(vm.rescheduleState.value.notice)
        }
}
