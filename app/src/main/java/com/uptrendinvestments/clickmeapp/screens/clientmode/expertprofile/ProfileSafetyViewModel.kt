package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.safety.REPORT_DETAILS_MAX
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A one-shot message the profile shows in its snackbar. */
sealed interface ProfileSafetySnackbar {
    /** The report was accepted: "Thanks, we'll review this." */
    data object ReportThanks : ProfileSafetySnackbar

    /** The expert was blocked: "You blocked {name}". */
    data class Blocked(
        val name: String?,
    ) : ProfileSafetySnackbar
}

/**
 * Everything the Expert Profile safety UI shows. [details] is stored untrimmed (it is trimmed on send by the
 * repository).
 */
data class ProfileSafetyState(
    val sheetOpen: Boolean = false,
    val reason: UserReportReason? = null,
    val details: String = "",
    val reportSubmitting: Boolean = false,
    val snackbar: ProfileSafetySnackbar? = null,
)

/**
 * Report from the Expert Profile (SAFE-01). Posts `POST users/{id}/report` with the enum wire reason. The open sheet,
 * the chosen reason and the details are mirrored to [SavedStateHandle] so they survive rotation and process death.
 * The details are report text: they are never logged.
 */
@HiltViewModel
class ProfileSafetyViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
    ) : ViewModel() {
        private val _state =
            MutableStateFlow(
                ProfileSafetyState(
                    sheetOpen = savedStateHandle.get<Boolean>(KEY_SHEET_OPEN) == true,
                    reason = UserReportReason.fromWire(savedStateHandle.get<String>(KEY_REASON)),
                    details = capText(savedStateHandle.get<String>(KEY_DETAILS).orEmpty(), REPORT_DETAILS_MAX),
                ),
            )
        val state: StateFlow<ProfileSafetyState> = _state.asStateFlow()

        fun openReport() {
            savedStateHandle[KEY_SHEET_OPEN] = true
            _state.update { it.copy(sheetOpen = true) }
        }

        fun dismissReport() {
            savedStateHandle[KEY_SHEET_OPEN] = false
            _state.update { it.copy(sheetOpen = false) }
        }

        fun selectReason(reason: UserReportReason) {
            if (_state.value.reportSubmitting) return
            savedStateHandle[KEY_REASON] = reason.wire
            _state.update { it.copy(reason = reason) }
        }

        fun onDetailsChange(text: String) {
            if (_state.value.reportSubmitting) return
            val capped = capText(text, REPORT_DETAILS_MAX)
            savedStateHandle[KEY_DETAILS] = capped
            _state.update { it.copy(details = capped) }
        }

        /**
         * Sends the report for [expertId]. Ignored without a reason and while a report is in flight. On success the
         * sheet closes, the inputs reset and the thank-you snackbar is set; the server answer is authoritative.
         */
        fun submitReport(expertId: String) {
            val snapshot = _state.value
            val reason = snapshot.reason ?: return
            if (snapshot.reportSubmitting) return
            _state.update { it.copy(reportSubmitting = true) }
            viewModelScope.launch {
                try {
                    repository.reportUser(expertId, reason, snapshot.details)
                    savedStateHandle[KEY_SHEET_OPEN] = false
                    savedStateHandle[KEY_REASON] = null
                    savedStateHandle[KEY_DETAILS] = null
                    _state.update {
                        it.copy(
                            sheetOpen = false,
                            reason = null,
                            details = "",
                            reportSubmitting = false,
                            snackbar = ProfileSafetySnackbar.ReportThanks,
                        )
                    }
                } catch (e: ApiException) {
                    // Failure copy is added with the error mapping; the inputs stay so the user can retry.
                    _state.update { it.copy(reportSubmitting = false) }
                }
            }
        }

        fun consumeSnackbar() {
            _state.update { it.copy(snackbar = null) }
        }

        private companion object {
            const val KEY_SHEET_OPEN = "sheet_open"
            const val KEY_REASON = "reason"
            const val KEY_DETAILS = "details"
        }
    }
