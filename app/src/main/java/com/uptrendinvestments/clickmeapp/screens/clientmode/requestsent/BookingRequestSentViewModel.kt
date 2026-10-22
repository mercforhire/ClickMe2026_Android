package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Request Sent state machine (BOOK-04). Port of iOS `BookingRequestSentViewModel`.
 *
 * The route carries only the booking id (D-14), so the booking is fetched on creation; after process death the
 * entry is restored and re-fetches. [retry] re-sends the request, cancelling any call still in flight
 * (latest wins). Every [ApiException] (404 for a booking the caller does not own, network error, suspended
 * account) ends in [BookingRequestSentUiState.Failed]; the global dialog owns the suspended case.
 */
@HiltViewModel(assistedFactory = BookingRequestSentViewModel.Factory::class)
class BookingRequestSentViewModel
    @AssistedInject
    constructor(
        @Assisted key: RequestSentKey,
        private val repository: ClientBookingsRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: RequestSentKey): BookingRequestSentViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow<BookingRequestSentUiState>(BookingRequestSentUiState.Loading)
        val uiState: StateFlow<BookingRequestSentUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            load()
        }

        fun retry() = load()

        private fun load() {
            loadJob?.cancel()
            _uiState.value = BookingRequestSentUiState.Loading
            loadJob =
                viewModelScope.launch {
                    _uiState.value =
                        try {
                            BookingRequestSentUiState.Loaded(repository.detail(bookingId).toRequestSentDetails())
                        } catch (e: ApiException) {
                            BookingRequestSentUiState.Failed
                        }
                }
        }
    }
