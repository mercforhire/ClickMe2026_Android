package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallResumeNotice
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
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
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSent
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

    private val callStarter = FakeCallStarter()
    private val callGuard = FakeCallGuard()
    private val notice = CallResumeNotice()
    private val sentRequest = RescheduleSentRequest()

    private fun vm(joinScope: CoroutineScope = CoroutineScope(Job() + main.testDispatcher)): UpcomingBookingViewModel {
        val bookings = ClientBookingsRepository(api)
        return UpcomingBookingViewModel(
            UpcomingBookingKey("b1"),
            bookings,
            RescheduleResponder(bookings, PaymentMethodsRepository(api)),
            realtime,
            clock,
            callStarter,
            callGuard,
            JoinClock(clock, joinScope),
            notice,
            sentRequest,
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
    fun toUpcomingDetailsCarriesTheServerJoinFlag() {
        assertTrue(bookingDetail(canJoin = true).toUpcomingDetails(now).canJoin)
        assertFalse(bookingDetail().toUpcomingDetails(now).canJoin)
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

    // ---- call resume note (D-11) ----

    @Test
    fun noteShownOnceWhenNoticePosted() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            notice.post("b1")
            val first = vm()
            runCurrent()
            val second = vm()
            runCurrent()

            assertTrue(loaded(first).showCallResumeNote)
            assertFalse(loaded(second).showCallResumeNote)
        }

    @Test
    fun noNoteForOtherBooking() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            notice.post("b2")
            val vm = vm()
            runCurrent()

            assertFalse(loaded(vm).showCallResumeNote)
        }

    @Test
    fun noteSurvivesReloads() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            notice.post("b1")
            val vm = vm()
            runCurrent()

            vm.refreshQuietly()
            runCurrent()

            assertTrue(loaded(vm).showCallResumeNote)
        }

    // ---- failed refresh with content (D-05) ----

    @Test
    fun realtimeFailureKeepsContentAndFlags() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()
            val before = loaded(vm).details

            api.clientBookingDetailHandler = { throw IOException("offline") }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "completed"))
            runCurrent()

            assertEquals(before, loaded(vm).details)
            assertTrue(loaded(vm).refreshFailed)

            vm.onRefreshFailureConsumed()
            assertFalse(loaded(vm).refreshFailed)
        }

    @Test
    fun aQuietResumeReloadFailureStaysSilent() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()

            api.clientBookingDetailHandler = { throw IOException("offline") }
            vm.refreshQuietly()
            runCurrent()

            assertFalse(loaded(vm).refreshFailed)
        }

    @Test
    fun refreshKeepsContentWhileRunning() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()
            val before = loaded(vm).details

            val gate = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { gate.await() }
            vm.refresh()
            runCurrent()
            assertTrue(loaded(vm).isRefreshing)
            assertEquals(before, loaded(vm).details)

            gate.complete(bookingDetail(status = "in_progress"))
            runCurrent()
            assertFalse(loaded(vm).isRefreshing)
            assertFalse(loaded(vm).refreshFailed)
            assertEquals(BookingStatusLabel.InProgress, loaded(vm).details.status.label)
        }

    @Test
    fun refreshFailureFlagsAndClearsTheRunningState() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()
            val before = loaded(vm).details

            api.clientBookingDetailHandler = { throw IOException("offline") }
            vm.refresh()
            runCurrent()

            assertFalse(loaded(vm).isRefreshing)
            assertTrue(loaded(vm).refreshFailed)
            assertEquals(before, loaded(vm).details)
        }

    // ---- unavailable (UI-SPEC Detail rule 5) ----

    @Test
    fun aNotFoundFirstLoadIsUnavailableNotFailed() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
            }
            val vm = vm()
            runCurrent()

            assertEquals(UpcomingBookingUiState.Unavailable, vm.uiState.value)
        }

    @Test
    fun aNotFoundQuietReloadWithContentBecomesUnavailable() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()

            api.clientBookingDetailHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
            }
            realtime.emit(RealtimeEvent.BookingUpdate("b1", "cancelled"))
            runCurrent()

            assertEquals(UpcomingBookingUiState.Unavailable, vm.uiState.value)
        }

    @Test
    fun refreshQuietlyDoesNotCancelARunningRetry() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()

            val gate = CompletableDeferred<ClientBookingDetail>()
            api.clientBookingDetailHandler = { gate.await() }
            vm.refresh()
            runCurrent()
            vm.onScreenHidden()
            vm.refreshQuietly()
            runCurrent()
            assertTrue(loaded(vm).isRefreshing)

            gate.complete(bookingDetail(status = "in_progress"))
            runCurrent()
            assertFalse(loaded(vm).isRefreshing)
            assertEquals(BookingStatusLabel.InProgress, loaded(vm).details.status.label)
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

    // ---- join and cancel guard ----

    @Test
    fun expert_id_is_mapped_and_blank_becomes_null() {
        assertEquals("e1", bookingDetail().toUpcomingDetails(now).expertId)
        assertNull(bookingDetail(expertId = " ").toUpcomingDetails(now).expertId)
        assertNull(bookingDetail(expertId = null).toUpcomingDetails(now).expertId)
    }

    @Test
    fun join_records_one_start_request_for_the_loaded_booking() =
        runTest(main.testDispatcher) {
            val start = Instant.parse("2026-10-24T18:00:00Z")
            api.clientBookingDetailHandler = { bookingDetail(start = start) }
            val vm = vm()
            runCurrent()

            vm.join()

            assertEquals(
                listOf(
                    CallStartRequest(
                        bookingId = "b1",
                        peerId = "e1",
                        peerName = "Ada Lovelace",
                        peerAvatarUrl = null,
                        topic = "Algorithms",
                        scheduledStart = start,
                        scheduledEnd = start.plusSeconds(3600),
                        meetingType = MeetingType.InAppVoice,
                    ),
                ),
                callStarter.requests,
            )
        }

    @Test
    fun join_before_the_booking_has_loaded_does_nothing() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { CompletableDeferred<ClientBookingDetail>().await() }
            val vm = vm()
            runCurrent()

            vm.join()

            assertTrue(callStarter.requests.isEmpty())
        }

    @Test
    fun join_does_not_re_check_the_window_at_tap_time() =
        runTest(main.testDispatcher) {
            // Start is far in the future: the button would be disabled, but the server has the final say (D-02).
            api.clientBookingDetailHandler = { bookingDetail(start = now.plusSeconds(86_400)) }
            val vm = vm()
            runCurrent()

            vm.join()

            assertEquals(1, callStarter.requests.size)
        }

    @Test
    fun attemptCancel_returns_the_guards_answer_and_records_the_attempt() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()

            assertTrue(vm.attemptCancel())
            callGuard.blocked = true
            assertFalse(vm.attemptCancel())

            assertEquals(
                listOf("b1" to RestrictedAction.CancelBooking, "b1" to RestrictedAction.CancelBooking),
                callGuard.modifyAttempts,
            )
        }

    @Test
    fun joinNow_emits_the_join_clock_value() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm(backgroundScope)
            runCurrent()
            assertEquals(now, vm.joinNow.value)
        }

    // ---- Reschedule sent hand-off (Phase 16.2, D-09) ----

    @Test
    fun postedBeforeCreationIsConsumedOnce() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            sentRequest.onSent(RescheduleOpener.UpcomingDetails, "b1")

            val vm = vm()
            runCurrent()

            assertTrue(vm.rescheduleSent.value)
            assertNull(sentRequest.upcomingDetails.pending.value)
            assertEquals(1, api.clientBookingDetailCalls)
            vm.consumeRescheduleSent()
            assertFalse(vm.rescheduleSent.value)
        }

    @Test
    fun postedWhileAliveReloadsQuietly() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            val vm = vm()
            runCurrent()
            assertEquals(1, api.clientBookingDetailCalls)
            assertFalse(vm.rescheduleSent.value)

            sentRequest.onSent(RescheduleOpener.UpcomingDetails, "b1")
            // The reload is quiet: the details stay on screen, never the skeleton.
            assertTrue(vm.uiState.value is UpcomingBookingUiState.Loaded)
            runCurrent()

            assertEquals(2, api.clientBookingDetailCalls)
            assertTrue(vm.uiState.value is UpcomingBookingUiState.Loaded)
            assertTrue(vm.rescheduleSent.value)
            assertNull(sentRequest.upcomingDetails.pending.value)
        }

    @Test
    fun otherBookingNotConsumed() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            sentRequest.onSent(RescheduleOpener.UpcomingDetails, "b2")

            val vm = vm()
            runCurrent()

            assertFalse(vm.rescheduleSent.value)
            assertEquals(RescheduleSent("b2"), sentRequest.upcomingDetails.pending.value)
            assertEquals(1, api.clientBookingDetailCalls)
        }

    @Test
    fun otherOpenersUntouched() =
        runTest(main.testDispatcher) {
            api.clientBookingDetailHandler = { bookingDetail() }
            sentRequest.onSent(RescheduleOpener.MyBookings, "b1")

            val vm = vm()
            runCurrent()

            assertFalse(vm.rescheduleSent.value)
            assertEquals(RescheduleSent("b1"), sentRequest.myBookings.pending.value)
        }
}
