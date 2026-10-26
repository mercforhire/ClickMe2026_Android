package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Incoming Requests state machine (EXP-06). Port of iOS `IncomingRequestsViewModel`.
 *
 * The list endpoint has no pagination: one call returns every pending request (RESEARCH C2), so no page or limit is
 * sent. Rows are built once per load against the injected clock, so the expiry hours on screen match the load.
 */
@HiltViewModel
class IncomingRequestsViewModel
    @Inject
    constructor(
        private val repository: BookingRequestsRepository,
        @Suppress("UnusedPrivateProperty") private val launchRequest: ExpertLaunchRequest,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(IncomingRequestsUiState())
        val uiState: StateFlow<IncomingRequestsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            startLoad()
        }

        fun retry() = startLoad()

        private fun startLoad() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = IncomingRequestsLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val rows = buildIncomingRequestRows(repository.list(), clock.instant())
                        _uiState.update { it.copy(load = IncomingRequestsLoad.Loaded(rows)) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = IncomingRequestsLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }
    }
