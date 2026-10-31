package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.DEFAULT_DURATION_MINS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The client Reschedule picker (Phase 16.2). All behavior lives in [ReschedulePickerController]; this ViewModel only
 * supplies the client's loaders: the booking detail by id, and the booking expert's availability with
 * `exclude_booking_id` set to this booking so its own slot is not reported as taken (D-14).
 */
@HiltViewModel(assistedFactory = RescheduleViewModel.Factory::class)
class RescheduleViewModel
    @AssistedInject
    constructor(
        @Assisted key: RescheduleKey,
        savedStateHandle: SavedStateHandle,
        bookings: ClientBookingsRepository,
        availability: BookingRepository,
        proposer: ClientRescheduleProposer,
        sentRequest: RescheduleSentRequest,
        callGuard: CallGuard,
        clock: Clock,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: RescheduleKey): RescheduleViewModel
        }

        private val controller =
            ReschedulePickerController(
                scope = viewModelScope,
                savedState = savedStateHandle,
                clock = clock,
                zoneProvider = ZoneId::systemDefault,
                bookingId = key.bookingId,
                opener = key.opener,
                loadBooking = { bookings.detail(key.bookingId).toPickerBooking() },
                loadMonth = { expertId, month ->
                    availability.availability(expertId, month, excludeBookingId = key.bookingId)
                },
                proposer = proposer,
                mapFailure = { it.toClientProposeFailure() },
                sentRequest = sentRequest,
                callGuard = callGuard,
            )

        val uiState: StateFlow<ReschedulePickerUiState> = controller.state

        init {
            controller.load()
        }

        fun retryLoad() = controller.retryLoad()

        fun previousMonth() = controller.previousMonth()

        fun nextMonth() = controller.nextMonth()

        fun selectDate(date: LocalDate) = controller.selectDate(date)

        fun selectSlot(start: Instant) = controller.selectSlot(start)

        fun retrySlots() = controller.retrySlots()

        fun updateMessage(text: String) = controller.updateMessage(text)

        fun submit() = controller.submit()

        fun refreshClock() = controller.refreshClock()

        fun consumeSent() = controller.consumeSent()

        fun consumeLeave() = controller.consumeLeave()
    }

/**
 * The picker's view of a client booking detail, or null when the expert id is missing (nothing to load availability
 * for). The end falls back to the topic duration, then [DEFAULT_DURATION_MINS], like the details screen.
 * `isPaid` follows [clientBookingIsPaid].
 */
fun ClientBookingDetail.toPickerBooking(): PickerBooking? {
    val expertId = expert.id?.takeIf { it.isNotBlank() } ?: return null
    return PickerBooking(
        viewer = RescheduleViewer.Client,
        counterpartName = expert.fullName?.takeIf { it.isNotBlank() },
        subtitle = expert.title?.takeIf { it.isNotBlank() } ?: topic.title.takeIf { it.isNotBlank() },
        avatarUrl = expert.avatarUrl,
        currentStart = startTime,
        currentEnd =
            endTime ?: startTime.plus(Duration.ofMinutes((topic.durationMins ?: DEFAULT_DURATION_MINS).toLong())),
        slotsTimezone = expertTimezone,
        availabilityExpertId = expertId,
        isPaid = clientBookingIsPaid(paymentStatus, topic.isFree),
        authorizationExpiresAt = authorizationExpiresAt,
    )
}
