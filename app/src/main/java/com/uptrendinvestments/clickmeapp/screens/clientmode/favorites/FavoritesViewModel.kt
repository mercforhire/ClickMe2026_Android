package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Saved Experts state machine. Port of iOS `FavoritesViewModel` with these deliberate deviations: D-09 (refresh on
 * return; iOS loads once), D-11 (pull-to-refresh and the Phase 15 states; iOS shows a spinner), D-12 (`limit=100`;
 * iOS uses the default 20), D-06 (View My History is wired; it does nothing on iOS), and a remove failure is a
 * snackbar message instead of the iOS alert.
 *
 * No load runs in `init`: the screen's first [onScreenShown] starts it (the MyBookings pattern).
 */
@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        private val repository: FavoritesRepository,
        private val launchRequest: BookingsLaunchRequest,
    ) : ViewModel() {
        private enum class LoadMode { Loud, Pull, Quiet }

        private val _uiState = MutableStateFlow(FavoritesUiState())
        val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        /**
         * D-09: the screen is shown, or shown again. A call while a current load runs does nothing, so first
         * composition never double-loads; a Loaded screen reloads quietly and keeps its rows.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(if (_uiState.value.load is FavoritesLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
        }

        /**
         * D-09: the screen was hidden or disposed. A load started before this may predate a change made while
         * hidden, so the next [onScreenShown] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        /** D-11: the Retry button on the first-load error (and any explicit reload). Shows the spinner again. */
        fun retry() = startLoad(LoadMode.Loud)

        /**
         * D-11: pull-to-refresh. Over rows it keeps them and shows the indicator; with no rows (Failed or Loading) it
         * behaves as a first load.
         */
        fun refresh() = startLoad(if (_uiState.value.load is FavoritesLoad.Loaded) LoadMode.Pull else LoadMode.Loud)

        /** The refresh-failed snackbar was shown; clears the one-shot flag. */
        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            // A new load clears a stale refreshing or refresh-failed flag (RESEARCH Pitfall 6): a cancelled pull
            // never reaches its own cleanup.
            _uiState.update {
                when (mode) {
                    LoadMode.Loud ->
                        it.copy(load = FavoritesLoad.Loading, isRefreshing = false, refreshFailed = false)
                    LoadMode.Pull -> it.copy(isRefreshing = true, refreshFailed = false)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val payload = repository.list()
                        _uiState.update {
                            it.copy(
                                load = FavoritesLoad.Loaded(payload.experts.map { e -> e.toUi() }),
                                isRefreshing = false,
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(load = FavoritesLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                                LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
                                // D-09: a failed return reload is silent and keeps the rows.
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }
    }
