package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

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
    val clientName: String?,
    val clientAvatarUrl: String?,
    val start: Instant,
    val end: Instant,
    val topic: String?,
    val meetingType: String?,
    val clientMessage: String?,
    val earnings: RequestEarningsUi,
)

/** [isExpired] comes from the tapped list row: the detail endpoint carries no status or expiry (C3). */
data class RequestDecisionUiState(
    val load: RequestDecisionLoad = RequestDecisionLoad.Loading,
    val isExpired: Boolean = false,
)
