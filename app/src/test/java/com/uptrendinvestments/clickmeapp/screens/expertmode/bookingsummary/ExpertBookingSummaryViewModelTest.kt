package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
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
}
