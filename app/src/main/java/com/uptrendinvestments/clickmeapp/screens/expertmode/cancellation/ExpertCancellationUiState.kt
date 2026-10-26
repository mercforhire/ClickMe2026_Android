package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelErrorDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelStep
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.Instant

/** What the expert cancel screen shows about the booking being cancelled. */
data class ExpertCancelSummary(
    val clientName: String?,
    val clientAvatarUrl: String?,
    val topic: String?,
    val start: Instant,
    val end: Instant,
)

/** Load state of the booking behind the cancel screen. */
sealed interface ExpertCancelLoad {
    data object Loading : ExpertCancelLoad

    data class Failed(
        val message: FieldError,
    ) : ExpertCancelLoad

    data class Loaded(
        val summary: ExpertCancelSummary,
    ) : ExpertCancelLoad
}

/**
 * Expert cancel screen state. [reason] is null until the expert picks one (UI-SPEC A10). [isFinished] tells the screen
 * to navigate away; it stays set until `consumeFinished`. [isSubmitting] stays true after a success so nothing is
 * tappable during the navigation.
 */
data class ExpertCancellationUiState(
    val load: ExpertCancelLoad = ExpertCancelLoad.Loading,
    val step: CancelStep = CancelStep.Reason,
    val reason: ExpertCancelReason? = null,
    val comment: String = "",
    val refund: ExpertRefundPreview = ExpertRefundPreview.None,
    val isSubmitting: Boolean = false,
    val error: CancelErrorDialog? = null,
    val isFinished: Boolean = false,
)

/** Maps the booking detail to the display model. Blank names and topics become null. */
fun ExpertBookingDetail.toExpertCancelSummary(): ExpertCancelSummary =
    ExpertCancelSummary(
        clientName = client.name?.takeIf { it.isNotBlank() },
        clientAvatarUrl = client.avatarUrl,
        topic = session.topicTitle?.takeIf { it.isNotBlank() },
        start = startTime,
        end = endTime,
    )
