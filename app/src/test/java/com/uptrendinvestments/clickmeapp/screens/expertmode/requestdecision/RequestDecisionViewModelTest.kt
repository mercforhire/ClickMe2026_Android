package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.AcceptBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.DeclineBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.AcceptBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.model.responses.DeclineBookingRequestData
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsNotice
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
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

    private val launch = ExpertLaunchRequest()

    private fun vm(key: RequestDecisionKey) = RequestDecisionViewModel(key, BookingRequestsRepository(api), launch)

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

    private fun loadedVm(
        key: RequestDecisionKey = RequestDecisionKey("r1"),
        detail: BookingRequestDetail = bookingRequestDetail(id = key.requestId),
    ): RequestDecisionViewModel {
        api.bookingRequestDetailHandler = { detail }
        val vm = vm(key)
        main.testDispatcher.scheduler.runCurrent()
        return vm
    }

    @Test
    fun acceptWithoutMessageSendsNull() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ -> AcceptBookingRequestData(bookingId = "b9") }
            val vm = loadedVm()

            vm.accept("   ")
            runCurrent()

            assertEquals(listOf("r1" to AcceptBookingRequestBody(null)), api.acceptBookingRequestRequests)
        }

    @Test
    fun acceptTrimsMessage() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ -> AcceptBookingRequestData(bookingId = "b9") }
            val vm = loadedVm()

            vm.accept("  See you  ")
            runCurrent()

            assertEquals(listOf("r1" to AcceptBookingRequestBody("See you")), api.acceptBookingRequestRequests)
        }

    @Test
    fun acceptSuccessEmitsConfirmedKey() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ -> AcceptBookingRequestData(bookingId = "b9") }
            val vm =
                loadedVm(
                    detail =
                        bookingRequestDetail(
                            id = "r1",
                            topic = "Algorithms",
                            meetingType = "skype_zoom",
                            message = "Looking forward",
                        ),
                )

            vm.accept("")
            runCurrent()

            val start = Instant.parse("2026-10-24T18:00:00Z").toEpochMilli()
            val end = Instant.parse("2026-10-24T19:00:00Z").toEpochMilli()
            val expected =
                BookingConfirmedKey(
                    bookingId = "b9",
                    clientName = "Ada Lovelace",
                    clientAvatarUrl = null,
                    topic = "Algorithms",
                    startMillis = start,
                    endMillis = end,
                    meetingType = "skype_zoom",
                    clientMessage = "Looking forward",
                )
            assertEquals(RequestDecisionEvent.Accepted(expected), vm.uiState.value.event)
            assertEquals(IncomingRequestsLaunch(), launch.incomingRequests.pending.value)
        }

    @Test
    fun acceptIsSingleFlight() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<AcceptBookingRequestData>()
            api.acceptBookingRequestHandler = { _, _ -> gate.await() }
            val vm = loadedVm()

            vm.accept("one")
            vm.accept("two")
            runCurrent()

            assertEquals(1, api.acceptBookingRequestCalls)
            assertEquals(DecisionSubmitting.Accepting, vm.uiState.value.submitting)

            gate.complete(AcceptBookingRequestData(bookingId = "b9"))
            runCurrent()

            assertEquals(DecisionSubmitting.Accepting, vm.uiState.value.submitting)
            assertTrue(vm.uiState.value.event is RequestDecisionEvent.Accepted)
        }

    @Test
    fun expiredRequestCannotAccept() =
        runTest(main.testDispatcher) {
            val vm = loadedVm(RequestDecisionKey("r1", isExpired = true))

            vm.accept("hello")
            runCurrent()

            assertEquals(0, api.acceptBookingRequestCalls)
            assertEquals(DecisionSubmitting.None, vm.uiState.value.submitting)
        }

    @Test
    fun acceptBeforeTheRequestLoadsSendsNothing() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<BookingRequestDetail>()
            api.bookingRequestDetailHandler = { gate.await() }
            val vm = vm(RequestDecisionKey("r1"))
            runCurrent()

            vm.accept("hello")
            runCurrent()

            assertEquals(0, api.acceptBookingRequestCalls)
            assertEquals(DecisionSubmitting.None, vm.uiState.value.submitting)
        }

    @Test
    fun declineSendsCodeAndFallbackText() =
        runTest(main.testDispatcher) {
            api.declineBookingRequestHandler = { id, _ -> DeclineBookingRequestData(requestId = id) }
            val vm = loadedVm()

            vm.decline(DeclineReason.OutsideExpertise, "")
            runCurrent()

            assertEquals(
                listOf("r1" to DeclineBookingRequestBody("OUT_OF_SCOPE", "Outside my expertise")),
                api.declineBookingRequestRequests,
            )
            assertEquals(
                IncomingRequestsLaunch(IncomingRequestsNotice.Declined),
                launch.incomingRequests.pending.value,
            )
            assertEquals(RequestDecisionEvent.Declined, vm.uiState.value.event)
        }

    @Test
    fun declineSendsTheTrimmedNote() =
        runTest(main.testDispatcher) {
            api.declineBookingRequestHandler = { id, _ -> DeclineBookingRequestData(requestId = id) }
            val vm = loadedVm()

            vm.decline(DeclineReason.Other, "  Booked solid  ")
            runCurrent()

            assertEquals(
                listOf("r1" to DeclineBookingRequestBody("OTHER", "Booked solid")),
                api.declineBookingRequestRequests,
            )
        }

    @Test
    fun declineIsSingleFlightAndBlocksAccept() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<DeclineBookingRequestData>()
            api.declineBookingRequestHandler = { _, _ -> gate.await() }
            val vm = loadedVm()

            vm.decline(DeclineReason.Other, "")
            vm.decline(DeclineReason.Other, "again")
            vm.accept("hello")
            runCurrent()

            assertEquals(1, api.declineBookingRequestCalls)
            assertEquals(0, api.acceptBookingRequestCalls)
            assertEquals(DecisionSubmitting.Declining, vm.uiState.value.submitting)
        }

    @Test
    fun expiredRequestCannotDecline() =
        runTest(main.testDispatcher) {
            val vm = loadedVm(RequestDecisionKey("r1", isExpired = true))

            vm.decline(DeclineReason.Other, "")
            runCurrent()

            assertEquals(0, api.declineBookingRequestCalls)
        }

    private fun failure(
        status: Int,
        code: String,
        message: String?,
    ): Throwable {
        val text = message?.let { ",\"message\":\"$it\"" }.orEmpty()
        return api.httpError(status, "{\"status\":\"error\",\"code\":\"$code\"$text}")
    }

    @Test
    fun alreadyProcessedShowsDialogThenExits() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ ->
                throw failure(409, "ALREADY_PROCESSED", "This request has already been handled.")
            }
            val vm = loadedVm()

            vm.accept("hi")
            runCurrent()

            assertEquals(
                RequestDecisionDialog(FieldError.Text("This request has already been handled."), popOnDismiss = true),
                vm.uiState.value.dialog,
            )
            assertNull(vm.uiState.value.event)
            assertEquals(DecisionSubmitting.None, vm.uiState.value.submitting)
            assertNull(launch.incomingRequests.pending.value)

            vm.dismissDialog()

            assertNull(vm.uiState.value.dialog)
            assertEquals(RequestDecisionEvent.ExitAfterProcessed, vm.uiState.value.event)
            assertEquals(IncomingRequestsLaunch(), launch.incomingRequests.pending.value)
        }

    @Test
    fun alreadyProcessedOnDeclineIsNeverReportedAsDeclined() =
        runTest(main.testDispatcher) {
            api.declineBookingRequestHandler = { _, _ -> throw failure(409, "ALREADY_PROCESSED", "Handled.") }
            val vm = loadedVm()

            vm.decline(DeclineReason.Other, "")
            runCurrent()
            vm.dismissDialog()

            assertEquals(RequestDecisionEvent.ExitAfterProcessed, vm.uiState.value.event)
            // A reload without the "Request declined" notice.
            assertEquals(IncomingRequestsLaunch(null), launch.incomingRequests.pending.value)
        }

    @Test
    fun otherErrorStays() =
        runTest(main.testDispatcher) {
            api.declineBookingRequestHandler = { _, _ -> throw failure(500, "INTERNAL_ERROR", "Boom") }
            val vm = loadedVm()

            vm.decline(DeclineReason.Other, "note")
            runCurrent()

            assertEquals(RequestDecisionDialog(FieldError.Text("Boom"), popOnDismiss = false), vm.uiState.value.dialog)
            assertEquals(DecisionSubmitting.None, vm.uiState.value.submitting)

            vm.dismissDialog()

            assertNull(vm.uiState.value.dialog)
            assertNull(vm.uiState.value.event)
            assertNull(launch.incomingRequests.pending.value)
        }

    @Test
    fun errorWithoutServerTextUsesTheGenericCopy() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ -> throw failure(500, "INTERNAL_ERROR", null) }
            val vm = loadedVm()

            vm.accept("")
            runCurrent()

            assertEquals(
                RequestDecisionDialog(FieldError.Res(R.string.login_error_unknown), popOnDismiss = false),
                vm.uiState.value.dialog,
            )
        }

    @Test
    fun networkErrorShowsTheConnectionCopy() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ -> throw IOException("offline") }
            val vm = loadedVm()

            vm.accept("")
            runCurrent()

            assertEquals(
                RequestDecisionDialog(FieldError.Res(R.string.login_error_network), popOnDismiss = false),
                vm.uiState.value.dialog,
            )
            assertEquals(DecisionSubmitting.None, vm.uiState.value.submitting)
        }

    @Test
    fun accountSuspendedShowsNothing() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ ->
                throw failure(403, "ACCOUNT_SUSPENDED", "Your account is suspended.")
            }
            val vm = loadedVm()

            vm.accept("")
            runCurrent()

            assertNull(vm.uiState.value.dialog)
            assertEquals(DecisionSubmitting.None, vm.uiState.value.submitting)
            assertNull(vm.uiState.value.event)
        }

    @Test
    fun aFailedAcceptCanBeRetried() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.acceptBookingRequestHandler = { _, _ ->
                calls++
                if (calls == 1) throw failure(500, "INTERNAL_ERROR", "Boom")
                AcceptBookingRequestData(bookingId = "b9")
            }
            val vm = loadedVm()

            vm.accept("hi")
            runCurrent()
            vm.dismissDialog()
            vm.accept("hi")
            runCurrent()

            assertEquals(2, api.acceptBookingRequestCalls)
            assertTrue(vm.uiState.value.event is RequestDecisionEvent.Accepted)
        }

    @Test
    fun dismissingWithoutADialogDoesNothing() =
        runTest(main.testDispatcher) {
            val vm = loadedVm()

            vm.dismissDialog()

            assertNull(vm.uiState.value.event)
            assertNull(launch.incomingRequests.pending.value)
        }

    @Test
    fun consumeEventClearsTheEvent() =
        runTest(main.testDispatcher) {
            api.acceptBookingRequestHandler = { _, _ -> AcceptBookingRequestData(bookingId = "b9") }
            val vm = loadedVm()
            vm.accept("")
            runCurrent()

            vm.consumeEvent()

            assertNull(vm.uiState.value.event)
        }
}
