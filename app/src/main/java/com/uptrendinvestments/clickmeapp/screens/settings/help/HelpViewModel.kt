package com.uptrendinvestments.clickmeapp.screens.settings.help

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

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

fun buildHelpSections(payload: FaqsPayload): List<FaqCategory> = emptyList()

@HiltViewModel
class HelpViewModel
    @Inject
    constructor(
        private val repository: HelpRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HelpUiState())
        val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()
    }
