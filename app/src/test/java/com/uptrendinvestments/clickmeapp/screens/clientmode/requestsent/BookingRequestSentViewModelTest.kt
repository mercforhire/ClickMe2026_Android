package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailTopicDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BookingRequestSentViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm() = BookingRequestSentViewModel(RequestSentKey("b1"), ClientBookingsRepository(api))

    private val start = Instant.parse("2026-10-24T18:00:00Z")

    private fun detail(
        endTime: Instant? = Instant.parse("2026-10-24T19:00:00Z"),
        title: String = "Intro",
        durationMins: Int? = null,
        isFree: Boolean = true,
        fullName: String? = "Ada Lovelace",
        avatarUrl: String? = "https://x/a.jpg",
    ) = ClientBookingDetail(
        bookingId = "b1",
        status = "pending",
        startTime = start,
        endTime = endTime,
        topic = BookingDetailTopicDto(title = title, durationMins = durationMins, isFree = isFree),
        expert = BookingDetailExpertDto(fullName = fullName, avatarUrl = avatarUrl),
    )

    @Test
    fun mapsAllFieldsFromTheBooking() {
        assertEquals(
            BookingRequestSentDetails(
                expertName = "Ada Lovelace",
                expertAvatarUrl = "https://x/a.jpg",
                topicTitle = "Intro",
                start = start,
                end = Instant.parse("2026-10-24T19:00:00Z"),
                isFree = true,
            ),
            detail().toRequestSentDetails(),
        )
    }

    @Test
    fun missingEndTimeFallsBackToStartPlusTopicDuration() {
        val mapped = detail(endTime = null, durationMins = 60).toRequestSentDetails()
        assertEquals(Instant.parse("2026-10-24T19:00:00Z"), mapped.end)
    }

    @Test
    fun missingEndTimeAndDurationFallsBackToThirtyMinutes() {
        val mapped = detail(endTime = null, durationMins = null).toRequestSentDetails()
        assertEquals(Instant.parse("2026-10-24T18:30:00Z"), mapped.end)
    }

    @Test
    fun blankOrNullExpertNameMapsToNull() {
        assertEquals(null, detail(fullName = "   ").toRequestSentDetails().expertName)
        assertEquals(null, detail(fullName = null).toRequestSentDetails().expertName)
    }

    @Test
    fun paidTopicIsNotFree() {
        assertEquals(false, detail(isFree = false).toRequestSentDetails().isFree)
    }

    @Test
    fun loadsTheBookingByIdOnceAndShowsLoadingUntilItReturns() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ClientBookingDetail>()
            val ids = mutableListOf<String>()
            api.clientBookingDetailHandler = {
                ids += it
                gate.await()
            }
            val vm = vm()
            runCurrent()
            assertEquals(BookingRequestSentUiState.Loading, vm.uiState.value)
            assertEquals(listOf("b1"), ids)
            assertEquals(1, api.clientBookingDetailCalls)
        }

    @Test
    fun successEndsLoadedWithMappedDetails() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { detail() }
            val vm = vm()
            runCurrent()
            assertEquals(BookingRequestSentUiState.Loaded(detail().toRequestSentDetails()), vm.uiState.value)
        }

    @Test
    fun networkErrorEndsFailed() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()
            assertEquals(BookingRequestSentUiState.Failed, vm.uiState.value)
        }

    @Test
    fun notFoundEndsFailed() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"Booking not found"}""")
            }
            val vm = vm()
            runCurrent()
            assertEquals(BookingRequestSentUiState.Failed, vm.uiState.value)
        }

    @Test
    fun retryAfterFailureLoadsAgain() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.clientBookingDetailHandler = {
                calls++
                if (calls == 1) throw IOException("offline") else detail()
            }
            val vm = vm()
            runCurrent()
            assertEquals(BookingRequestSentUiState.Failed, vm.uiState.value)
            vm.retry()
            assertEquals(BookingRequestSentUiState.Loading, vm.uiState.value)
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)
            assertEquals(BookingRequestSentUiState.Loaded(detail().toRequestSentDetails()), vm.uiState.value)
        }

    @Test
    fun retryWhileLoadingCancelsTheFirstCall() =
        runTest(main.testDispatcher) {
            val first = CompletableDeferred<ClientBookingDetail>()
            val second = CompletableDeferred<ClientBookingDetail>()
            var calls = 0
            api.clientBookingDetailHandler = { if (++calls == 1) first.await() else second.await() }
            val vm = vm()
            runCurrent()
            vm.retry()
            runCurrent()
            first.complete(detail(title = "Stale"))
            runCurrent()
            assertEquals(BookingRequestSentUiState.Loading, vm.uiState.value)
            second.complete(detail(title = "Fresh"))
            runCurrent()
            assertEquals(
                BookingRequestSentUiState.Loaded(detail(title = "Fresh").toRequestSentDetails()),
                vm.uiState.value,
            )
        }
}
