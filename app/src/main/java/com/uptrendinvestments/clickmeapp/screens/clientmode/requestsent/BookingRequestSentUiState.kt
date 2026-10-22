package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import java.time.Duration
import java.time.Instant

private const val DEFAULT_DURATION_MINS = 30L

/**
 * What the Request Sent screen shows for one booking. [isFree] is the topic-level flag (D-14): it hides the
 * payment-lifecycle card and selects the free headline. Phase 7 adds the paid lifecycle data; this phase only
 * needs [isFree] to hide that card.
 */
data class BookingRequestSentDetails(
    val expertName: String?,
    val expertAvatarUrl: String?,
    val topicTitle: String,
    val start: Instant,
    val end: Instant,
    val isFree: Boolean,
)

/** Request Sent load state (D-14): fetch by booking id, retry on failure. */
sealed interface BookingRequestSentUiState {
    data object Loading : BookingRequestSentUiState

    data class Loaded(
        val details: BookingRequestSentDetails,
    ) : BookingRequestSentUiState

    data object Failed : BookingRequestSentUiState
}

/**
 * Maps the booking detail to the display model. A blank expert name becomes null (the screen shows the
 * "Expert" fallback, iOS parity); a missing end time falls back to start plus the topic duration (default 30
 * minutes).
 */
fun ClientBookingDetail.toRequestSentDetails(): BookingRequestSentDetails =
    BookingRequestSentDetails(
        expertName = expert.fullName?.takeIf { it.isNotBlank() },
        expertAvatarUrl = expert.avatarUrl,
        topicTitle = topic.title,
        start = startTime,
        end = endTime ?: startTime.plus(Duration.ofMinutes(topic.durationMins?.toLong() ?: DEFAULT_DURATION_MINS)),
        isFree = topic.isFree,
    )
