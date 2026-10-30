package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/**
 * The notifications list (PUSH-05). Port of iOS `NotificationsListViewModel` (D-16): loads the first 50 rows, no read
 * state, grouped by local day. The device time zone is read at load time.
 *
 * Refresh triggers: pull-to-refresh ([refresh], flags a failure for the snackbar); a quiet reload when the screen is
 * shown again ([onScreenShown], a failure is silent); and a reload on every foreground push, the iOS
 * `newNotificationTick` (D-12), which flags a failure so the snackbar offers Retry (Phase 15 D-05).
 * Neither reload shows Loading and a failed one keeps the rows. A row tap goes through the same pipeline as a
 * push tap ([onRowTapped]).
 */
@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val repository: NotificationsRepository,
        private val launchRequest: PushLaunchRequest,
        private val clock: Clock,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the spinner and a failure message. */
            Loud,

            /** Silent: keeps the previous rows and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,

            /**
             * Reload started by a foreground push arrival (Phase 15 D-05): behaves like [Quiet] (no skeleton, no
             * indicator, rows kept) but flags a failure so the refresh-failed snackbar offers Retry.
             */
            Realtime,
        }

        private val _uiState = MutableStateFlow(NotificationsUiState())
        val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        init {
            startLoad(LoadMode.Loud)
            // A push that arrived before this ViewModel existed is already covered by the first load.
            val seen = launchRequest.arrivals.value
            viewModelScope.launch {
                launchRequest.arrivals.collect { tick ->
                    if (tick != seen) startLoad(LoadMode.Realtime)
                }
            }
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /**
         * The screen is shown again. A call while a current load runs does nothing, so first composition never
         * double-loads; otherwise it reloads quietly.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(LoadMode.Quiet)
        }

        /** The screen was hidden or disposed: a load started before this may predate a change made while hidden. */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        /** Posts the row's destination to the push pipeline; an inert row (no destination) does nothing. */
        fun onRowTapped(row: NotificationRowModel) {
            row.destination?.let { launchRequest.destination.post(it) }
        }

        private fun startLoad(requested: LoadMode) {
            // Nothing to keep yet: a quiet or realtime reload before the first success is a loud one.
            val mode =
                if (requested.isBackground() && _uiState.value.load !is NotificationsLoad.Loaded) {
                    LoadMode.Loud
                } else {
                    requested
                }
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = NotificationsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet, LoadMode.Realtime -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val loaded = loadSections()
                        _uiState.update { it.copy(load = loaded, isRefreshing = false) }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(load = NotificationsLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                                LoadMode.Pull, LoadMode.Realtime ->
                                    it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private fun LoadMode.isBackground() = this == LoadMode.Quiet || this == LoadMode.Realtime

        private suspend fun loadSections(): NotificationsLoad.Loaded {
            val now = clock.instant()
            val zone = ZoneId.systemDefault()
            val rows = repository.recent().map { it.toRowModel(now, zone) }
            return NotificationsLoad.Loaded(groupNotifications(rows, now, zone))
        }
    }
