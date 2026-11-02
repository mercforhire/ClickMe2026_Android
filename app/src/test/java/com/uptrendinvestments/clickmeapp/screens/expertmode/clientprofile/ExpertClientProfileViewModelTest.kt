@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingClientDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.testing.expertClientProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
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
}
