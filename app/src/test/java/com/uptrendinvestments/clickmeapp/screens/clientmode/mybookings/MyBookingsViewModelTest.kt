package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
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
class MyBookingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val clock = MutableClock(Instant.parse("2026-10-22T16:00:00Z"))
    private val launchRequest = BookingsLaunchRequest()
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

    private fun vm(handle: SavedStateHandle = saved) =
        MyBookingsViewModel(handle, ClientBookingsRepository(api), realtime, clock, launchRequest)

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
}
