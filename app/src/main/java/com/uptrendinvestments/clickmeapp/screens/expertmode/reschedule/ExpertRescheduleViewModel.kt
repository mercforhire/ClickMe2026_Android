package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.PickerBooking
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.ReschedulePickerController
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.ReschedulePickerUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.toExpertProposeFailure
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The expert Reschedule picker (Phase 16.2, PARITY-AUDIT #11). All behavior lives in the shared
 * `ReschedulePickerController`; this ViewModel supplies the expert's loaders: the expert booking detail by id, and the
 * SIGNED-IN expert's own availability with `exclude_booking_id` set to this booking (D-14). The expert id comes only
 * from [CurrentUserIdProvider]: when it is unavailable the load fails rather than loading anyone else's calendar
 * (T-16.2-24). The expert proposer sends one PATCH with no card and the picker never shows the payment disclosure
 * (RESCHEDULE-PAYMENT-CONVERSION.md sections 4 and 6).
 */
@HiltViewModel(assistedFactory = ExpertRescheduleViewModel.Factory::class)
class ExpertRescheduleViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertRescheduleKey,
        savedStateHandle: SavedStateHandle,
        bookings: ExpertBookingsRepository,
        availability: BookingRepository,
        proposer: ExpertRescheduleProposer,
        sentRequest: RescheduleSentRequest,
        callGuard: CallGuard,
        currentUserId: CurrentUserIdProvider,
        clock: Clock,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ExpertRescheduleKey): ExpertRescheduleViewModel
        }

        private val controller =
            ReschedulePickerController(
                scope = viewModelScope,
                savedState = savedStateHandle,
                clock = clock,
                zoneProvider = ZoneId::systemDefault,
                bookingId = key.bookingId,
                opener = key.opener,
                loadBooking = {
                    currentUserId.currentUserId()?.takeIf { it.isNotBlank() }?.let { me ->
                        bookings.detail(key.bookingId).toPickerBooking(me, ZoneId.systemDefault())
                    }
                },
                loadMonth = { expertId, month ->
                    availability.availability(expertId, month, excludeBookingId = key.bookingId)
                },
                proposer = proposer,
                mapFailure = { it.toExpertProposeFailure() },
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

        fun confirmDisclosure() = controller.confirmDisclosure()

        fun dismissDialog() = controller.dismissDialog()

        fun refreshClock() = controller.refreshClock()

        fun consumeSent() = controller.consumeSent()

        fun consumeLeave() = controller.consumeLeave()
    }

/**
 * The picker's view of an expert booking detail (iOS `ExpertRescheduleViewModel.map`, D-14). The counterpart is the
 * client and the subtitle the topic. Slots are shown in the device zone [zone], so the timezone note reads "Times are
 * in your time zone". [expertId] is the signed-in expert's own id. `isPaid` follows [ExpertBookingDetail.isPaid]
 * (RESEARCH A2; the server's paid-cap backstop covers a wrongly "free" booking). The authorization expiry is null: the
 * expert is never shown the payment disclosure (D-02).
 */
fun ExpertBookingDetail.toPickerBooking(
    expertId: String,
    zone: ZoneId,
): PickerBooking =
    PickerBooking(
        viewer = RescheduleViewer.Expert,
        counterpartName = client.name?.takeIf { it.isNotBlank() },
        subtitle = session.topicTitle?.takeIf { it.isNotBlank() },
        avatarUrl = client.avatarUrl,
        currentStart = startTime,
        currentEnd = endTime,
        slotsTimezone = zone.id,
        availabilityExpertId = expertId,
        isPaid = isPaid,
        authorizationExpiresAt = null,
    )
