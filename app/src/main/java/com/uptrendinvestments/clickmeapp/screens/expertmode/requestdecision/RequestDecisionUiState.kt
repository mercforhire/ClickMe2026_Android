package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.Instant

/** Load state of the request detail. */
sealed interface RequestDecisionLoad {
    data object Loading : RequestDecisionLoad

    data class Failed(
        val message: FieldError,
    ) : RequestDecisionLoad

    data class Loaded(
        val details: RequestDecisionDetails,
    ) : RequestDecisionLoad
}

/**
 * The request as shown on the decision screen. [clientMessage] is the trimmed message, or null when blank. Names,
 * topic and message are untrusted server text: render as plain text only.
 */
data class RequestDecisionDetails(
    val requestId: String,
    val clientId: String,
    val clientName: String?,
    val clientAvatarUrl: String?,
    val start: Instant,
    val end: Instant,
    val topic: String?,
    val meetingType: String?,
    val clientMessage: String?,
    val earnings: RequestEarningsUi,
)

/** Which decision call is in flight. Accept and decline are single flight: only one can run at a time (T-11-36). */
enum class DecisionSubmitting {
    None,
    Accepting,
    Declining,
}

/** One-shot outcomes the screen turns into navigation, then consumes. */
sealed interface RequestDecisionEvent {
    /** The accept succeeded; [confirmed] carries everything Booking Confirmed shows (no refetch there). */
    data class Accepted(
        val confirmed: BookingConfirmedKey,
    ) : RequestDecisionEvent

    /** The decline succeeded; Incoming Requests shows its snackbar (D-03). */
    data object Declined : RequestDecisionEvent

    /** The request was already handled; leave for a refreshed Incoming Requests list (D-07). */
    data object ExitAfterProcessed : RequestDecisionEvent
}

/**
 * The "Couldn't update request" dialog. [popOnDismiss] is true for 409 ALREADY_PROCESSED: OK then leaves for a
 * refreshed Incoming Requests list. Otherwise OK keeps the screen and the sheet inputs.
 */
data class RequestDecisionDialog(
    val message: FieldError,
    val popOnDismiss: Boolean,
)

/**
 * [isExpired] comes from the tapped list row: the detail endpoint carries no status or expiry (C3). While
 * [submitting] is not [DecisionSubmitting.None] both actions are disabled and Back is locked.
 */
data class RequestDecisionUiState(
    val load: RequestDecisionLoad = RequestDecisionLoad.Loading,
    val isExpired: Boolean = false,
    val submitting: DecisionSubmitting = DecisionSubmitting.None,
    val dialog: RequestDecisionDialog? = null,
    val event: RequestDecisionEvent? = null,
)
