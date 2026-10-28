package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.NotificationsPayload
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.NOTIF_BOOKING_ID
import com.uptrendinvestments.clickmeapp.testing.notificationItem
import com.uptrendinvestments.clickmeapp.testing.notificationsPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
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
import java.time.Instant
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val launchRequest = PushLaunchRequest()

    // 2026-10-28 15:00 in America/Toronto (EDT, UTC-4).
    private val clock = MutableClock(Instant.parse("2026-10-28T19:00:00Z"))
    private var savedZone: TimeZone = TimeZone.getDefault()

    @Before
    fun pinZone() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(savedZone)
    }

    private fun vm() = NotificationsViewModel(NotificationsRepository(api), launchRequest, clock)

    private fun todayRow(id: String) = notificationItem(id = id, createdAt = Instant.parse("2026-10-28T18:00:00Z"))

    private fun serve(vararg ids: String) {
        api.notificationsHandler = { _, _ -> notificationsPayload(*ids.map(::todayRow).toTypedArray()) }
    }

    private fun NotificationsViewModel.loadedIds(): List<String> =
        (uiState.value.load as NotificationsLoad.Loaded).sections.flatMap { s -> s.rows.map { it.id } }

    @Test
    fun firstLoadGroupsBySection() =
        runTest(main.testDispatcher) {
            api.notificationsHandler = { _, _ ->
                notificationsPayload(
                    notificationItem(id = "t1", createdAt = Instant.parse("2026-10-28T18:00:00Z")),
                    notificationItem(id = "t2", createdAt = Instant.parse("2026-10-28T13:00:00Z")),
                    notificationItem(id = "y1", createdAt = Instant.parse("2026-10-28T00:00:00Z")),
                    notificationItem(id = "e1", createdAt = Instant.parse("2026-10-23T16:00:00Z")),
                )
            }
            val vm = vm()
            assertEquals(NotificationsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            val load = vm.uiState.value.load as NotificationsLoad.Loaded
            assertEquals(
                listOf(NotificationDay.Today, NotificationDay.Yesterday, NotificationDay.Earlier),
                load.sections.map { it.day },
            )
            assertEquals(listOf(2, 1, 1), load.sections.map { it.rows.size })
            assertEquals(listOf("t1", "t2"), load.sections[0].rows.map { it.id })
            assertEquals(listOf(1 to 50), api.notificationsQueries)
        }

    @Test
    fun firstLoadFailureShowsServerMessage() =
        runTest(main.testDispatcher) {
            api.notificationsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            val vm = vm()
            runCurrent()
            assertEquals(NotificationsLoad.Failed(FieldError.Text("Boom")), vm.uiState.value.load)
        }

    @Test
    fun firstLoadNetworkErrorThenRetryLoads() =
        runTest(main.testDispatcher) {
            api.notificationsHandler = { _, _ -> throw IOException("offline") }
            val vm = vm()
            runCurrent()
            assertEquals(
                NotificationsLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
            serve("a")
            vm.retry()
            assertEquals(NotificationsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(listOf("a"), vm.loadedIds())
        }

    @Test
    fun emptyListIsLoadedWithoutSections() =
        runTest(main.testDispatcher) {
            api.notificationsHandler = { _, _ -> NotificationsPayload() }
            val vm = vm()
            runCurrent()
            assertEquals(NotificationsLoad.Loaded(emptyList()), vm.uiState.value.load)
        }

    @Test
    fun arrivalTickReloadsQuietly() =
        runTest(main.testDispatcher) {
            // A push that arrived before the ViewModel existed does not trigger a second request.
            launchRequest.onForegroundArrival()
            serve("a")
            val vm = vm()
            val seen = mutableListOf<NotificationsLoad>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.uiState.collect { seen += it.load }
            }
            runCurrent()
            assertEquals(1, api.notificationsQueries.size)
            seen.clear()

            serve("b", "a")
            launchRequest.onForegroundArrival()
            runCurrent()
            assertEquals(2, api.notificationsQueries.size)
            assertEquals(listOf("b", "a"), vm.loadedIds())
            assertTrue(seen.none { it is NotificationsLoad.Loading })
        }

    @Test
    fun screenShownReloadsQuietlyButNotOnFirstComposition() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            vm.onScreenShown()
            runCurrent()
            assertEquals(1, api.notificationsQueries.size)

            serve("b", "a")
            vm.onScreenHidden()
            vm.onScreenShown()
            assertTrue(vm.uiState.value.load is NotificationsLoad.Loaded)
            runCurrent()
            assertEquals(2, api.notificationsQueries.size)
            assertEquals(listOf("b", "a"), vm.loadedIds())
        }

    @Test
    fun aRunningQuietReloadIsCoalescedUntilTheScreenWasHidden() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            val hold = CompletableDeferred<Unit>()
            api.notificationsHandler = { _, _ ->
                hold.await()
                notificationsPayload(todayRow("a"))
            }
            vm.onScreenShown()
            runCurrent()
            vm.onScreenShown()
            runCurrent()
            assertEquals(2, api.notificationsQueries.size)
            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            assertEquals(3, api.notificationsQueries.size)
            hold.complete(Unit)
            runCurrent()
            assertEquals(listOf("a"), vm.loadedIds())
        }

    @Test
    fun quietReloadFailureKeepsRows() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            api.notificationsHandler = { _, _ -> throw IOException("offline") }
            vm.onScreenShown()
            runCurrent()
            assertEquals(listOf("a"), vm.loadedIds())
            assertFalse(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun pullFailureSetsRefreshFailed() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            api.notificationsHandler = { _, _ -> throw IOException("offline") }
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.refreshFailed)
            assertEquals(listOf("a"), vm.loadedIds())
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun pullSuccessReplacesRows() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            serve("b", "a")
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            assertEquals(listOf("a"), vm.loadedIds())
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf("b", "a"), vm.loadedIds())
        }

    @Test
    fun latestLoadWins() =
        runTest(main.testDispatcher) {
            serve("a")
            val vm = vm()
            runCurrent()
            val gates = listOf(CompletableDeferred<String>(), CompletableDeferred())
            var phase = 0
            api.notificationsHandler = { _, _ -> notificationsPayload(todayRow(gates[phase].await())) }
            launchRequest.onForegroundArrival()
            runCurrent()
            phase = 1
            launchRequest.onForegroundArrival()
            runCurrent()
            gates[0].complete("stale")
            runCurrent()
            assertEquals(listOf("a"), vm.loadedIds())
            gates[1].complete("latest")
            runCurrent()
            assertEquals(listOf("latest"), vm.loadedIds())
        }

    @Test
    fun rowTapPostsDestination() =
        runTest(main.testDispatcher) {
            api.notificationsHandler = { _, _ ->
                notificationsPayload(
                    todayRow("a"),
                    notificationItem(id = "p", category = "payout", subCategory = "paid", metaData = null),
                )
            }
            val vm = vm()
            runCurrent()
            val rows = (vm.uiState.value.load as NotificationsLoad.Loaded).sections.flatMap { it.rows }

            vm.onRowTapped(rows.single { it.id == "p" })
            assertNull(launchRequest.destination.pending.value)

            vm.onRowTapped(rows.single { it.id == "a" })
            assertEquals(
                PushDestination.BookingDetail(NOTIF_BOOKING_ID, PushAudience.Client),
                launchRequest.destination.pending.value,
            )
        }
}
