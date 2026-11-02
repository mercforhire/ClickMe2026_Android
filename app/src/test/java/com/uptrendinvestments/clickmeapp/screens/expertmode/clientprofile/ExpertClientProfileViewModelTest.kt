@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingClientDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.testing.expertClientProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

class ExpertClientProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm(clientId: String = "c1") =
        ExpertClientProfileViewModel(
            ExpertClientProfileKey(clientId, ExpertBookingOrigin.Bookings),
            ExpertClientProfileRepository(api),
            ExpertBookingsRepository(api),
        )

    private fun at(day: Int): Instant = Instant.parse("2026-10-${day.toString().padStart(2, '0')}T18:00:00Z")

    private fun stub(
        upcoming: List<ExpertBookingItem> = emptyList(),
        past: List<ExpertBookingItem> = emptyList(),
        profile: ExpertClientProfileData = expertClientProfile(),
    ) {
        api.getExpertClientHandler = { profile }
        api.expertBookingsHandler = { type ->
            ExpertBookingsPayload(bookings = if (type == ExpertBookingsRepository.TYPE_UPCOMING) upcoming else past)
        }
    }

    private fun ExpertClientProfileViewModel.loaded() = uiState.value.load as ClientProfileLoad.Loaded

    @Test
    fun loadsTheProfileAndMergesHistoryNewestFirst() =
        runTest(main.testDispatcher) {
            val profile = expertClientProfile()
            stub(
                upcoming = listOf(expertBookingItem(id = "u1", start = at(28))),
                past =
                    listOf(
                        expertBookingItem(id = "p1", statusCode = "completed", start = at(10)),
                        expertBookingItem(id = "p2", statusCode = "completed", start = at(20)),
                    ),
                profile = profile,
            )
            val vm = vm()
            runCurrent()

            val loaded = vm.loaded()
            assertEquals(profile, loaded.profile)
            assertEquals(listOf("u1", "p2", "p1"), loaded.history.map { it.item.bookingId })
        }

    @Test
    fun requestsBothBucketsOnPageOneLimitFifty() =
        runTest(main.testDispatcher) {
            stub()
            vm()
            runCurrent()

            assertEquals(
                setOf(Triple("upcoming", 1, 50), Triple("past", 1, 50)),
                api.expertBookingsQueries.toSet(),
            )
            assertEquals(2, api.expertBookingsQueries.size)
        }

    @Test
    fun historyOnlyHoldsThisClient() =
        runTest(main.testDispatcher) {
            val other = expertBookingItem(id = "x1").let { it.copy(client = ExpertBookingClientDto(id = "other")) }
            stub(upcoming = listOf(expertBookingItem(id = "u1"), other))
            val vm = vm()
            runCurrent()

            assertEquals(listOf("u1"), vm.loaded().history.map { it.item.bookingId })
        }

    @Test
    fun clientIdsCompareCaseInsensitively() =
        runTest(main.testDispatcher) {
            val row = expertBookingItem(id = "u1").let { it.copy(client = ExpertBookingClientDto(id = "abc-1")) }
            stub(upcoming = listOf(row))
            val vm = vm(clientId = "ABC-1")
            runCurrent()

            assertEquals(listOf("u1"), vm.loaded().history.map { it.item.bookingId })
        }

    @Test
    fun pendingApprovalAndCompletedRowsAreKept() =
        runTest(main.testDispatcher) {
            stub(
                upcoming = listOf(expertBookingItem(id = "a", statusCode = "pending_approval", start = at(25))),
                past = listOf(expertBookingItem(id = "b", statusCode = "completed", start = at(5))),
            )
            val vm = vm()
            runCurrent()

            val history = vm.loaded().history
            assertEquals(listOf("a", "b"), history.map { it.item.bookingId })
            assertEquals(
                listOf(ClientHistoryBucket.Upcoming, ClientHistoryBucket.Completed),
                history.map { it.bucket },
            )
        }

    @Test
    fun anEmptyHistoryIsStillLoaded() =
        runTest(main.testDispatcher) {
            stub()
            val vm = vm()
            runCurrent()

            assertTrue(vm.loaded().history.isEmpty())
        }

    @Test
    fun profileIsFetchedBeforeBookings() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ExpertClientProfileData>()
            stub()
            api.getExpertClientHandler = { gate.await() }
            val vm = vm()
            runCurrent()

            assertEquals(1, api.getExpertClientCalls)
            assertEquals(0, api.expertBookingsCalls)
            assertEquals(ClientProfileLoad.Loading, vm.uiState.value.load)

            gate.complete(expertClientProfile())
            runCurrent()
            assertEquals(2, api.expertBookingsCalls)
            assertTrue(vm.uiState.value.load is ClientProfileLoad.Loaded)
        }

    private fun httpError(
        status: Int,
        code: String,
        message: String = "Nope",
    ) = api.httpError(status, """{"status":"error","code":"$code","message":"$message"}""")

    private fun noRelationship() = httpError(403, "NO_RELATIONSHIP")

    @Test
    fun noRelationshipGivesItsOwnStateAndNoBookingsCall() =
        runTest(main.testDispatcher) {
            stub()
            api.getExpertClientHandler = { throw noRelationship() }
            val vm = vm()
            runCurrent()

            assertEquals(ClientProfileLoad.NoRelationship, vm.uiState.value.load)
            assertEquals(0, api.expertBookingsCalls)
        }

    @Test
    fun notFoundGivesUnavailableAndNoBookingsCall() =
        runTest(main.testDispatcher) {
            stub()
            api.getExpertClientHandler = { throw httpError(404, "NOT_FOUND") }
            val vm = vm()
            runCurrent()

            assertEquals(ClientProfileLoad.Unavailable, vm.uiState.value.load)
            assertEquals(0, api.expertBookingsCalls)
        }

    @Test
    fun otherProfileFailureGivesFailed() =
        runTest(main.testDispatcher) {
            stub()
            api.getExpertClientHandler = { throw httpError(500, "INTERNAL", "Boom") }
            val vm = vm()
            runCurrent()
            assertEquals(ClientProfileLoad.Failed(FieldError.Text("Boom")), vm.uiState.value.load)

            api.getExpertClientHandler = { throw IOException("offline") }
            vm.retry()
            runCurrent()
            assertEquals(
                ClientProfileLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
        }

    @Test
    fun aForbiddenWithAnotherCodeIsAGenericFailure() =
        runTest(main.testDispatcher) {
            stub()
            api.getExpertClientHandler = { throw httpError(403, "FORBIDDEN", "Forbidden") }
            val vm = vm()
            runCurrent()

            assertEquals(ClientProfileLoad.Failed(FieldError.Text("Forbidden")), vm.uiState.value.load)
        }

    @Test
    fun eitherBookingsFailureFailsTheWholeScreen() =
        runTest(main.testDispatcher) {
            for (failing in listOf(ExpertBookingsRepository.TYPE_PAST, ExpertBookingsRepository.TYPE_UPCOMING)) {
                stub(upcoming = listOf(expertBookingItem(id = "u1")))
                api.expertBookingsHandler = { type ->
                    if (type == failing) throw httpError(500, "INTERNAL", "Down") else ExpertBookingsPayload()
                }
                val vm = vm()
                runCurrent()

                assertEquals(failing, ClientProfileLoad.Failed(FieldError.Text("Down")), vm.uiState.value.load)
            }
        }

    @Test
    fun aBookingsNotFoundIsAGenericFailure() =
        runTest(main.testDispatcher) {
            stub()
            api.expertBookingsHandler = { throw httpError(404, "NOT_FOUND", "Gone") }
            val vm = vm()
            runCurrent()

            assertEquals(ClientProfileLoad.Failed(FieldError.Text("Gone")), vm.uiState.value.load)
        }

    @Test
    fun retryAfterFailedLoadsAgain() =
        runTest(main.testDispatcher) {
            stub()
            api.getExpertClientHandler = { throw httpError(500, "INTERNAL") }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is ClientProfileLoad.Failed)

            val gate = CompletableDeferred<ExpertClientProfileData>()
            api.getExpertClientHandler = { gate.await() }
            vm.retry()
            assertEquals(ClientProfileLoad.Loading, vm.uiState.value.load)
            gate.complete(expertClientProfile())
            runCurrent()

            assertTrue(vm.uiState.value.load is ClientProfileLoad.Loaded)
        }

    @Test
    fun pullOverLoadedKeepsContentAndRefreshes() =
        runTest(main.testDispatcher) {
            stub(upcoming = listOf(expertBookingItem(id = "u1")))
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value.load

            val gate = CompletableDeferred<ExpertClientProfileData>()
            stub(upcoming = listOf(expertBookingItem(id = "u1"), expertBookingItem(id = "u2", start = at(30))))
            api.getExpertClientHandler = { gate.await() }
            vm.refresh()

            assertEquals(before, vm.uiState.value.load)
            assertTrue(vm.uiState.value.isRefreshing)
            gate.complete(expertClientProfile())
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf("u2", "u1"), vm.loaded().history.map { it.item.bookingId })
        }

    @Test
    fun pullFailureOverLoadedKeepsContentAndFlags() =
        runTest(main.testDispatcher) {
            stub(upcoming = listOf(expertBookingItem(id = "u1")))
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value.load

            api.getExpertClientHandler = { throw httpError(500, "INTERNAL") }
            vm.refresh()
            runCurrent()

            assertEquals(before, vm.uiState.value.load)
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)

            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun aBookingsFailureOnAPullKeepsContentAndFlags() =
        runTest(main.testDispatcher) {
            stub(upcoming = listOf(expertBookingItem(id = "u1")))
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value.load

            api.expertBookingsHandler = { throw httpError(500, "INTERNAL") }
            vm.refresh()
            runCurrent()

            assertEquals(before, vm.uiState.value.load)
            assertTrue(vm.uiState.value.refreshFailed)
        }

    @Test
    fun pullFromNoRelationshipOrUnavailableReruns() =
        runTest(main.testDispatcher) {
            stub()
            for (failure in listOf(noRelationship(), httpError(404, "NOT_FOUND"))) {
                api.getExpertClientHandler = { throw failure }
                val vm = vm()
                runCurrent()
                val gate = CompletableDeferred<ExpertClientProfileData>()
                api.getExpertClientHandler = { gate.await() }

                vm.refresh()
                assertFalse(vm.uiState.value.isRefreshing)
                assertEquals(ClientProfileLoad.Loading, vm.uiState.value.load)
                gate.complete(expertClientProfile())
                runCurrent()

                assertTrue(vm.uiState.value.load is ClientProfileLoad.Loaded)
            }
        }

    @Test
    fun pullOverLoadedThatNowAnswersNotFoundBecomesUnavailable() =
        runTest(main.testDispatcher) {
            stub()
            val vm = vm()
            runCurrent()

            api.getExpertClientHandler = { throw httpError(404, "NOT_FOUND") }
            vm.refresh()
            runCurrent()

            assertEquals(ClientProfileLoad.Unavailable, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun pullOverLoadedThatNowAnswersNoRelationshipBecomesNoRelationship() =
        runTest(main.testDispatcher) {
            stub()
            val vm = vm()
            runCurrent()

            api.getExpertClientHandler = { throw noRelationship() }
            vm.refresh()
            runCurrent()

            assertEquals(ClientProfileLoad.NoRelationship, vm.uiState.value.load)
            assertFalse(vm.uiState.value.isRefreshing)
        }

    @Test
    fun aNewerLoadWinsOverAnOlderOne() =
        runTest(main.testDispatcher) {
            stub()
            val old = CompletableDeferred<ExpertClientProfileData>()
            api.getExpertClientHandler = { old.await() }
            val vm = vm()
            runCurrent()

            val fresh = expertClientProfile(id = "fresh")
            api.getExpertClientHandler = { fresh }
            vm.refresh()
            runCurrent()
            old.complete(expertClientProfile(id = "stale"))
            runCurrent()

            assertEquals("fresh", vm.loaded().profile.id)
        }
}
