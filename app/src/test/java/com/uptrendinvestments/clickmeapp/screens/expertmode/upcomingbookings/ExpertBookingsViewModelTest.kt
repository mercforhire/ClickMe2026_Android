package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
class ExpertBookingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val clock = MutableClock(Instant.parse("2026-10-22T16:00:00Z"))
    private val launchRequest = ExpertLaunchRequest()
    private val saved = SavedStateHandle()
    private val callStarter = FakeCallStarter()

    private val upcomingRows =
        listOf(expertBookingItem(id = "u1", start = Instant.parse("2026-10-24T18:00:00Z")))
    private val pastRows =
        listOf(
            expertBookingItem(id = "p1", statusCode = "completed", start = Instant.parse("2026-10-20T18:00:00Z")),
            // Completed with a future end: rejoinable, so it lands in Upcoming.
            expertBookingItem(
                id = "r1",
                statusCode = "completed",
                start = Instant.parse("2026-10-22T15:30:00Z"),
                end = Instant.parse("2026-10-22T17:00:00Z"),
            ),
        )

    private fun payload(rows: List<ExpertBookingItem>) = ExpertBookingsPayload(bookings = rows)

    private fun serve(
        upcoming: List<ExpertBookingItem> = upcomingRows,
        past: List<ExpertBookingItem> = pastRows,
    ) {
        api.expertBookingsHandler = { type -> payload(if (type == "upcoming") upcoming else past) }
    }

    private fun vm(
        handle: SavedStateHandle = saved,
        joinScope: CoroutineScope = CoroutineScope(Job() + main.testDispatcher),
    ) = ExpertBookingsViewModel(
        handle,
        ExpertBookingsRepository(api),
        realtime,
        clock,
        launchRequest,
        callStarter,
        JoinClock(clock, joinScope),
    )

    private fun expectedLists(
        upcoming: List<ExpertBookingItem> = upcomingRows,
        past: List<ExpertBookingItem> = pastRows,
    ) = buildExpertBookingLists(upcoming, past, clock.instant())

    @Test
    fun loadsBothBucketsTogether() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            assertEquals(ExpertBookingsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(2, api.expertBookingsCalls)
            assertEquals(
                setOf(Triple("upcoming", 1, 50), Triple("past", 1, 50)),
                api.expertBookingsQueries.toSet(),
            )
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            assertEquals(listOf("r1", "u1"), vm.uiState.value.lists.upcoming.map { it.bookingId })
        }

    @Test
    fun aServerErrorFailsTheWholeLoadWithTheServerMessage() =
        runTest(main.testDispatcher) {
            api.expertBookingsHandler = { type ->
                if (type == "past") {
                    throw api.httpError(500, """{"status":"error","message":"Boom"}""")
                } else {
                    payload(upcomingRows)
                }
            }
            val vm = vm()
            runCurrent()
            assertEquals(ExpertBookingsLoad.Failed(FieldError.Text("Boom")), vm.uiState.value.load)
        }

    @Test
    fun aNetworkErrorGivesTheConnectionMessageAndRetryRecovers() =
        runTest(main.testDispatcher) {
            var failing = true
            api.expertBookingsHandler = { type ->
                if (failing) throw IOException("offline")
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            val vm = vm()
            runCurrent()
            assertEquals(
                ExpertBookingsLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
            failing = false
            vm.retry()
            assertEquals(ExpertBookingsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun realtimeBookingUpdateReloadsQuietly() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            val fresh = listOf(expertBookingItem(id = "u2", start = Instant.parse("2026-10-25T18:00:00Z")))
            api.expertBookingsHandler = { type ->
                hold.await()
                payload(if (type == "upcoming") fresh else emptyList())
            }
            realtime.emit(RealtimeEvent.BookingUpdate("x9", "cancelled"))
            runCurrent()
            assertEquals(4, api.expertBookingsCalls)
            // The reload is in flight and the screen never went back to Loading.
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            hold.complete(Unit)
            runCurrent()
            assertEquals(expectedLists(fresh, emptyList()), vm.uiState.value.lists)
        }

    @Test
    fun aBurstOfRealtimeEventsEndsWithOneCompletedLoad() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val latest = listOf(expertBookingItem(id = "latest", start = Instant.parse("2026-10-25T18:00:00Z")))
            api.expertBookingsHandler = { type -> payload(if (type == "upcoming") latest else emptyList()) }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "confirmed"))
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "in_progress"))
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "cancelled"))
            runCurrent()
            // Two initial calls plus one pair: each event cancels the load the previous event started.
            assertEquals(4, api.expertBookingsCalls)
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(latest, emptyList()), vm.uiState.value.lists)
        }

    @Test
    fun otherRealtimeEventsDoNotReload() =
        runTest(main.testDispatcher) {
            serve()
            vm()
            runCurrent()
            realtime.emit(RealtimeEvent.CallEnded("b1"))
            runCurrent()
            assertEquals(2, api.expertBookingsCalls)
        }

    @Test
    fun screenShownReloadsQuietlyWhenLoaded() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            val fresh = listOf(expertBookingItem(id = "u2", start = Instant.parse("2026-10-25T18:00:00Z")))
            api.expertBookingsHandler = { type ->
                hold.await()
                payload(if (type == "upcoming") fresh else emptyList())
            }
            vm.onScreenShown()
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            runCurrent()
            assertEquals(4, api.expertBookingsCalls)
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            hold.complete(Unit)
            runCurrent()
            assertEquals(expectedLists(fresh, emptyList()), vm.uiState.value.lists)
        }

    @Test
    fun screenShownDuringCurrentLoadDoesNothing() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            vm.onScreenShown()
            runCurrent()
            assertEquals(2, api.expertBookingsCalls)
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun screenShownAfterHiddenRestartsTheLoad() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            api.expertBookingsHandler = { type ->
                hold.await()
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            vm.onScreenShown()
            runCurrent()
            assertEquals(4, api.expertBookingsCalls)
            vm.onScreenShown()
            runCurrent()
            assertEquals(4, api.expertBookingsCalls)
            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            assertEquals(6, api.expertBookingsCalls)
            hold.complete(Unit)
            runCurrent()
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun screenShownOnAFailedScreenRunsALoudLoad() =
        runTest(main.testDispatcher) {
            var failing = true
            api.expertBookingsHandler = { type ->
                if (failing) throw IOException("offline")
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is ExpertBookingsLoad.Failed)
            failing = false
            vm.onScreenHidden()
            vm.onScreenShown()
            assertEquals(ExpertBookingsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun pullFailureFlagsRefreshFailedAndKeepsRows() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            api.expertBookingsHandler = { throw IOException("offline") }
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun quietFailureKeepsRowsSilently() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            api.expertBookingsHandler = { throw IOException("offline") }
            vm.onScreenShown()
            runCurrent()
            assertTrue(api.expertBookingsCalls > 2)
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            assertFalse(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun launchPostedBeforeInitSelectsPastWithNotice() =
        runTest(main.testDispatcher) {
            serve()
            launchRequest.bookings.post(BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled))
            val vm = vm()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            assertEquals(BookingsNotice.Cancelled, vm.uiState.value.notice)
            assertNull(launchRequest.bookings.pending.value)
            runCurrent()
            // A single initial load: no second pair for the hand-off.
            assertEquals(2, api.expertBookingsCalls)
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            assertEquals("Past", saved[ExpertBookingsViewModel.KEY_SELECTED_TAB])
        }

    @Test
    fun launchPostedWhileAliveForcesAQuietReloadAndIsConsumedOnce() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val refund = BookingsNotice.CancelledWithRefund("\$12.50")
            launchRequest.bookings.post(BookingsLaunch(BookingsTab.Past, refund))
            runCurrent()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            assertEquals(refund, vm.uiState.value.notice)
            assertEquals(4, api.expertBookingsCalls)
            assertEquals(ExpertBookingsLoad.Loaded, vm.uiState.value.load)
            assertNull(launchRequest.bookings.pending.value)
            vm.consumeNotice()
            assertNull(vm.uiState.value.notice)
            runCurrent()
            assertEquals(4, api.expertBookingsCalls)
        }

    @Test
    fun selectedTabSurvivesSavedState() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm(SavedStateHandle(mapOf(ExpertBookingsViewModel.KEY_SELECTED_TAB to "Past")))
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            runCurrent()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
        }

    @Test
    fun selectingATabWritesTheSavedStateAndDoesNotFetch() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            vm.selectTab(BookingsTab.Past)
            runCurrent()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            assertEquals("Past", saved[ExpertBookingsViewModel.KEY_SELECTED_TAB])
            assertEquals(2, api.expertBookingsCalls)
        }

    @Test
    fun anUnknownSavedTabFallsBackToUpcoming() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm(SavedStateHandle(mapOf(ExpertBookingsViewModel.KEY_SELECTED_TAB to "Nope")))
            assertEquals(BookingsTab.Upcoming, vm.uiState.value.selectedTab)
            assertNull(vm.uiState.value.notice)
        }

    @Test
    fun joinRecordsTheStartRequestWithTheClientAsPeer() {
        val start = Instant.parse("2026-10-22T16:10:00Z")
        val item =
            expertBookingItem(id = "u9", start = start, end = start.plusSeconds(1800), topic = "Compilers")
        serve()
        val vm = vm()

        vm.join(item)

        assertEquals(
            listOf(
                CallStartRequest(
                    bookingId = "u9",
                    peerId = "c1",
                    peerName = "Ada Lovelace",
                    peerAvatarUrl = null,
                    topic = "Compilers",
                    scheduledStart = start,
                    scheduledEnd = start.plusSeconds(1800),
                    meetingType = MeetingType.InAppVoice,
                ),
            ),
            callStarter.requests,
        )
    }

    @Test
    fun joinMapsAnUnknownMeetingTypeToNull() {
        serve()
        val vm = vm()

        vm.join(expertBookingItem(meetingType = null))

        assertNull(callStarter.requests.single().meetingType)
    }

    @Test
    fun joinNowEmitsTheJoinClockValue() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm(joinScope = backgroundScope)
            runCurrent()
            assertEquals(clock.instant, vm.joinNow.value)
        }
}
