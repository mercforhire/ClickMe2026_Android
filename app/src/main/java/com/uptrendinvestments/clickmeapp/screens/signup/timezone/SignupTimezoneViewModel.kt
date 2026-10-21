package com.uptrendinvestments.clickmeapp.screens.signup.timezone

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.fieldMessage
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignupTimezoneUiState(
    val selectedId: String,
    val entries: List<TimezoneEntry>,
    val showSheet: Boolean = false,
    val timezoneError: FieldError? = null,
    val dialogError: FieldError? = null,
    val saveState: SaveState = SaveState.Idle,
) {
    /** Label of the selected zone; falls back to the runtime label when the id is not in [entries]. */
    val selectedLabel: String
        get() = entries.firstOrNull { it.id == selectedId }?.label ?: Timezones.entryFor(selectedId).label
}

private const val KEY_SELECTED = "timezone_selected"
private const val SAVED_HOLD_MS = 600L

/**
 * Timezone step state (D-04, D-13). The selection seeds from the shared draft (device zone or the hydrated server
 * value) unless the user already chose one; the list is the curated 10 zones plus the seed zone first when it is not
 * one of them, so the default is always visible. Save sends only `{timezone}`.
 */
@HiltViewModel
class SignupTimezoneViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private var seedId: String = repository.draft.value.timezone
        private var userChose: Boolean = savedStateHandle.contains(KEY_SELECTED)

        private val _uiState =
            MutableStateFlow(
                buildState(
                    selectedId = savedStateHandle.get<String>(KEY_SELECTED) ?: seedId,
                    showSheet = false,
                ),
            )
        val uiState: StateFlow<SignupTimezoneUiState> = _uiState.asStateFlow()

        private val _saved = MutableStateFlow(false)
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        init {
            // Hydration may finish after this screen opens; follow the draft until the user picks a zone.
            viewModelScope.launch {
                repository.draft
                    .map { it.timezone }
                    .distinctUntilChanged()
                    .collect { id ->
                        seedId = id
                        _uiState.update { current ->
                            buildState(if (userChose) current.selectedId else id, current)
                        }
                    }
            }
        }

        private fun buildState(
            selectedId: String,
            current: SignupTimezoneUiState? = null,
            showSheet: Boolean = current?.showSheet ?: false,
        ): SignupTimezoneUiState {
            val base = Timezones.pickerEntries()
            val listed = base.mapTo(HashSet()) { it.id }
            val extras =
                listOf(seedId, selectedId)
                    .distinct()
                    .filter { it !in listed }
                    .map { Timezones.entryFor(it) }
            return SignupTimezoneUiState(
                selectedId = selectedId,
                entries = extras + base,
                showSheet = showSheet,
                timezoneError = current?.timezoneError,
                dialogError = current?.dialogError,
                saveState = current?.saveState ?: SaveState.Idle,
            )
        }

        fun openSheet() = _uiState.update { it.copy(showSheet = true) }

        fun dismissSheet() = _uiState.update { it.copy(showSheet = false) }

        fun select(id: String) {
            userChose = true
            savedStateHandle[KEY_SELECTED] = id
            _uiState.update { buildState(id, it, showSheet = false).copy(timezoneError = null) }
        }

        /** Resets the selection to the device zone (nearest entry when not listed, UTC last; see [Timezones]). */
        fun autoDetect() = select(Timezones.detected().id)

        fun save() {
            val state = _uiState.value
            if (state.saveState != SaveState.Idle) return
            _uiState.update { it.copy(saveState = SaveState.Saving, timezoneError = null, dialogError = null) }
            viewModelScope.launch {
                try {
                    repository.saveTimezone(state.selectedId)
                } catch (e: ApiException) {
                    fail(e)
                    return@launch
                }
                _uiState.update { it.copy(saveState = SaveState.Saved) }
                delay(SAVED_HOLD_MS)
                _saved.value = true
            }
        }

        private fun fail(e: ApiException) {
            val inline = (e as? ApiException.Http)?.takeIf { it.status == HTTP_UNPROCESSABLE }?.fieldMessage("timezone")
            _uiState.update {
                if (inline != null) {
                    it.copy(saveState = SaveState.Idle, timezoneError = FieldError.Text(inline))
                } else {
                    it.copy(saveState = SaveState.Idle, dialogError = e.toDialogError())
                }
            }
        }

        fun dismissDialog() = _uiState.update { it.copy(dialogError = null) }

        fun onSavedConsumed() {
            _saved.value = false
            _uiState.update { it.copy(saveState = SaveState.Idle) }
        }

        private companion object {
            const val HTTP_UNPROCESSABLE = 422
        }
    }
