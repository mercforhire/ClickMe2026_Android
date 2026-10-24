package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** Load state of the combined Upcoming and Past fetch. One state covers both tabs (RESEARCH Finding 1). */
sealed interface MyBookingsLoad {
    /** First load, or a retry after a failure: the screen shows a spinner. */
    data object Loading : MyBookingsLoad

    /** Both buckets arrived at least once; quiet reloads keep this state. */
    data object Loaded : MyBookingsLoad

    /** The first load failed; the screen shows [message] with Retry. */
    data class Failed(
        val message: FieldError,
    ) : MyBookingsLoad
}

/**
 * What My Bookings renders.
 *
 * @property lists both tab lists, derived together from the two server buckets.
 * @property isRefreshing a pull-to-refresh is running.
 * @property refreshFailed a pull-to-refresh failed; the screen shows a snackbar then calls `consumeRefreshFailure`.
 * @property notice a one-time message handed over by the cancel flow; cleared by `consumeNotice`.
 */
data class MyBookingsUiState(
    val load: MyBookingsLoad = MyBookingsLoad.Loading,
    val lists: BookingLists = BookingLists.EMPTY,
    val selectedTab: BookingsTab = BookingsTab.Upcoming,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val notice: BookingsNotice? = null,
)
