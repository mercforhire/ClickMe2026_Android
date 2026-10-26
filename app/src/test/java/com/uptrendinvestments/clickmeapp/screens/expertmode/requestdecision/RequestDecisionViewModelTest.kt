package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.bookingRequestDetail
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
class RequestDecisionViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun vm(key: RequestDecisionKey) = RequestDecisionViewModel(key, BookingRequestsRepository(api))

    private fun RequestDecisionViewModel.loaded(): RequestDecisionDetails =
        (uiState.value.load as RequestDecisionLoad.Loaded).details

    @Test
    fun loadsTheKeyedRequest() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingRequestDetail>()
            val ids = mutableListOf<String>()
            api.bookingRequestDetailHandler = {
                ids += it
                gate.await()
            }

            val vm = vm(RequestDecisionKey("r1"))
            runCurrent()
            assertEquals(RequestDecisionLoad.Loading, vm.uiState.value.load)
            assertEquals(listOf("r1"), ids)

            gate.complete(bookingRequestDetail(id = "r1", message = "  Hello  ", topic = "Algorithms"))
            runCurrent()

            val details = vm.loaded()
            assertEquals("r1", details.requestId)
            assertEquals("Ada Lovelace", details.clientName)
            assertEquals("Algorithms", details.topic)
            assertEquals("Hello", details.clientMessage)
            assertEquals(Instant.parse("2026-10-24T18:00:00Z"), details.start)
            assertEquals(Instant.parse("2026-10-24T19:00:00Z"), details.end)
            assertEquals(1, api.bookingRequestDetailCalls)
        }

    @Test
    fun blankMessageAndTopicMapToNull() =
        runTest(main.testDispatcher) {
            api.bookingRequestDetailHandler = { bookingRequestDetail(id = it, message = "   ", topic = "  ") }

            val vm = vm(RequestDecisionKey("r1"))
            runCurrent()

            assertNull(vm.loaded().clientMessage)
            assertNull(vm.loaded().topic)
        }

    @Test
    fun expiredComesFromTheKey() =
        runTest(main.testDispatcher) {
            api.bookingRequestDetailHandler = { bookingRequestDetail(id = it) }

            val expired = vm(RequestDecisionKey("r1", isExpired = true))
            val pending = vm(RequestDecisionKey("r2"))
            assertTrue(expired.uiState.value.isExpired)
            assertFalse(pending.uiState.value.isExpired)
            runCurrent()

            assertTrue(expired.uiState.value.isExpired)
            assertFalse(pending.uiState.value.isExpired)
        }

    @Test
    fun listEarningsFeedPricePending() =
        runTest(main.testDispatcher) {
            api.bookingRequestDetailHandler = { bookingRequestDetail(id = it, breakdown = null) }

            val vm = vm(RequestDecisionKey("r1", listEarningsMajor = 40.0, listCurrency = "CAD"))
            runCurrent()

            val earnings = vm.loaded().earnings
            assertTrue("expected PricePending but was $earnings", earnings is RequestEarningsUi.PricePending)
        }

    @Test
    fun breakdownWinsOverTheListEarnings() =
        runTest(main.testDispatcher) {
            api.bookingRequestDetailHandler = { bookingRequestDetail(id = it) }

            val vm = vm(RequestDecisionKey("r1", listEarningsMajor = 40.0, listCurrency = "CAD"))
            runCurrent()

            assertTrue(vm.loaded().earnings is RequestEarningsUi.Receipt)
        }

    @Test
    fun failureThenRetryLoads() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.bookingRequestDetailHandler = {
                calls++
                if (calls == 1) throw IOException("offline")
                bookingRequestDetail(id = it)
            }

            val vm = vm(RequestDecisionKey("r1"))
            runCurrent()
            val failed = vm.uiState.value.load as RequestDecisionLoad.Failed
            assertEquals(FieldError.Res(R.string.login_error_network), failed.message)

            vm.retry()
            runCurrent()

            assertTrue(vm.uiState.value.load is RequestDecisionLoad.Loaded)
            assertEquals(2, calls)
        }

    @Test
    fun retryWhileLoadingKeepsOnlyTheLatestResult() =
        runTest(main.testDispatcher) {
            val first = CompletableDeferred<BookingRequestDetail>()
            val second = CompletableDeferred<BookingRequestDetail>()
            val gates = ArrayDeque(listOf(first, second))
            api.bookingRequestDetailHandler = { gates.removeFirst().await() }

            val vm = vm(RequestDecisionKey("r1"))
            runCurrent()
            vm.retry()
            runCurrent()
            first.complete(bookingRequestDetail(id = "r1", clientName = "Stale"))
            second.complete(bookingRequestDetail(id = "r1", clientName = "Fresh"))
            runCurrent()

            assertEquals("Fresh", vm.loaded().clientName)
        }
}
