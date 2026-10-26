package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsNotice
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** Load state of the request list. */
sealed interface IncomingRequestsLoad {
    /** First load, or a retry after a failure: the screen shows the skeleton. */
    data object Loading : IncomingRequestsLoad

    /** The first load failed; the screen shows [message] with Retry. */
    data class Failed(
        val message: FieldError,
    ) : IncomingRequestsLoad

    /** The list arrived at least once; quiet reloads keep this state. [rows] is in display order. */
    data class Loaded(
        val rows: List<IncomingRequestRow>,
    ) : IncomingRequestsLoad
}

/**
 * What Incoming Requests renders.
 *
 * @property isRefreshing a pull-to-refresh is running.
 * @property refreshFailed a pull-to-refresh failed; the screen shows a snackbar then calls `consumeRefreshFailure`.
 * @property notice a one-time message handed over by a finished flow (the decline); cleared by `consumeNotice`.
 */
data class IncomingRequestsUiState(
    val load: IncomingRequestsLoad = IncomingRequestsLoad.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val notice: IncomingRequestsNotice? = null,
)
