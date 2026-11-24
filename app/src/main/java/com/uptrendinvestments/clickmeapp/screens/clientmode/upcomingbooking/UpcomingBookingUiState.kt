package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.DEFAULT_DURATION_MINS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.CANCELLABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_APPROVAL
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.detailStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.paymentPillFor
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.toRescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val SHORT_ID_LENGTH = 8

/** The ID row on the details screen: first eight characters, upper-cased, with a leading `#` (iOS parity). */
fun shortBookingId(id: String): String = "#" + id.take(SHORT_ID_LENGTH).uppercase(Locale.ROOT)

/**
 * What Upcoming Booking Details shows for one booking (BOOK-06).
 *
 * @property status the booking's real status (D-14); iOS hardcodes "Confirmed".
 * @property price Money or Free from the topic-level flag (D-14).
 * @property paymentPill the money-state pill for paid held / captured / refunded bookings only (D-15).
 * @property showsPendingBanner the booking awaits expert approval.
 * @property canCancel the status allows Cancel: confirmed, pending approval and pending reschedule only. The screen
 *   also applies the time rule in [canClientCancelNow].
 * @property reschedule the shared reschedule card model, present only for `pending_reschedule` (D-01, D-03).
 * @property expertId the expert's user id, sent as the peer id when Join Call starts the call (CALL-01).
 * @property canJoin the GET /client/bookings/{id} `can_join`. It only opens the reconnect tier of
 *   [isWithinJoinWindow], and it is always fresh because the ViewModel reloads on `booking.update`.
 */
@Suppress("LongParameterList")
data class UpcomingBookingDetails(
    val bookingId: String,
    val shortId: String,
    val status: BookingStatusDisplay,
    val expertName: String?,
    val expertTitle: String?,
    val expertAvatarUrl: String?,
    val topicTitle: String?,
    val price: PriceDisplay,
    val start: Instant,
    val end: Instant,
    val expertTimezone: String?,
    val paymentPill: PaymentPill?,
    val meetingType: String?,
    val joinLink: String?,
    val clientNotes: String?,
    val showsPendingBanner: Boolean,
    val canCancel: Boolean,
    val reschedule: RescheduleProposalUi?,
    val expertId: String? = null,
    val canJoin: Boolean = false,
)

/**
 * 16.5 review WR-01: whether Cancel Booking is shown right now. Payment History can open a confirmed or
 * pending_reschedule booking whose session has already ended, and the server's cancel has no time check, so a tap would
 * cancel a past session. Those statuses hide Cancel from [now] at or after the end. Pending approval keeps it because
 * no session has taken place and the client can still withdraw the request.
 *
 * This deliberately differs from iOS, which has no time check. [now] must come from the ticking join clock so the end
 * bound takes effect live.
 */
fun UpcomingBookingDetails.canClientCancelNow(now: Instant): Boolean =
    canCancel && (status.raw == STATUS_PENDING_APPROVAL || now.isBefore(end))

/** Upcoming Booking Details load state. */
sealed interface UpcomingBookingUiState {
    data object Loading : UpcomingBookingUiState

    data class Failed(
        val message: FieldError,
    ) : UpcomingBookingUiState

    /** The server no longer has this booking (404). Not an error: no Retry, the back arrow is the exit. */
    data object Unavailable : UpcomingBookingUiState

    /**
     * @property showCallResumeNote the one-time "You were on a call" note, set for the booking a process kill
     *   interrupted (D-11, UI-SPEC A4).
     * @property isRefreshing a user Retry of a failed refresh is running; the content stays (D-05).
     * @property refreshFailed a background or Retry refresh failed with content showing; the screen shows the
     *   refresh-failed snackbar and consumes the flag.
     */
    data class Loaded(
        val details: UpcomingBookingDetails,
        val showCallResumeNote: Boolean = false,
        val isRefreshing: Boolean = false,
        val refreshFailed: Boolean = false,
    ) : UpcomingBookingUiState
}

/**
 * Pure mapping of the booking detail to the display model. Blank strings become null; a missing end time falls
 * back to start plus the topic duration (default 30 minutes). There is no private-note field (D-16). [now] comes
 * from the injected `Clock`.
 */
fun ClientBookingDetail.toUpcomingDetails(now: Instant): UpcomingBookingDetails =
    UpcomingBookingDetails(
        bookingId = bookingId,
        shortId = shortBookingId(bookingId),
        status = detailStatusDisplay(status),
        expertName = expert.fullName?.takeIf { it.isNotBlank() },
        expertTitle = expert.title?.takeIf { it.isNotBlank() },
        expertAvatarUrl = expert.avatarUrl,
        topicTitle = topic.title.takeIf { it.isNotBlank() },
        price = PriceDisplay.of(topic.isFree, topic.price?.amount, topic.price?.currency),
        start = startTime,
        end = endTime ?: startTime.plus(Duration.ofMinutes((topic.durationMins ?: DEFAULT_DURATION_MINS).toLong())),
        expertTimezone = expertTimezone?.takeIf { it.isNotBlank() },
        paymentPill = paymentPillFor(topic.isFree, paymentStatus),
        meetingType = meetingType?.takeIf { it.isNotBlank() },
        joinLink = joinLink?.takeIf { it.isNotBlank() },
        clientNotes = clientNotes?.takeIf { it.isNotBlank() },
        showsPendingBanner = status == STATUS_PENDING_APPROVAL,
        canCancel = status in CANCELLABLE_BOOKING_STATUSES,
        reschedule = toRescheduleProposalUi(now),
        expertId = expert.id?.takeIf { it.isNotBlank() },
        canJoin = canJoin,
    )
