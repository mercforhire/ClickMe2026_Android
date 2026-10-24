package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.rescheduleDto
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingBookingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val clock = MutableClock(Instant.parse("2026-10-22T16:00:00Z"))
    private val now = clock.instant

    private fun vm(): UpcomingBookingViewModel {
        val bookings = ClientBookingsRepository(api)
        return UpcomingBookingViewModel(
            UpcomingBookingKey("b1"),
            bookings,
            RescheduleResponder(bookings, PaymentMethodsRepository(api)),
            realtime,
            clock,
        )
    }

    private fun loaded(vm: UpcomingBookingViewModel) = vm.uiState.value as UpcomingBookingUiState.Loaded

    // ---- mapping ----

    @Test
    fun shortBookingId_uses_the_first_eight_characters_upper_cased() {
        assertEquals("#A1B2C3D4", shortBookingId("a1b2c3d4-e5f6-7890"))
        assertEquals("#B1", shortBookingId("b1"))
    }

    @Test
    fun pending_reschedule_maps_the_proposal_and_allows_cancel() {
        val details =
            bookingDetail(
                status = "pending_reschedule",
                reschedule = rescheduleDto(initiatedBy = "expert"),
            ).toUpcomingDetails(now)

        assertEquals(BookingStatusLabel.NewTimeProposed, details.status.label)
        assertTrue(details.canCancel)
        assertFalse(details.showsPendingBanner)
        assertNotNull(details.reschedule)
        assertTrue(details.reschedule!!.initiatedByExpert)
    }

    @Test
    fun pending_approval_shows_the_banner_and_allows_cancel() {
        val details = bookingDetail(status = "pending_approval").toUpcomingDetails(now)

        assertTrue(details.showsPendingBanner)
        assertTrue(details.canCancel)
        assertEquals(BookingStatusLabel.PendingApproval, details.status.label)
    }

    @Test
    fun confirmed_allows_cancel_and_has_no_proposal() {
        val details = bookingDetail(status = "confirmed").toUpcomingDetails(now)

        assertTrue(details.canCancel)
        assertFalse(details.showsPendingBanner)
        assertNull(details.reschedule)
        assertEquals(BookingStatusLabel.Confirmed, details.status.label)
    }

    @Test
    fun in_progress_and_completed_cannot_be_cancelled() {
        assertFalse(bookingDetail(status = "in_progress").toUpcomingDetails(now).canCancel)
        assertFalse(bookingDetail(status = "completed").toUpcomingDetails(now).canCancel)
    }

    @Test
    fun paid_held_booking_shows_money_and_the_hold_pill() {
        val details =
            bookingDetail(isFree = false, amount = 5000, currency = "CAD", paymentStatus = "held")
                .toUpcomingDetails(now)

        assertEquals(PriceDisplay.of(false, 5000, "CAD"), details.price)
        assertEquals(PaymentPill.HoldPlaced, details.paymentPill)
    }

    @Test
    fun free_booking_shows_free_and_no_pill() {
        val details = bookingDetail(isFree = true, paymentStatus = "held").toUpcomingDetails(now)

        assertEquals(PriceDisplay.Free, details.price)
        assertNull(details.paymentPill)
    }

    @Test
    fun paid_booking_with_no_payment_yet_has_no_pill() {
        assertNull(bookingDetail(isFree = false, paymentStatus = "none").toUpcomingDetails(now).paymentPill)
    }

    @Test
    fun blank_strings_map_to_null_and_link_and_type_are_copied() {
        val details =
            bookingDetail(
                expertTitle = "  ",
                clientNotes = "   ",
                meetingType = "skype_zoom",
                joinLink = "https://zoom.us/j/1",
            ).toUpcomingDetails(now)

        assertNull(details.expertTitle)
        assertNull(details.clientNotes)
        assertEquals("skype_zoom", details.meetingType)
        assertEquals("https://zoom.us/j/1", details.joinLink)
    }

    @Test
    fun blank_topic_title_maps_to_null() {
        val base = bookingDetail()
        val blank = base.copy(topic = base.topic.copy(title = " "))

        assertNull(blank.toUpcomingDetails(now).topicTitle)
        assertEquals("Algorithms", base.toUpcomingDetails(now).topicTitle)
    }

    @Test
    fun missing_end_time_falls_back_to_start_plus_duration() {
        val start = Instant.parse("2026-10-24T18:00:00Z")
        val details = bookingDetail(start = start, end = null).toUpcomingDetails(now)

        assertEquals(start, details.start)
        assertEquals(Instant.parse("2026-10-24T19:00:00Z"), details.end)
    }

    // ---- loading ----

    @Test
    fun creation_loads_the_booking_by_id_once() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ClientBookingDetail>()
            val ids = mutableListOf<String>()
            api.clientBookingDetailHandler = {
                ids += it
                gate.await()
            }
            val vm = vm()
            runCurrent()
            assertEquals(UpcomingBookingUiState.Loading, vm.uiState.value)
            assertEquals(listOf("b1"), ids)

            gate.complete(bookingDetail())
            runCurrent()
            assertEquals(
                UpcomingBookingUiState.Loaded(bookingDetail().toUpcomingDetails(now)),
                vm.uiState.value,
            )
            assertEquals(1, api.clientBookingDetailCalls)
        }

    @Test
    fun network_failure_ends_failed_and_retry_loads_again() =
        runTest(main.testDispatcher) {
            var calls = 0
            api.clientBookingDetailHandler = {
                calls++
                if (calls == 1) throw IOException("offline") else bookingDetail()
            }
            val vm = vm()
            runCurrent()
            assertEquals(
                UpcomingBookingUiState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value,
            )

            vm.retry()
            assertEquals(UpcomingBookingUiState.Loading, vm.uiState.value)
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)
            assertTrue(vm.uiState.value is UpcomingBookingUiState.Loaded)
        }

    // ---- quiet reloads ----

    @Test
    fun refreshQuietly_during_the_initial_load_adds_no_call() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { gate.await() }
            val vm = vm()
            runCurrent()

            vm.refreshQuietly()
            runCurrent()
            assertEquals(1, api.clientBookingDetailCalls)

            gate.complete(bookingDetail())
            runCurrent()
            assertEquals(1, api.clientBookingDetailCalls)
        }

    @Test
    fun refreshQuietly_when_loaded_reloads_without_showing_loading() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()
            val seen = mutableListOf<UpcomingBookingUiState>()

            api.clientBookingDetailHandler = { bookingDetail(status = "in_progress") }
            vm.refreshQuietly()
            seen += vm.uiState.value
            runCurrent()
            seen += vm.uiState.value

            assertEquals(2, api.clientBookingDetailCalls)
            assertTrue(seen.all { it is UpcomingBookingUiState.Loaded })
            assertEquals(BookingStatusLabel.InProgress, loaded(vm).details.status.label)
        }

    @Test
    fun a_failing_quiet_reload_keeps_the_loaded_details() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value

            api.clientBookingDetailHandler = { throw IOException("offline") }
            vm.refreshQuietly()
            runCurrent()

            assertEquals(2, api.clientBookingDetailCalls)
            assertEquals(before, vm.uiState.value)
        }

    @Test
    fun refreshQuietly_while_a_quiet_reload_runs_adds_nothing_until_the_screen_was_hidden() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()

            val gate = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { gate.await() }
            vm.refreshQuietly()
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)

            vm.refreshQuietly()
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)

            vm.onScreenHidden()
            vm.refreshQuietly()
            runCurrent()
            assertEquals(3, api.clientBookingDetailCalls)
        }

    // ---- realtime ----

    @Test
    fun a_booking_update_for_this_booking_triggers_one_quiet_reload() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()

            api.clientBookingDetailHandler = { bookingDetail(status = "completed") }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "completed"))
            runCurrent()

            assertEquals(2, api.clientBookingDetailCalls)
            assertEquals(BookingStatusLabel.Completed, loaded(vm).details.status.label)
        }

    @Test
    fun a_booking_update_for_another_booking_is_ignored() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            vm()
            runCurrent()

            realtime.emit(RealtimeEvent.BookingUpdate("other", "cancelled"))
            runCurrent()

            assertEquals(1, api.clientBookingDetailCalls)
        }

    // ---- reschedule response ----

    private fun pendingReschedule() =
        bookingDetail(status = "pending_reschedule", reschedule = rescheduleDto(initiatedBy = "expert"))

    private val rescheduleOk = RescheduleResponseData(bookingId = "b1", status = "confirmed")

    @Test
    fun accept_reloads_before_the_notice_is_set() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { pendingReschedule() }
            val vm = vm()
            runCurrent()
            val acceptGate = CompletableDeferred<RescheduleResponseData>()
            api.acceptClientRescheduleHandler = { _, _ -> acceptGate.await() }
            val reloadGate = CompletableDeferred<ClientBookingDetail>()

            vm.accept()
            runCurrent()
            assertTrue(vm.rescheduleState.value.isSubmitting)
            assertEquals(listOf("b1"), api.acceptClientRescheduleRequests.map { it.first })

            api.clientBookingDetailHandler = { reloadGate.await() }
            acceptGate.complete(rescheduleOk)
            runCurrent()
            assertEquals(2, api.clientBookingDetailCalls)
            assertNull(vm.rescheduleState.value.notice)
            assertTrue(vm.rescheduleState.value.isSubmitting)

            reloadGate.complete(bookingDetail(status = "confirmed"))
            runCurrent()
            assertEquals(RescheduleNotice.Accepted, vm.rescheduleState.value.notice)
            assertFalse(vm.rescheduleState.value.isSubmitting)
            assertNull(loaded(vm).details.reschedule)

            vm.consumeRescheduleNotice()
            assertNull(vm.rescheduleState.value.notice)
        }

    @Test
    fun decline_sends_the_trimmed_reason_then_reloads_and_sets_the_notice() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { pendingReschedule() }
            val vm = vm()
            runCurrent()
            api.declineClientRescheduleHandler = { _, _ -> rescheduleOk }

            vm.decline("  Busy ")
            runCurrent()

            assertEquals(
                listOf("b1" to DeclineRescheduleRequest("Busy")),
                api.declineClientRescheduleRequests,
            )
            assertEquals(2, api.clientBookingDetailCalls)
            assertEquals(RescheduleNotice.Declined, vm.rescheduleState.value.notice)
        }

    @Test
    fun accept_on_a_missing_booking_shows_a_pop_dialog_that_can_be_dismissed() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { pendingReschedule() }
            val vm = vm()
            runCurrent()
            api.acceptClientRescheduleHandler = { _, _ ->
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"Booking not found"}""")
            }

            vm.accept()
            runCurrent()

            assertEquals(
                RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_not_found), popOnDismiss = true),
                vm.rescheduleState.value.dialog,
            )
            vm.dismissRescheduleDialog()
            assertNull(vm.rescheduleState.value.dialog)
        }
}
