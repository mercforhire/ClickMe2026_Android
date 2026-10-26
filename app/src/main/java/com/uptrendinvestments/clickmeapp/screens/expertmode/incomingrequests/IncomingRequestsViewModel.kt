package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Incoming Requests state machine (EXP-06). Port of iOS `IncomingRequestsViewModel`.
 *
 * The list endpoint has no pagination: one call returns every pending request (RESEARCH C2), so no page or limit is
 * sent. Rows are built once per load against the injected clock, so the expiry hours on screen match the load.
 *
 * Refresh triggers: pull-to-refresh ([refresh], flags a failure for the snackbar) and a quiet reload when the screen
 * is shown again ([onScreenShown]). A quiet reload never shows Loading and a failed one keeps the previous rows.
 *
 * Hand-offs arrive through [ExpertLaunchRequest.incomingRequests] and always mean "reload":
 * - D-03: a decline pops back here with [IncomingRequestsLaunch.notice] set, so the screen shows "Request declined"
 *   once and the declined request is gone from the list.
 * - D-04: Back from Booking Confirmed lands here, and the accepted request must be gone.
 * - D-07: the ALREADY_PROCESSED dialog returns here, and the stale request must be gone.
 * A launch posted before this ViewModel existed is applied in `init`, before the first load, so that load is the
 * fresh one; a launch posted while it lives forces a quiet reload and is applied exactly once.
 */
@HiltViewModel
class IncomingRequestsViewModel
    @Inject
    constructor(
        private val repository: BookingRequestsRepository,
        private val launchRequest: ExpertLaunchRequest,
        private val clock: Clock,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the skeleton and a failure message. */
            Loud,

            /** Silent: keeps the previous rows and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private val _uiState = MutableStateFlow(IncomingRequestsUiState())
        val uiState: StateFlow<IncomingRequestsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        init {
            launchRequest.incomingRequests.consume()?.let { applyLaunch(it, reload = false) }
            startLoad(LoadMode.Loud)
            viewModelScope.launch {
                launchRequest.incomingRequests.pending.filterNotNull().collect {
                    launchRequest.incomingRequests.consume()?.let { launch -> applyLaunch(launch, reload = true) }
                }
            }
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /**
         * The screen is shown again. The screen calls this from `LifecycleEventEffect(ON_RESUME)` and
         * `LaunchedEffect(Unit)`. A call while a current load runs does nothing, so first composition never
         * double-loads; a Loaded screen reloads quietly.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(if (_uiState.value.load is IncomingRequestsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
        }

        /**
         * The screen was hidden or disposed (`ON_PAUSE`, disposal; Nav3 may not deliver ON_RESUME to a covered entry).
         * A load started before this may predate a change made while hidden, so the next [onScreenShown] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun consumeNotice() {
            _uiState.update { it.copy(notice = null) }
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun applyLaunch(
            launch: IncomingRequestsLaunch,
            reload: Boolean,
        ) {
            launch.notice?.let { notice -> _uiState.update { it.copy(notice = notice) } }
            if (reload) {
                startLoad(if (_uiState.value.load is IncomingRequestsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
            }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = IncomingRequestsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val rows = buildIncomingRequestRows(repository.list(), clock.instant())
                        _uiState.update { it.copy(load = IncomingRequestsLoad.Loaded(rows), isRefreshing = false) }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(
                                        load = IncomingRequestsLoad.Failed(e.toSectionMessage()),
                                        isRefreshing = false,
                                    )
                                LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }
    }
