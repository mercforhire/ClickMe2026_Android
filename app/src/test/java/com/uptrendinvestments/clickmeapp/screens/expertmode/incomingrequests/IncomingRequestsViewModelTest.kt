package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsNotice
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.bookingRequestItem
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
class IncomingRequestsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse("2026-10-22T16:00:00Z"))
    private val launchRequest = ExpertLaunchRequest()

    private fun item(id: String) = bookingRequestItem(id = id, expiresAt = clock.instant().plusSeconds(7_200))

    private fun serve(vararg ids: String) {
        api.bookingRequestsHandler = { ids.map { item(it) } }
    }

    private fun vm() = IncomingRequestsViewModel(BookingRequestsRepository(api), launchRequest, clock)

    private fun IncomingRequestsViewModel.rowIds(): List<String> =
        (uiState.value.load as IncomingRequestsLoad.Loaded).rows.map { it.item.requestId }

    @Test
    fun theFirstLoadIsLoudAndSendsNoPagination() =
        runTest(main.testDispatcher) {
            serve("a", "b")
            val vm = vm()
            assertEquals(IncomingRequestsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(listOf("a", "b"), vm.rowIds())
            assertEquals(1, api.bookingRequestsCalls)
        }

    @Test
    fun aFailedFirstLoadShowsTheMessageAndRetryReloads() =
        runTest(main.testDispatcher) {
            api.bookingRequestsHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is IncomingRequestsLoad.Failed)
            serve("a")
            vm.retry()
            assertEquals(IncomingRequestsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(listOf("a"), vm.rowIds())
        }

    @Test
    fun screenShownReloadsQuietly() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            api.bookingRequestsHandler =
                {
                    hold.await()
                    listOf(item("a"), item("b"))
                }
            vm.onScreenShown()
            assertTrue(vm.uiState.value.load is IncomingRequestsLoad.Loaded)
            runCurrent()
            assertEquals(2, api.bookingRequestsCalls)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf("a"), vm.rowIds())
            // A second call while the current reload still runs does nothing.
            vm.onScreenShown()
            runCurrent()
            assertEquals(2, api.bookingRequestsCalls)
            hold.complete(Unit)
            runCurrent()
            assertEquals(listOf("a", "b"), vm.rowIds())
        }

    @Test
    fun screenShownWhileTheFirstLoadRunsAddsNoCall() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            vm.onScreenShown()
            runCurrent()
            assertEquals(1, api.bookingRequestsCalls)
        }

    @Test
    fun screenShownAfterTheScreenWasHiddenReloadsAgain() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            api.bookingRequestsHandler =
                {
                    hold.await()
                    listOf(item("a"))
                }
            vm.onScreenShown()
            runCurrent()
            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            assertEquals(3, api.bookingRequestsCalls)
            hold.complete(Unit)
            runCurrent()
            assertEquals(listOf("a"), vm.rowIds())
        }

    @Test
    fun aPullKeepsTheRowsAndShowsTheIndicator() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            api.bookingRequestsHandler =
                {
                    hold.await()
                    listOf(item("b"))
                }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(listOf("a"), vm.rowIds())
            hold.complete(Unit)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf("b"), vm.rowIds())
        }

    @Test
    fun pullFailureFlagsRefreshFailed() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            api.bookingRequestsHandler = { throw IOException("offline") }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf("a"), vm.rowIds())
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun quietFailureKeepsRowsAndFlagsNothing() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            api.bookingRequestsHandler = { throw IOException("offline") }
            vm.onScreenShown()
            runCurrent()
            assertEquals(2, api.bookingRequestsCalls)
            assertFalse(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf("a"), vm.rowIds())
        }

    @Test
    fun declinedLaunchPostedBeforeInitShowsNoticeOnce() =
        runTest(main.testDispatcher) {
            serve("a")
            launchRequest.incomingRequests.post(IncomingRequestsLaunch(IncomingRequestsNotice.Declined))
            val vm = vm()
            assertEquals(IncomingRequestsNotice.Declined, vm.uiState.value.notice)
            runCurrent()
            assertEquals(1, api.bookingRequestsCalls)
            assertEquals(IncomingRequestsNotice.Declined, vm.uiState.value.notice)
            vm.consumeNotice()
            assertNull(vm.uiState.value.notice)
            assertNull(launchRequest.incomingRequests.consume())
        }

    @Test
    fun launchWhileAliveForcesOneQuietReload() =
        runTest(main.testDispatcher) {
            serve("a", "gone")
            val vm = vm()
            runCurrent()
            serve("a")
            launchRequest.incomingRequests.post(IncomingRequestsLaunch(IncomingRequestsNotice.Declined))
            runCurrent()
            assertEquals(2, api.bookingRequestsCalls)
            assertEquals(listOf("a"), vm.rowIds())
            assertEquals(IncomingRequestsNotice.Declined, vm.uiState.value.notice)
            assertNull(launchRequest.incomingRequests.consume())
        }

    @Test
    fun aLaunchWithoutANoticeStillReloads() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            launchRequest.incomingRequests.post(IncomingRequestsLaunch())
            runCurrent()
            assertEquals(2, api.bookingRequestsCalls)
            assertNull(vm.uiState.value.notice)
        }

    @Test
    fun rowsAreBuiltAgainstTheInjectedClock() =
        runTest(main.testDispatcher) {
            api.bookingRequestsHandler =
                {
                    listOf<BookingRequestItem>(
                        bookingRequestItem(id = "a", expiresAt = clock.instant().minusSeconds(60)),
                    )
                }
            val vm = vm()
            runCurrent()
            val rows = (vm.uiState.value.load as IncomingRequestsLoad.Loaded).rows
            assertTrue(rows.single().isExpired)
        }
}
