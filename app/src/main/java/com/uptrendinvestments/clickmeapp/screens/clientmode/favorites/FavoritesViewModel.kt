package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
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

        /** Ids whose DELETE is in flight; a reload never brings these rows back (RESEARCH Pitfall 4). */
        private val removing = mutableSetOf<String>()

        /** Ids whose DELETE succeeded after the current load started; a load that began earlier may still list them. */
        private val removedSinceLoadStart = mutableSetOf<String>()

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
            // A load that starts after a successful DELETE reflects the server, so the guard resets here.
            removedSinceLoadStart.clear()
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
                        val experts =
                            payload.experts
                                .map { e -> e.toUi() }
                                .filterNot { e -> e.expertId in removing || e.expertId in removedSinceLoadStart }
                        _uiState.update { it.copy(load = FavoritesLoad.Loaded(experts), isRefreshing = false) }
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

        /**
         * D-10: confirmed removal. The row drops at once and DELETE is sent; on failure the row returns to its
         * original index and [FavoritesUiState.removeFailed] carries the message (iOS `removeFavorite`). Only a
         * Loaded list with the id present is touched.
         */
        fun remove(expertId: String) {
            val rows = (_uiState.value.load as? FavoritesLoad.Loaded)?.experts ?: return
            val index = rows.indexOfFirst { it.expertId == expertId }
            if (index < 0) return
            val row = rows[index]
            removing += expertId
            _uiState.update { it.copy(load = FavoritesLoad.Loaded(rows.filterNot { r -> r.expertId == expertId })) }
            viewModelScope.launch {
                try {
                    repository.remove(expertId)
                    removedSinceLoadStart += expertId
                } catch (e: ApiException) {
                    val failure = e.toFavoriteFailure()
                    _uiState.update { state ->
                        val current = (state.load as? FavoritesLoad.Loaded)?.experts
                        if (current == null || current.any { it.expertId == expertId }) {
                            state.copy(removeFailed = failure)
                        } else {
                            val restored = current.toMutableList().apply { add(index.coerceAtMost(size), row) }
                            state.copy(load = FavoritesLoad.Loaded(restored), removeFailed = failure)
                        }
                    }
                } finally {
                    removing -= expertId
                }
            }
        }

        /** The remove-failed snackbar was shown; clears the one-shot message. */
        fun consumeRemoveFailure() {
            _uiState.update { it.copy(removeFailed = null) }
        }

        /**
         * D-06: "View My History". Posts the Past launch; the screen calls this and then the shell switches to the
         * Bookings tab (`Navigator.showMyBookings()`). My Bookings consumes the launch at init or while alive.
         */
        fun viewHistory() {
            launchRequest.post(BookingsLaunch(BookingsTab.Past))
        }
    }
