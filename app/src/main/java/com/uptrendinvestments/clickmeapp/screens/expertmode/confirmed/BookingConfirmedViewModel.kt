package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.screens.expertmode.DashboardLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Booking Confirmed has no state of its own (it shows only what its key carries, Pitfall 8). This ViewModel exists for
 * the Back to Home hand-off (D-05): the Dashboard may be in another stack position, so its ViewModel may not exist
 * yet, which is why the reload request goes through the one-shot [ExpertLaunchRequest] store and not a direct call.
 */
@HiltViewModel
class BookingConfirmedViewModel
    @Inject
    constructor(
        private val launchRequest: ExpertLaunchRequest,
    ) : ViewModel() {
        /** Asks the Dashboard to reload the next time it is shown, so its pending count drops after the accept. */
        fun backToHome() = launchRequest.dashboard.post(DashboardLaunch)
    }
