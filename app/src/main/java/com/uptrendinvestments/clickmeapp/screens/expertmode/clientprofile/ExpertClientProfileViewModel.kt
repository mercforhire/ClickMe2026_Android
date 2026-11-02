package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
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
                    try {
                        val profile = profiles.profile(clientId)
                        val history =
                            coroutineScope {
                                val upcoming = async { bookings.list(ExpertBookingsRepository.TYPE_UPCOMING).bookings }
                                val past = async { bookings.list(ExpertBookingsRepository.TYPE_PAST).bookings }
                                buildClientHistory(clientId, upcoming.await(), past.await())
                            }
                        _uiState.update {
                            it.copy(
                                load = ClientProfileLoad.Loaded(profile, history),
                                isRefreshing = false,
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            it.copy(
                                load = ClientProfileLoad.Failed(e.toSectionMessage()),
                                isRefreshing = false,
                            )
                        }
                    }
                }
        }
    }
