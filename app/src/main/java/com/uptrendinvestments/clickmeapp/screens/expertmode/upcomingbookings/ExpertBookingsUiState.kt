package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** Load state of the combined Upcoming and Past fetch. One state covers both tabs. */
sealed interface ExpertBookingsLoad {
    /** First load, or a retry after a failure: the screen shows the skeleton. */
    data object Loading : ExpertBookingsLoad

    /** The first load failed; the screen shows [message] with Retry. */
    data class Failed(
        val message: FieldError,
    ) : ExpertBookingsLoad

    /** Both buckets arrived at least once; quiet reloads keep this state. */
    data object Loaded : ExpertBookingsLoad
}

/**
 * What the expert Bookings tab renders.
 *
 * @property lists both tab lists, derived together from the two server buckets.
 * @property isRefreshing a pull-to-refresh is running.
 * @property refreshFailed a pull-to-refresh failed; the screen shows a snackbar then calls `consumeRefreshFailure`.
 * @property notice a one-time message handed over by the cancel flow; cleared by `consumeNotice`.
 */
data class ExpertBookingsUiState(
    val selectedTab: BookingsTab = BookingsTab.Upcoming,
    val load: ExpertBookingsLoad = ExpertBookingsLoad.Loading,
    val lists: ExpertBookingLists = ExpertBookingLists.EMPTY,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val notice: BookingsNotice? = null,
)
