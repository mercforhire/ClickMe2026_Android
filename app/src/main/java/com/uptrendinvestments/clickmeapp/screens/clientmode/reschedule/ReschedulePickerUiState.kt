@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * The booking being rescheduled, as the picker header (UI-SPEC Screen 1) and the rules need it.
 *
 * Client side: [counterpartName] is the expert and [subtitle] the expert's title, else the topic. Expert side:
 * [counterpartName] is the client and [subtitle] the topic. Null names fall back in the UI
 * (`reschedule_start_client_fallback`, `reschedule_start_session_fallback`).
 *
 * [authorizationExpiresAt] is always null on the expert side: the expert is not charged, so no disclosure.
 * [availabilityExpertId] is the booking's expert (client) or the signed-in expert (D-14). [isPaid] decides the
 * 6-day pick cap.
 */
data class PickerBooking(
    val viewer: RescheduleViewer,
    val counterpartName: String?,
    val subtitle: String?,
    val avatarUrl: String?,
    val currentStart: Instant,
    val currentEnd: Instant,
    val slotsTimezone: String?,
    val availabilityExpertId: String,
    val isPaid: Boolean,
    val authorizationExpiresAt: Instant?,
) {
    /** The original session length; the amended D-11 disclosure end is a proposed start plus this. */
    val originalDuration: Duration
        get() = Duration.between(currentStart, currentEnd)
}

/** Whether the booking behind the picker has loaded (UI-SPEC Screen 1: the header and the Message card wait on it). */
sealed interface PickerLoad {
    data object Loading : PickerLoad

    data class Failed(
        val message: FieldError,
    ) : PickerLoad

    data class Loaded(
        val booking: PickerBooking,
    ) : PickerLoad
}

/**
 * The dialogs the picker can show (UI-SPEC Screen 1 Interaction Contract), one at a time.
 *
 * [Disclosure] is the payment disclosure the client confirms before sending (D-11); [chargesSoon] adds the
 * under-48-hour caveat. [PaymentMethodNeeded] and [CardUnusable] are the server's payment refusals (D-06, D-13).
 * [Error] is any other failure; [Error.leaveOnDismiss] pops the picker after the dialog closes (a booking that is no
 * longer reschedulable).
 */
sealed interface ReschedulePickerDialog {
    data class Disclosure(
        val chargesSoon: Boolean,
    ) : ReschedulePickerDialog

    data object PaymentMethodNeeded : ReschedulePickerDialog

    data object CardUnusable : ReschedulePickerDialog

    data class Error(
        val message: FieldError,
        val leaveOnDismiss: Boolean = false,
    ) : ReschedulePickerDialog
}

/**
 * Everything the reschedule picker renders (UI-SPEC Screen 1), plus the rules derived from it. Shared by the client
 * picker (plan 06) and the expert picker (plan 08).
 *
 * [now] is stamped by the ViewModel from the injected Clock on every load and user action, so past-slot dimming and
 * the open-day dots are deterministic in tests. [zone] is the viewer's zone that [today] and the slot grouping were
 * derived from. [selectedDateLoaded] is false when [selectedDate] is outside the loaded months.
 *
 * [sent] and [leave] are one-shot signals the route consumes to pop. [isSubmitting] is never saved. No card id ever
 * lives in this state.
 */
data class ReschedulePickerUiState(
    val zone: ZoneId,
    val now: Instant,
    val displayedMonth: YearMonth,
    val selectedDate: LocalDate,
    val load: PickerLoad = PickerLoad.Loading,
    val availability: AvailabilityState = AvailabilityState.Loading,
    val selectedDateLoaded: Boolean = true,
    val selectedSlotStart: Instant? = null,
    val message: String = "",
    val isSubmitting: Boolean = false,
    val dialog: ReschedulePickerDialog? = null,
    val sent: Boolean = false,
    val leave: Boolean = false,
) {
    val today: LocalDate
        get() = now.atZone(zone).toLocalDate()

    val booking: PickerBooking?
        get() = (load as? PickerLoad.Loaded)?.booking

    /** The booking's own start; its slot is never pickable (D-12). */
    val currentStart: Instant?
        get() = booking?.currentStart

    /** D-01, D-02: now + 6 days for a paid booking, null for a free one (and while the booking is loading). */
    val latestSelectableStart: Instant?
        get() = booking?.let { latestPaidStart(it.isPaid, now) }

    val slotsForSelectedDate: List<BookingTimeSlot>
        get() = (availability as? AvailabilityState.Loaded)?.slotsByDate?.get(selectedDate).orEmpty()

    val openDays: Set<LocalDate>
        get() =
            (availability as? AvailabilityState.Loaded)
                ?.slotsByDate
                ?.pickableDays(now, latestSelectableStart, currentStart)
                .orEmpty()

    /** Send is enabled once the booking has loaded, a slot is chosen, and no send is in flight. */
    val canSubmit: Boolean
        get() = booking != null && selectedSlotStart != null && !isSubmitting
}
