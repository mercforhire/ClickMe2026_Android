package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showMyBookings
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.showExpertBookings

/**
 * Why a self-delete was refused with a 409, and what the blocker dialog says and offers. Port of iOS
 * `AccountDeletionBlocker` (SET-06, D-10).
 *
 * The backend's `assertNoBlockingBookings` returns one of three codes from both `POST user/account/delete-request` and
 * `DELETE user/account`. The text is client-owned: server messages are not shown for these codes, and the text names
 * no booking, person or amount. Branch on the [ErrorCode], never on the message.
 */
enum class AccountDeletionBlocker {
    /** Upcoming bookings where the user is the expert. */
    ExpertUpcoming,

    /** Upcoming sessions the user booked as a client (confirmed or pending_reschedule; pending_approval does not block). */
    ClientUpcoming,

    /** A session is live or about to start, on either side. */
    SessionInProgress,
    ;

    /**
     * Whether the dialog offers "View My Bookings". The bookings list that holds the blocking bookings is the one for
     * the blocker's side, so the action is only useful when the active [mode] matches it. A session in progress is
     * reachable from either mode.
     */
    fun offersBookingsAction(mode: AppMode): Boolean =
        when (this) {
            ExpertUpcoming -> mode == AppMode.Expert
            ClientUpcoming -> mode == AppMode.Client
            SessionInProgress -> true
        }

    /** The dialog message. In the wrong mode it names the mode to switch to. */
    @StringRes
    fun messageRes(mode: AppMode): Int =
        when (this) {
            ExpertUpcoming ->
                if (mode == AppMode.Expert) {
                    R.string.delete_account_blocker_expert_in_expert_mode
                } else {
                    R.string.delete_account_blocker_expert_in_client_mode
                }
            ClientUpcoming ->
                if (mode == AppMode.Client) {
                    R.string.delete_account_blocker_client_in_client_mode
                } else {
                    R.string.delete_account_blocker_client_in_expert_mode
                }
            SessionInProgress -> R.string.delete_account_blocker_session_in_progress
        }

    companion object {
        /** The dialog title, shared by all three blockers. */
        @get:StringRes
        val titleRes: Int = R.string.delete_account_blocker_title

        /** The "View My Bookings" button label. */
        @get:StringRes
        val bookingsActionRes: Int = R.string.delete_account_blocker_bookings

        /** The blocker for a 409's [code]. Any other code, and null, is not a blocker. */
        fun fromCode(code: ErrorCode?): AccountDeletionBlocker? =
            when (code) {
                ErrorCode.ExpertHasUpcomingBookings -> ExpertUpcoming
                ErrorCode.ClientHasUpcomingBookings -> ClientUpcoming
                ErrorCode.SessionInProgress -> SessionInProgress
                else -> null
            }
    }
}

/**
 * "View My Bookings" from the blocker dialog: returns the Profile tab (which holds Delete Account) to its root, then
 * opens the Bookings tab of the active [mode], resetting the stacks the way a finished cancel does. iOS `showMyBookings`
 * leaves the Profile path alone, so this Android-only reset keeps Delete Account from being parked under the Bookings
 * tab (see the Request Sent precedent, Phase 16.3 CR-01).
 */
fun Navigator.showBookingsFromDeleteAccount(mode: AppMode) {
    when (mode) {
        AppMode.Client -> {
            if (ClientProfileRoot in state.backStacks) reselect(ClientProfileRoot)
            showMyBookings()
        }
        AppMode.Expert -> {
            if (ExpertProfileRoot in state.backStacks) reselect(ExpertProfileRoot)
            showExpertBookings()
        }
    }
}
