package com.uptrendinvestments.clickmeapp.screens.settings.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The FAQ load: the first load shows [Loading]; a failure is [Failed] with a message ready to show. */
sealed interface HelpLoad {
    data object Loading : HelpLoad

    data class Loaded(
        val categories: List<FaqCategory>,
    ) : HelpLoad

    data class Failed(
        val message: FieldError,
    ) : HelpLoad
}

data class HelpUiState(
    val load: HelpLoad = HelpLoad.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

/**
 * Categories worth showing: those with no featured articles are dropped (iOS `HelpViewModel.forceLoad`), the rest are
 * ordered by `display_order`. The sort is stable, so equal orders keep the server's order.
 */
fun buildHelpSections(payload: FaqsPayload): List<FaqCategory> =
    payload.categories.filter { it.featuredArticles.isNotEmpty() }.sortedBy { it.displayOrder }

/**
 * State holder for Help (PROF-05). Loads `GET help/faqs` once on entry; a single latest-wins job, so [retry] cancels
 * a load still running. Expanded categories are screen state, not kept here.
 */
@HiltViewModel
class HelpViewModel
    @Inject
    constructor(
        private val repository: HelpRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HelpUiState())
        val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            retry()
        }

        /** First load and Retry: the screen shows the skeleton, and a failure the error state. */
        fun retry() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = HelpLoad.Loading, isRefreshing = false) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val categories = buildHelpSections(repository.faqs())
                        _uiState.update { it.copy(load = HelpLoad.Loaded(categories)) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = HelpLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        /**
         * Pull-to-refresh: reloads quietly and keeps the list when it fails, flagging the failure for a snackbar.
         * With nothing loaded yet there is no list to keep, so it is a [retry].
         */
        fun refresh() {
            if (_uiState.value.load !is HelpLoad.Loaded) {
                retry()
                return
            }
            loadJob?.cancel()
            _uiState.update { it.copy(isRefreshing = true) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val categories = buildHelpSections(repository.faqs())
                        _uiState.update { it.copy(load = HelpLoad.Loaded(categories), isRefreshing = false) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(isRefreshing = false, refreshFailed = true) }
                    }
                }
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }
    }
