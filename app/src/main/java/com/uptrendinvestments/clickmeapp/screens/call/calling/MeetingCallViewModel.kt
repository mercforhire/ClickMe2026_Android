package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallCenter
import com.uptrendinvestments.clickmeapp.managers.CallDialog
import com.uptrendinvestments.clickmeapp.managers.CallPermissionBroker
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import com.uptrendinvestments.clickmeapp.managers.PendingMicPermission
import com.uptrendinvestments.clickmeapp.managers.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

/** Display tick of the call timer. Drives rendering only; the 3600 s cutoff is [CallCenter]'s. */
const val UI_TICK_MS = 1_000L

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * View-state adapter for the root call overlay (D-08). All call state and logic live in the app-scoped
 * [CallCenter]; this ViewModel only projects the session to [MeetingCallUiState] at a 1 s tick and forwards the
 * user's actions, mirroring iOS where the views observe the `CallCenter` singleton's view model.
 *
 * The ticker runs only while [uiState] is collected (the layer or the pill is on screen), so a backgrounded app
 * does no per-second work (UI-SPEC A8). The 3600 s cutoff does not depend on it.
 */
@HiltViewModel
class MeetingCallViewModel
    @Inject
    constructor(
        private val callCenter: CallCenter,
        private val permissionBroker: CallPermissionBroker,
        userManager: UserManager,
        clock: Clock,
    ) : ViewModel() {
        private val ticker =
            flow {
                while (true) {
                    emit(clock.instant())
                    delay(UI_TICK_MS)
                }
            }

        private val myAvatarUrl =
            userManager.profile.map { it?.personalDetails?.avatarUrl }.distinctUntilChanged()

        /** The expanded screen's state, or null when there is no live call. */
        val uiState: StateFlow<MeetingCallUiState?> =
            combine(callCenter.session, ticker, myAvatarUrl) { session, now, mine ->
                session?.toMeetingCallUiState(now, mine)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

        /** True when the call is collapsed to the pill. */
        val isMinimized: StateFlow<Boolean> = callCenter.isMinimized

        /** The dialog at the head of the queue, or null. */
        val dialog: StateFlow<CallDialog?> =
            callCenter.dialogs
                .map { it.firstOrNull() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

        /** The microphone request the overlay should fulfil, or null. */
        val pendingPermission: StateFlow<PendingMicPermission?> = permissionBroker.pending

        fun minimize() = callCenter.minimize()

        fun expand() = callCenter.expand()

        fun endCall() = callCenter.endCall()

        fun toggleMute() = callCenter.toggleMute()

        fun toggleSpeaker() = callCenter.toggleSpeaker()

        fun onExternalLinkOpened() = callCenter.onExternalLinkOpened()

        fun dismissDialog() = callCenter.dismissDialog()

        fun onPermissionResult(
            id: Long,
            result: MicPermissionResult,
        ) = permissionBroker.complete(id, result)
    }
