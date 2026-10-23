package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.Instant

/** Max length of the optional decline reason (D-04; backend `declineSchema.reason` max 500). */
const val DECLINE_REASON_MAX_CHARS = 500

/**
 * What the shared reschedule card renders. Built from the booking detail by `toRescheduleProposalUi` (plan 08-05)
 * so Upcoming Booking Details and Booking Summary show the same card (D-01).
 *
 * @property initiatedByExpert false means the read-only "Awaiting expert's response" card (D-03).
 * @property showsPaymentDisclosure the accept dialog adds the card-hold-release disclosure.
 * @property chargesSoon the accept dialog adds the "may be charged sooner" line.
 */
data class RescheduleProposalUi(
    val currentStart: Instant,
    val proposedStart: Instant,
    val proposedEnd: Instant?,
    val note: String?,
    val initiatedByExpert: Boolean,
    val showsPaymentDisclosure: Boolean,
    val chargesSoon: Boolean,
)

/** Which reschedule response call is in flight. */
enum class RescheduleSubmitting { None, Accepting, Declining }

/** The dialog to show on top of a detail screen after a reschedule response. */
sealed interface RescheduleDialog {
    /** D-02 recovery: the booking has no saved card, offer to add a payment method. */
    data object PaymentMethodNeeded : RescheduleDialog

    /** D-02 recovery: the card on file can't be used for this reschedule. */
    data object CardUnusable : RescheduleDialog

    /** "Couldn't update booking" error dialog; [popOnDismiss] leaves the screen when it is dismissed. */
    data class Error(
        val message: FieldError,
        val popOnDismiss: Boolean = false,
    ) : RescheduleDialog
}

/** One-shot success notice, shown as a snackbar (UI-SPEC A3). */
enum class RescheduleNotice { Accepted, Declined }

/** Reschedule response state held by the controller and read by both detail ViewModels. */
data class RescheduleUiState(
    val submitting: RescheduleSubmitting = RescheduleSubmitting.None,
    val dialog: RescheduleDialog? = null,
    val notice: RescheduleNotice? = null,
) {
    val isSubmitting: Boolean get() = submitting != RescheduleSubmitting.None
}
