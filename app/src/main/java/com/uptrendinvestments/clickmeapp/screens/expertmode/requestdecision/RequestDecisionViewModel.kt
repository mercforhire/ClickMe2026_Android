package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Request Decision state (EXP-06). Port of the read side of iOS `RequestDecisionViewModel`; accept and decline are
 * added by plan 11-12.
 *
 * The request detail carries no status and no expiry, so [RequestDecisionUiState.isExpired] comes from the tapped
 * list row through the key (C3). Earnings use `earnings_breakdown` plus the list row's major-unit earnings (C4). The
 * client message is user-written free text and is never logged.
 */
@HiltViewModel(assistedFactory = RequestDecisionViewModel.Factory::class)
class RequestDecisionViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: RequestDecisionKey,
        private val repository: BookingRequestsRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: RequestDecisionKey): RequestDecisionViewModel
        }

        private val _uiState =
            MutableStateFlow(RequestDecisionUiState(load = RequestDecisionLoad.Loading, isExpired = key.isExpired))
        val uiState: StateFlow<RequestDecisionUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = RequestDecisionLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val detail = repository.detail(key.requestId)
                        _uiState.update { it.copy(load = RequestDecisionLoad.Loaded(detail.toDetails())) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = RequestDecisionLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        private fun BookingRequestDetail.toDetails(): RequestDecisionDetails =
            RequestDecisionDetails(
                requestId = requestId,
                clientName = client.name,
                clientAvatarUrl = client.avatarUrl,
                start = session.startTime,
                end = session.endTime,
                topic = session.topic?.trim()?.takeIf { it.isNotEmpty() },
                meetingType = session.meetingType,
                clientMessage = session.clientMessage?.trim()?.takeIf { it.isNotEmpty() },
                earnings =
                    requestEarnings(
                        breakdown = economics.earningsBreakdown,
                        listEarningsMajor = key.listEarningsMajor,
                        listCurrency = key.listCurrency,
                        locale = Locale.getDefault(),
                    ),
            )
    }
