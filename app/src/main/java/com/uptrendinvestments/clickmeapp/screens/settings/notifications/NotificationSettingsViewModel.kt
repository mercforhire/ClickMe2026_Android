package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The preferences load: the first load shows [Loading]; a failure is [Failed] with a message ready to show. */
sealed interface NotificationLoad {
    data object Loading : NotificationLoad

    data object Loaded : NotificationLoad

    data class Failed(
        val message: FieldError,
    ) : NotificationLoad
}

/**
 * [values] maps each grid row's key (`category|sub_category`) to its toggle. [status] is the whole save-failure UI:
 * [AutoSaveStatus.Error] with a Retry action, and no modal message state exists (CONTEXT D-02).
 */
data class NotificationSettingsUiState(
    val load: NotificationLoad = NotificationLoad.Loading,
    val values: Map<String, Boolean> = defaultNotificationValues(),
    val status: AutoSaveStatus = AutoSaveStatus.Idle,
)

/**
 * State holder for Notification Settings (PARITY-AUDIT #5). Loads the preferences once, then auto-saves every toggle
 * flip through an [AutoSaver] with an 800 ms debounce.
 *
 * Deliberate differences from iOS: hydration goes through [AutoSaver.rebase], so opening the screen sends no PATCH
 * (iOS PATCHes every row on open); a failed save keeps the toggles as the user set them and offers Retry (D-02, iOS
 * shows an alert with no retry). Known behavior carried from iOS: the first PATCH rewrites all 13 rows to one value
 * across the three channels.
 *
 * Leaving flushes a pending edit on the application scope. Limitation of [AutoSaver.flush]: a failure after the
 * screen is gone is not shown; the row shows its old value on the next open, because the server kept it.
 */
@HiltViewModel
class NotificationSettingsViewModel
    @Inject
    constructor(
        private val repository: NotificationSettingsRepository,
        @ApplicationScope appScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NotificationSettingsUiState())
        val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

        // onFailure is deliberately empty: the Error status is the whole failure UI.
        private val saver =
            AutoSaver<Map<String, Boolean>>(
                scope = viewModelScope,
                flushScope = appScope,
                baseline = defaultNotificationValues(),
                persist = { _, new -> repository.update(new.toPreferencesRequest()) },
                onFailure = { },
                debounceMs = NOTIFICATION_DEBOUNCE_MS,
            )

        private var loadJob: Job? = null

        init {
            viewModelScope.launch {
                saver.status.collect { status -> _uiState.update { it.copy(status = status) } }
            }
            retry()
        }

        /** First load and Retry: a single latest-wins job, so a second call cancels a load still running. */
        fun retry() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = NotificationLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val values = hydrateNotificationValues(repository.preferences())
                        saver.rebase(values)
                        _uiState.update { it.copy(load = NotificationLoad.Loaded, values = values) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = NotificationLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        /** A toggle flip: updates the row at once and hands the newest map to the debounce. */
        fun onToggle(
            key: String,
            on: Boolean,
        ) {
            val newValues = _uiState.value.values + (key to on)
            _uiState.update { it.copy(values = newValues) }
            saver.onEdited(newValues)
        }
    }
