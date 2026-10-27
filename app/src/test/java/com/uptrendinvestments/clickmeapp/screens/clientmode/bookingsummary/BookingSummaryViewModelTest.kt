package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.rescheduleDto
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class BookingSummaryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse("2026-10-23T12:00:00Z"))
    private val now = clock.instant()

    private fun vm() =
        BookingSummaryViewModel(
            BookingSummaryKey("b1"),
            ClientBookingsRepository(api),
            RescheduleResponder(ClientBookingsRepository(api), PaymentMethodsRepository(api)),
            clock,
        )

    // ---- display model ----

    @Test
    fun declinedStatusShowsTheCancelledBadge() {
        val details = bookingDetail(status = "declined").toSummaryDetails(now)
        assertEquals(BookingStatusLabel.Cancelled, details.status.label)
        assertNull(details.reschedule)
    }

    @Test
    fun pendingRescheduleShowsNewTimeProposedWithTheProposal() {
        val details =
            bookingDetail(status = "pending_reschedule", reschedule = rescheduleDto()).toSummaryDetails(now)
        assertEquals(BookingStatusLabel.NewTimeProposed, details.status.label)
        assertNotNull(details.reschedule)
    }

    @Test
    fun completedStatusShowsTheCompletedBadge() {
        assertEquals(
            BookingStatusLabel.Completed,
            bookingDetail(status = "completed").toSummaryDetails(now).status.label,
        )
    }

    @Test
    fun priceLabelFollowsThePaymentStatus() {
        fun label(paymentStatus: String?) =
            bookingDetail(status = "completed", paymentStatus = paymentStatus).toSummaryDetails(now).priceLabel
        assertEquals(SummaryPriceLabel.PricePaid, label("captured"))
        assertEquals(SummaryPriceLabel.Refunded, label("refunded"))
        assertEquals(SummaryPriceLabel.SessionFee, label("held"))
        assertEquals(SummaryPriceLabel.SessionFee, label("none"))
        assertEquals(SummaryPriceLabel.SessionFee, label(null))
    }

    @Test
    fun freeTopicShowsTheSessionFeeLabelWithAFreePrice() {
        val details =
            bookingDetail(status = "completed", isFree = true, paymentStatus = "captured").toSummaryDetails(now)
        assertEquals(SummaryPriceLabel.SessionFee, details.priceLabel)
        assertEquals(PriceDisplay.Free, details.price)
    }

    @Test
    fun paidTopicCarriesAFormattedPrice() {
        val details = bookingDetail(status = "completed", paymentStatus = "captured").toSummaryDetails(now)
        assertTrue(details.price is PriceDisplay.Paid)
    }

    @Test
    fun bookAgainSeedComesFromTheExpertAndIsNullWithoutAnId() {
        val details = bookingDetail(status = "completed").toSummaryDetails(now)
        assertEquals(BookAgainSeed("e1", "Ada Lovelace", "Engineer", null), details.bookAgain)
        assertNull(bookingDetail(status = "completed", expertId = null).toSummaryDetails(now).bookAgain)
    }

    @Test
    fun detailsCarryExpertId() {
        assertEquals("e1", bookingDetail(status = "completed").toSummaryDetails(now).expertId)
        assertNull(bookingDetail(status = "completed", expertId = null).toSummaryDetails(now).expertId)
        assertNull(bookingDetail(status = "completed", expertId = " ").toSummaryDetails(now).expertId)
    }

    @Test
    fun blankStringsBecomeNullAndTheEndFallsBackToStartPlusDuration() {
        val details =
            bookingDetail(status = "completed", expertName = "  ", expertTitle = "", end = null).toSummaryDetails(now)
        assertNull(details.expertName)
        assertNull(details.expertTitle)
        assertEquals(details.start.plusSeconds(3600), details.end)
    }

    @Test
    fun bookAgainSeedBuildsABookingKeyWithFallbacks() {
        assertEquals(
            BookingKey("e1", "Expert", "", null),
            BookAgainSeed("e1", null, null, null).toBookingKey("Expert"),
        )
        assertEquals(
            BookingKey("e1", "Ada", "Engineer", "https://x/a.jpg"),
            BookAgainSeed("e1", "Ada", "Engineer", "https://x/a.jpg").toBookingKey("Expert"),
        )
    }

    // ---- ViewModel ----

    @Test
    fun loadsTheBookingByIdOnceThenShowsLoaded() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ClientBookingDetail>()
            val ids = mutableListOf<String>()
            api.clientBookingDetailHandler = {
                ids += it
                gate.await()
            }
            val vm = vm()
            runCurrent()
            assertEquals(BookingSummaryUiState.Loading, vm.uiState.value)
            gate.complete(bookingDetail(status = "completed"))
            runCurrent()
            assertEquals(listOf("b1"), ids)
            assertEquals(
                BookingSummaryUiState.Loaded(bookingDetail(status = "completed").toSummaryDetails(now)),
                vm.uiState.value,
            )
        }

    @Test
    fun networkErrorFailsWithTheConnectionMessageAndRetryReloads() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.clientBookingDetailHandler = {
                calls++
                if (calls == 1) throw IOException("offline") else bookingDetail(status = "completed")
            }
            val vm = vm()
            runCurrent()
            assertEquals(
                BookingSummaryUiState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value,
            )
            vm.retry()
            assertEquals(BookingSummaryUiState.Loading, vm.uiState.value)
            runCurrent()
            assertTrue(vm.uiState.value is BookingSummaryUiState.Loaded)
            assertEquals(2, api.clientBookingDetailCalls)
        }

    @Test
    fun refreshQuietlyWhileTheInitialLoadRunsAddsNoCall() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { gate.await() }
            val vm = vm()
            runCurrent()
            vm.refreshQuietly()
            runCurrent()
            assertEquals(1, api.clientBookingDetailCalls)
            gate.complete(bookingDetail(status = "completed"))
            runCurrent()
            assertTrue(vm.uiState.value is BookingSummaryUiState.Loaded)
        }

    @Test
    fun refreshQuietlyOnLoadedAddsOneCallWithoutLoading() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail(status = "completed") }
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { gate.await() }
            vm.refreshQuietly()
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)
            assertTrue(vm.uiState.value is BookingSummaryUiState.Loaded)
            gate.complete(bookingDetail(status = "completed", expertName = "Grace Hopper"))
            runCurrent()
            val loaded = vm.uiState.value as BookingSummaryUiState.Loaded
            assertEquals("Grace Hopper", loaded.details.expertName)
        }

    @Test
    fun aFailedQuietReloadKeepsTheLoadedSummary() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail(status = "completed") }
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value
            api.clientBookingDetailHandler = { throw IOException("offline") }
            vm.refreshQuietly()
            runCurrent()
            assertEquals(before, vm.uiState.value)
        }

    @Test
    fun onScreenHiddenLetsTheNextRefreshRestartAnInFlightLoad() =
        runTest(main.testDispatcher) {
            val first = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { first.await() }
            val vm = vm()
            runCurrent()
            vm.onScreenHidden()
            vm.refreshQuietly()
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)
        }

    @Test
    fun acceptRecordsOneRequestReloadsThenSetsTheAcceptedNotice() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = {
                bookingDetail(status = "pending_reschedule", reschedule = rescheduleDto())
            }
            val accepted = RescheduleResponseData(bookingId = "b1", status = "confirmed")
            api.acceptClientRescheduleHandler = { _, _ -> accepted }
            val vm = vm()
            runCurrent()
            api.clientBookingDetailHandler = { bookingDetail(status = "confirmed") }
            vm.accept()
            runCurrent()
            assertEquals(1, api.acceptClientRescheduleRequests.size)
            assertEquals(2, api.clientBookingDetailCalls)
            assertEquals(RescheduleNotice.Accepted, vm.rescheduleState.value.notice)
            val loaded = vm.uiState.value as BookingSummaryUiState.Loaded
            assertNull(loaded.details.reschedule)
            vm.consumeRescheduleNotice()
            assertNull(vm.rescheduleState.value.notice)
        }
}
