package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsNotice
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
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
 * Request Decision state (EXP-06). Port of iOS `RequestDecisionViewModel`: the read side plus accept and decline.
 *
 * The request detail carries no status and no expiry, so [RequestDecisionUiState.isExpired] comes from the tapped
 * list row through the key (C3). Earnings use `earnings_breakdown` plus the list row's major-unit earnings (C4).
 *
 * [accept] and [decline] are single flight (T-11-36): nothing starts while a call is in flight, and an expired or
 * not yet loaded request cannot be decided. Success is announced as a one-shot [RequestDecisionEvent]. The client
 * message, the accept message, the decline note and the reason text are user-written free text and are never logged.
 */
@HiltViewModel(assistedFactory = RequestDecisionViewModel.Factory::class)
class RequestDecisionViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: RequestDecisionKey,
        private val repository: BookingRequestsRepository,
        private val launchRequest: ExpertLaunchRequest,
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

        /**
         * Accepts the request with the optional [message] (trimmed, omitted when blank). On success posts a reload to
         * Incoming Requests, so Back from Booking Confirmed lands on a refreshed list (D-04), and emits
         * [RequestDecisionEvent.Accepted] carrying the Confirmed key built from the loaded request.
         */
        fun accept(message: String) {
            val details = beginSubmitting(DecisionSubmitting.Accepting) ?: return
            viewModelScope.launch {
                try {
                    val data = repository.accept(key.requestId, acceptMessage(message))
                    launchRequest.incomingRequests.post(IncomingRequestsLaunch())
                    // submitting stays set on success: nothing is tappable while the screen is being replaced.
                    _uiState.update {
                        it.copy(event = RequestDecisionEvent.Accepted(details.toConfirmedKey(data.bookingId)))
                    }
                } catch (e: ApiException) {
                    onDecisionFailed(e)
                }
            }
        }

        /**
         * Declines the request with a backend [reason] code and the optional [note] (D-01, D-02): a blank note sends
         * the reason's label as `reason_text`, which the backend requires. On success posts a reload plus the
         * "Request declined" notice to Incoming Requests and emits [RequestDecisionEvent.Declined] (D-03).
         */
        fun decline(
            reason: DeclineReason,
            note: String,
        ) {
            beginSubmitting(DecisionSubmitting.Declining) ?: return
            viewModelScope.launch {
                try {
                    repository.decline(key.requestId, reason.wire, declineReasonText(reason, note))
                    launchRequest.incomingRequests.post(IncomingRequestsLaunch(IncomingRequestsNotice.Declined))
                    _uiState.update { it.copy(event = RequestDecisionEvent.Declined) }
                } catch (e: ApiException) {
                    onDecisionFailed(e)
                }
            }
        }

        /**
         * A failed accept or decline unlocks the screen and maps the error (D-07). ALREADY_PROCESSED is never a
         * success: its dialog sends the expert back to a refreshed list. Other errors keep the sheet inputs.
         */
        private fun onDecisionFailed(e: ApiException) {
            val dialog =
                when (val failure = e.toRequestDecisionFailure()) {
                    RequestDecisionFailure.Silent -> null
                    is RequestDecisionFailure.AlreadyProcessed -> RequestDecisionDialog(failure.message, true)
                    is RequestDecisionFailure.Show -> RequestDecisionDialog(failure.message, false)
                }
            _uiState.update { it.copy(submitting = DecisionSubmitting.None, dialog = dialog) }
        }

        /**
         * Closes the error dialog. After ALREADY_PROCESSED it also asks Incoming Requests to reload, so the stale
         * request is gone, and leaves this screen ([RequestDecisionEvent.ExitAfterProcessed]).
         */
        fun dismissDialog() {
            val dialog = _uiState.value.dialog ?: return
            if (dialog.popOnDismiss) {
                launchRequest.incomingRequests.post(IncomingRequestsLaunch())
                _uiState.update { it.copy(dialog = null, event = RequestDecisionEvent.ExitAfterProcessed) }
            } else {
                _uiState.update { it.copy(dialog = null) }
            }
        }

        fun consumeEvent() {
            _uiState.update { it.copy(event = null) }
        }

        /** Moves to [kind] when a decision may start now, and returns the loaded request; null when it may not. */
        private fun beginSubmitting(kind: DecisionSubmitting): RequestDecisionDetails? {
            val current = _uiState.value
            val loaded = current.load as? RequestDecisionLoad.Loaded ?: return null
            if (current.submitting != DecisionSubmitting.None || current.isExpired) return null
            _uiState.update { it.copy(submitting = kind) }
            return loaded.details
        }

        private fun RequestDecisionDetails.toConfirmedKey(bookingId: String) =
            BookingConfirmedKey(
                bookingId = bookingId,
                clientName = clientName,
                clientAvatarUrl = clientAvatarUrl,
                topic = topic,
                startMillis = start.toEpochMilli(),
                endMillis = end.toEpochMilli(),
                meetingType = meetingType,
                clientMessage = clientMessage,
            )

        private fun BookingRequestDetail.toDetails(): RequestDecisionDetails =
            RequestDecisionDetails(
                requestId = requestId,
                clientId = client.id,
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
