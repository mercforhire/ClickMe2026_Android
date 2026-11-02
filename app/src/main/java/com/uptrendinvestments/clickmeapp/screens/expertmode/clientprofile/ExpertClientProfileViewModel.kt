package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isNotFound
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State machine for the expert's read-only Client Profile. Ports iOS `ClientProfileViewModel`: the profile loads
 * first, then both bookings buckets (in parallel here; iOS is sequential only for a Swift 6 reason) and the
 * history is filtered to this client by [buildClientHistory].
 *
 * Failures (Phase 16.4):
 * - Only the profile call can produce a specific state. A 403 `NO_RELATIONSHIP` is
 *   [ClientProfileLoad.NoRelationship] (D-14) and a 404 is [ClientProfileLoad.Unavailable] (D-15); iOS shows a generic
 *   error with Retry for both. Neither state offers Retry, because a retry cannot change the answer; pull-to-refresh
 *   stays available, as on iOS. Neither fetches bookings. A 403 with any other code is a generic failure.
 * - Any other profile failure, and a failure of either bookings call, is the whole-screen
 *   [ClientProfileLoad.Failed] with Retry, never a partial history (D-12). A 404 on a bookings call is generic too.
 * - A pull over loaded content that fails keeps the content and raises [ExpertClientProfileUiState.refreshFailed]; a
 *   pull over loaded content that now answers 404 or `NO_RELATIONSHIP` shows that state. A pull from any
 *   non-loaded state is a first load. Nothing on this screen can be edited, so there is no refresh on return.
 *
 * Expert shell only: the screen is reachable solely through the expert route, because the backend's `ensureExpert`
 * converts a client-only account on the first touch of any expert endpoint (T-16.4-16).
 */
@HiltViewModel(assistedFactory = ExpertClientProfileViewModel.Factory::class)
class ExpertClientProfileViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertClientProfileKey,
        private val profiles: ExpertClientProfileRepository,
        private val bookings: ExpertBookingsRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ExpertClientProfileKey): ExpertClientProfileViewModel
        }

        private enum class LoadMode { Loud, Pull }

        private val clientId = key.clientId

        private val _uiState = MutableStateFlow(ExpertClientProfileUiState())
        val uiState: StateFlow<ExpertClientProfileUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            startLoad(LoadMode.Loud)
        }

        /** Reloads from the failed state (Retry). */
        fun retry() = startLoad(LoadMode.Loud)

        /** Pull-to-refresh: keeps loaded content on screen; from any other state it is a first load. */
        fun refresh() = startLoad(if (_uiState.value.load is ClientProfileLoad.Loaded) LoadMode.Pull else LoadMode.Loud)

        /** Clears the one-shot refresh-failed flag after the snackbar was shown. */
        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            _uiState.update {
                it.copy(
                    refreshFailed = false,
                    isRefreshing = mode == LoadMode.Pull,
                    load = if (mode == LoadMode.Loud) ClientProfileLoad.Loading else it.load,
                )
            }
            loadJob =
                viewModelScope.launch {
                    val profile =
                        try {
                            profiles.profile(clientId)
                        } catch (e: ApiException) {
                            applyProfileFailure(e, mode)
                            return@launch
                        }
                    val history =
                        try {
                            coroutineScope {
                                val upcoming = async { bookings.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings }
                                val past = async { bookings.list(ExpertBookingsRepository.TYPE_PAST).bookings }
                                buildClientHistory(clientId, upcoming.await(), past.await())
                            }
                        } catch (e: ApiException) {
                            applyFailure(e, mode)
                            return@launch
                        }
                    _uiState.update {
                        it.copy(load = ClientProfileLoad.Loaded(profile, history), isRefreshing = false)
                    }
                }
        }

        private fun applyProfileFailure(
            e: ApiException,
            mode: LoadMode,
        ) {
            when {
                e is ApiException.Http && e.code == ErrorCode.NoRelationship ->
                    _uiState.update { it.copy(load = ClientProfileLoad.NoRelationship, isRefreshing = false) }
                e.isNotFound() ->
                    _uiState.update { it.copy(load = ClientProfileLoad.Unavailable, isRefreshing = false) }
                else -> applyFailure(e, mode)
            }
        }

        private fun applyFailure(
            e: ApiException,
            mode: LoadMode,
        ) {
            _uiState.update {
                if (mode == LoadMode.Loud) {
                    it.copy(load = ClientProfileLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                } else {
                    it.copy(refreshFailed = true, isRefreshing = false)
                }
            }
        }
    }
