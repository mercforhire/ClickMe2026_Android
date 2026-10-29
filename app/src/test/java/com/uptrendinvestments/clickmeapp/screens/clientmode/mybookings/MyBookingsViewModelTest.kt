package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewSubmitted
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewOpener
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
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
class MyBookingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val clock = MutableClock(Instant.parse("2026-10-22T16:00:00Z"))
    private val launchRequest = BookingsLaunchRequest()
    private val reviewLaunch = ReviewLaunchRequest()
    private val saved = SavedStateHandle()

    private val upcomingRows =
        listOf(bookingItem(id = "u1", start = Instant.parse("2026-10-24T18:00:00Z")))
    private val pastRows =
        listOf(
            bookingItem(id = "p1", status = "completed", start = Instant.parse("2026-10-20T18:00:00Z")),
            // Completed with a future end: rejoinable, so it lands in Upcoming.
            bookingItem(
                id = "r1",
                status = "completed",
                start = Instant.parse("2026-10-22T15:30:00Z"),
                end = Instant.parse("2026-10-22T17:00:00Z"),
            ),
        )

    private fun payload(rows: List<ClientBookingItem>) = ClientBookingsPayload(bookings = rows)

    private fun serve(
        upcoming: List<ClientBookingItem> = upcomingRows,
        past: List<ClientBookingItem> = pastRows,
    ) {
        api.clientBookingsHandler = { type -> payload(if (type == "upcoming") upcoming else past) }
    }

    private val callStarter = FakeCallStarter()

    private fun vm(
        handle: SavedStateHandle = saved,
        joinScope: CoroutineScope = CoroutineScope(Job() + main.testDispatcher),
    ) = MyBookingsViewModel(
        handle,
        ClientBookingsRepository(api),
        realtime,
        clock,
        launchRequest,
        reviewLaunch,
        callStarter,
        JoinClock(clock, joinScope),
    )

    private fun expectedLists(
        upcoming: List<ClientBookingItem> = upcomingRows,
        past: List<ClientBookingItem> = pastRows,
    ) = buildBookingLists(upcoming, past, clock.instant())

    @Test
    fun startsLoadingThenLoadsBothBucketsOnPageOne() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            assertEquals(MyBookingsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(2, api.clientBookingsCalls)
            assertEquals(
                setOf(Triple("upcoming", 1, 20), Triple("past", 1, 20)),
                api.clientBookingsQueries.toSet(),
            )
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
        }

    @Test
    fun aCompletedRowWithAFutureEndShowsInUpcoming() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val lists = vm.uiState.value.lists
            assertEquals(listOf("r1", "u1"), lists.upcoming.map { it.bookingId })
            assertEquals(listOf("p1"), lists.past.map { it.item.bookingId })
        }

    @Test
    fun aServerErrorOnOneBucketFailsTheWholeLoadWithTheServerMessage() =
        runTest(main.testDispatcher) {
            api.clientBookingsHandler = { type ->
                if (type == "past") {
                    throw api.httpError(500, """{"status":"error","message":"Boom"}""")
                } else {
                    payload(upcomingRows)
                }
            }
            val vm = vm()
            runCurrent()
            assertEquals(MyBookingsLoad.Failed(FieldError.Text("Boom")), vm.uiState.value.load)
        }

    @Test
    fun aNetworkErrorGivesTheConnectionMessage() =
        runTest(main.testDispatcher) {
            api.clientBookingsHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()
            assertEquals(
                MyBookingsLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
        }

    @Test
    fun retryGoesLoadingThenLoaded() =
        runTest(main.testDispatcher) {
            var failing = true
            api.clientBookingsHandler = { type ->
                if (failing) throw IOException("offline")
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is MyBookingsLoad.Failed)
            failing = false
            vm.retry()
            assertEquals(MyBookingsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
        }

    @Test
    fun refreshKeepsTheListsVisibleWhileLoadingAndShowsTheNewOnes() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            val fresh = listOf(bookingItem(id = "u2", start = Instant.parse("2026-10-25T18:00:00Z")))
            api.clientBookingsHandler = { type ->
                gate.await()
                payload(if (type == "upcoming") fresh else emptyList())
            }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            gate.complete(Unit)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(expectedLists(fresh, emptyList()), vm.uiState.value.lists)
        }

    @Test
    fun aFailedRefreshKeepsTheOldListsAndFlagsTheFailure() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            api.clientBookingsHandler = { throw IOException("offline") }
            vm.refresh()
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun refreshFromTheFailedStateThatSucceedsEndsLoaded() =
        runTest(main.testDispatcher) {
            var failing = true
            api.clientBookingsHandler = { type ->
                if (failing) throw IOException("offline")
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is MyBookingsLoad.Failed)
            failing = false
            vm.refresh()
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
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
            assertEquals("Past", saved[MyBookingsViewModel.KEY_SELECTED_TAB])
            assertEquals(2, api.clientBookingsCalls)
        }

    @Test
    fun theSelectedTabIsRestoredFromTheSavedState() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm(SavedStateHandle(mapOf(MyBookingsViewModel.KEY_SELECTED_TAB to "Past")))
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            runCurrent()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
        }

    @Test
    fun anUnknownSavedTabFallsBackToUpcoming() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm(SavedStateHandle(mapOf(MyBookingsViewModel.KEY_SELECTED_TAB to "Nope")))
            assertEquals(BookingsTab.Upcoming, vm.uiState.value.selectedTab)
            assertNull(vm.uiState.value.notice)
        }

    @Test
    fun onScreenShownWhileTheInitialLoadRunsAddsNoCalls() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            vm.onScreenShown()
            runCurrent()
            assertEquals(2, api.clientBookingsCalls)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun onScreenShownOnALoadedScreenReloadsQuietlyWithoutLoading() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            val fresh = listOf(bookingItem(id = "u2", start = Instant.parse("2026-10-25T18:00:00Z")))
            api.clientBookingsHandler = { type ->
                hold.await()
                payload(if (type == "upcoming") fresh else emptyList())
            }
            vm.onScreenShown()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            runCurrent()
            assertEquals(4, api.clientBookingsCalls)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            hold.complete(Unit)
            runCurrent()
            assertEquals(expectedLists(fresh, emptyList()), vm.uiState.value.lists)
        }

    @Test
    fun aFailedQuietReloadKeepsTheListsAndDoesNotFlagARefreshFailure() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            api.clientBookingsHandler = { throw IOException("offline") }
            vm.onScreenShown()
            runCurrent()
            // The first failing bucket cancels its sibling, so the reload made 1 or 2 calls.
            assertTrue(api.clientBookingsCalls > 2)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            assertFalse(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    // Phase 15 D-05: realtime refetch failures flag the refresh-failed snackbar, tab-return reloads stay silent.

    @Test
    fun realtimeFailureWithRowsFlagsRefreshFailed() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            api.clientBookingsHandler = { throw IOException("offline") }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "cancelled"))
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun quietTabReturnFailureIsSilent() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            api.clientBookingsHandler = { throw IOException("offline") }
            vm.onScreenShown()
            runCurrent()
            assertFalse(vm.uiState.value.refreshFailed)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
        }

    @Test
    fun realtimeDuringLoadingStaysLoud() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.clientBookingsHandler = {
                gate.await()
                throw IOException("offline")
            }
            val vm = vm()
            runCurrent()
            assertEquals(MyBookingsLoad.Loading, vm.uiState.value.load)
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "confirmed"))
            runCurrent()
            gate.complete(Unit)
            runCurrent()
            assertTrue(vm.uiState.value.load is MyBookingsLoad.Failed)
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun retryDuringRealtimeIsLatestWins() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val gates = listOf(CompletableDeferred<Unit>(), CompletableDeferred())
            var phase = 0
            val fresh = listOf(bookingItem(id = "u2", start = Instant.parse("2026-10-25T18:00:00Z")))
            api.clientBookingsHandler = { type ->
                gates[phase].await()
                payload(if (type == "upcoming") fresh else emptyList())
            }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "confirmed"))
            runCurrent()
            phase = 1
            vm.refresh()
            runCurrent()
            gates[1].complete(Unit)
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(fresh, emptyList()), vm.uiState.value.lists)
            assertFalse(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun aSuspendedQuietReloadIsCoalescedUntilTheScreenWasHidden() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            api.clientBookingsHandler = { type ->
                hold.await()
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            vm.onScreenShown()
            runCurrent()
            assertEquals(4, api.clientBookingsCalls)
            vm.onScreenShown()
            runCurrent()
            assertEquals(4, api.clientBookingsCalls)
            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            assertEquals(6, api.clientBookingsCalls)
            hold.complete(Unit)
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun onScreenShownOnAFailedScreenRunsALoudLoad() =
        runTest(main.testDispatcher) {
            var failing = true
            api.clientBookingsHandler = { type ->
                if (failing) throw IOException("offline")
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is MyBookingsLoad.Failed)
            failing = false
            vm.onScreenHidden()
            vm.onScreenShown()
            assertEquals(MyBookingsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun aRealtimeBookingUpdateForAnyBookingTriggersOneQuietReload() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            realtime.emit(RealtimeEvent.BookingUpdate("x9", "cancelled"))
            runCurrent()
            assertEquals(4, api.clientBookingsCalls)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertEquals(expectedLists(), vm.uiState.value.lists)
        }

    @Test
    fun otherRealtimeEventsDoNotReload() =
        runTest(main.testDispatcher) {
            serve()
            vm()
            runCurrent()
            realtime.emit(RealtimeEvent.CallEnded("b1"))
            runCurrent()
            assertEquals(2, api.clientBookingsCalls)
        }

    @Test
    fun twoRealtimeEventsInARowLeaveTheLatestResponse() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val gates = listOf(CompletableDeferred<List<ClientBookingItem>>(), CompletableDeferred())
            var phase = 0
            api.clientBookingsHandler = { type ->
                val rows = gates[phase].await()
                payload(if (type == "upcoming") rows else emptyList())
            }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "confirmed"))
            runCurrent()
            phase = 1
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "cancelled"))
            runCurrent()
            val stale = listOf(bookingItem(id = "stale"))
            val latest = listOf(bookingItem(id = "latest"))
            gates[0].complete(stale)
            runCurrent()
            assertEquals(expectedLists(), vm.uiState.value.lists)
            gates[1].complete(latest)
            runCurrent()
            assertEquals(expectedLists(latest, emptyList()), vm.uiState.value.lists)
        }

    @Test
    fun aLaunchPostedBeforeTheViewModelExistsIsAppliedOnCreation() =
        runTest(main.testDispatcher) {
            serve()
            launchRequest.post(BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled))
            val vm = vm()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            assertEquals(BookingsNotice.Cancelled, vm.uiState.value.notice)
            assertNull(launchRequest.pending.value)
            runCurrent()
            assertEquals(2, api.clientBookingsCalls)
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
        }

    @Test
    fun aLaunchPostedWhileLoadedSwitchesTabSetsTheNoticeAndForcesAQuietReload() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            launchRequest.post(BookingsLaunch(BookingsTab.Past, BookingsNotice.CancelledWithRefund("CA\$12.50")))
            runCurrent()
            assertEquals(BookingsTab.Past, vm.uiState.value.selectedTab)
            assertEquals("Past", saved[MyBookingsViewModel.KEY_SELECTED_TAB])
            assertEquals(BookingsNotice.CancelledWithRefund("CA\$12.50"), vm.uiState.value.notice)
            assertEquals(4, api.clientBookingsCalls)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertNull(launchRequest.pending.value)
            vm.consumeNotice()
            assertNull(vm.uiState.value.notice)
        }

    @Test
    fun joinRecordsTheStartRequestForTheItem() {
        val start = Instant.parse("2026-10-22T16:10:00Z")
        val item =
            ClientBookingItem(
                bookingId = "u9",
                expert = BookingExpertDto(id = "e9", fullName = "Grace Hopper", avatarUrl = "https://img/a.png"),
                topic = "Compilers",
                startTime = start,
                endTime = start.plusSeconds(1800),
                meetingType = "in_app_voice",
                status = "confirmed",
            )
        serve()
        val vm = vm()

        vm.join(item)

        assertEquals(
            listOf(
                CallStartRequest(
                    bookingId = "u9",
                    peerId = "e9",
                    peerName = "Grace Hopper",
                    peerAvatarUrl = "https://img/a.png",
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

        vm.join(bookingItem(meetingType = null))

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

    // D-19 review hand-off

    @Test
    fun reviewHandOffReloadsQuietlyAndThanks() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val callsBefore = api.clientBookingsCalls
            val gate = CompletableDeferred<Unit>()
            api.clientBookingsHandler = { type ->
                gate.await()
                payload(if (type == "upcoming") upcomingRows else pastRows)
            }

            reviewLaunch.bookingsReviewed.post(ReviewSubmitted("p1"))
            runCurrent()

            assertNull(reviewLaunch.bookingsReviewed.pending.value)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            assertTrue(vm.uiState.value.reviewThanks)
            gate.complete(Unit)
            runCurrent()
            assertEquals(callsBefore + 2, api.clientBookingsCalls)
            assertEquals(MyBookingsLoad.Loaded, vm.uiState.value.load)
            vm.consumeReviewThanks()
            assertFalse(vm.uiState.value.reviewThanks)
        }

    @Test
    fun aReviewHandOffPostedBeforeTheViewModelExistsThanksWithoutASecondLoad() =
        runTest(main.testDispatcher) {
            serve()
            reviewLaunch.bookingsReviewed.post(ReviewSubmitted("p1"))
            val vm = vm()
            runCurrent()

            assertNull(reviewLaunch.bookingsReviewed.pending.value)
            assertTrue(vm.uiState.value.reviewThanks)
            assertEquals(2, api.clientBookingsCalls)
        }

    @Test
    fun summaryHandOffIsLeftAlone() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            val callsBefore = api.clientBookingsCalls

            reviewLaunch.onSubmitted("p1", ReviewOpener.BookingSummary)
            runCurrent()

            assertEquals(ReviewSubmitted("p1"), reviewLaunch.summaryReviewed.pending.value)
            assertEquals(callsBefore, api.clientBookingsCalls)
            assertFalse(vm.uiState.value.reviewThanks)
        }

    @Test
    fun reviewedIdsMirrorLaunchRequest() =
        runTest(main.testDispatcher) {
            serve()
            val vm = vm()
            runCurrent()
            assertEquals(emptySet<String>(), vm.reviewedBookingIds.value)

            reviewLaunch.onSubmitted("p1", ReviewOpener.BookingSummary)
            reviewLaunch.onSubmitted("p2", ReviewOpener.MyBookings)

            assertEquals(setOf("p1", "p2"), vm.reviewedBookingIds.value)
        }
}
