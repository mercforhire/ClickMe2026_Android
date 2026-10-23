package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.DEFAULT_DURATION_MINS
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.Duration
import java.time.Instant

/** The two steps of the cancel flow (D-08): pick a reason and comment, then confirm. */
enum class CancelStep { Reason, Confirm }

/** What the cancel screen shows about the booking being cancelled. */
data class CancellationSummary(
    val expertName: String?,
    val expertTitle: String?,
    val expertAvatarUrl: String?,
    val start: Instant,
    val end: Instant,
)

/** Load state of the booking behind the cancel screen. */
sealed interface CancellationLoad {
    data object Loading : CancellationLoad

    data class Failed(
        val message: FieldError,
    ) : CancellationLoad

    data class Loaded(
        val summary: CancellationSummary,
    ) : CancellationLoad
}

/**
 * The "Couldn't cancel booking" dialog.
 *
 * @property returnToBookings dismissing it leaves the screen for My Bookings (Upcoming) instead of staying on the
 *   confirm step.
 */
data class CancelErrorDialog(
    val message: FieldError,
    val returnToBookings: Boolean,
)

/**
 * Cancel screen state. [isFinished] tells the screen to navigate away to My Bookings; it stays set until
 * `consumeFinished`. [isSubmitting] stays true after a success so nothing is tappable during the navigation.
 */
data class ClientCancellationUiState(
    val load: CancellationLoad = CancellationLoad.Loading,
    val step: CancelStep = CancelStep.Reason,
    val reason: CancellationReason = CancellationReason.ChangeOfPlans,
    val comment: String = "",
    val refund: RefundPreview = RefundPreview.None,
    val isSubmitting: Boolean = false,
    val error: CancelErrorDialog? = null,
    val isFinished: Boolean = false,
)

/**
 * Maps the booking detail to the display model. Blank expert names and titles become null (the screen falls back to
 * "Expert"); a missing end time falls back to start plus the topic duration (default 30 minutes), as Request Sent does.
 */
fun ClientBookingDetail.toCancellationSummary(): CancellationSummary =
    CancellationSummary(
        expertName = expert.fullName?.takeIf { it.isNotBlank() },
        expertTitle = expert.title?.takeIf { it.isNotBlank() },
        expertAvatarUrl = expert.avatarUrl,
        start = startTime,
        end = endTime ?: startTime.plus(Duration.ofMinutes((topic.durationMins ?: DEFAULT_DURATION_MINS).toLong())),
    )
