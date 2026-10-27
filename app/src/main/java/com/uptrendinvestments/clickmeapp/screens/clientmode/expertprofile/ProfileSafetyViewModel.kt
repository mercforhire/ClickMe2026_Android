package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.safety.REPORT_DETAILS_MAX
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.SafetyDialog
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import com.uptrendinvestments.clickmeapp.screens.chat.isAccountSuspended
import com.uptrendinvestments.clickmeapp.screens.chat.toBlockFailure
import com.uptrendinvestments.clickmeapp.screens.chat.toChatEntryError
import com.uptrendinvestments.clickmeapp.screens.chat.toReportFailure
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
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
    val blockConfirmOpen: Boolean = false,
    val blockInFlight: Boolean = false,
    val dialog: SafetyDialog? = null,
    val snackbar: ProfileSafetySnackbar? = null,
)

/**
 * Report and Block from the Expert Profile (SAFE-01, SAFE-02). Report posts `POST users/{id}/report` with the enum
 * wire reason. The open sheet, the chosen reason and the details are mirrored to [SavedStateHandle] so they survive
 * rotation and process death. The details are report text: they are never logged.
 *
 * Block has no endpoint on a user id, so it initiates the thread first and then blocks it (D-05).
 */
@HiltViewModel
class ProfileSafetyViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
        private val sessionState: ChatSessionState,
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

        /** Closes the sheet. Ignored while a report is in flight so the request cannot be abandoned half way. */
        fun dismissReport() {
            if (_state.value.reportSubmitting) return
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
                    // The sheet and the inputs stay as they are so the user can retry.
                    _state.update {
                        it.copy(
                            reportSubmitting = false,
                            dialog = failureDialog(R.string.report_error_title, e, ApiException::toReportFailure),
                        )
                    }
                }
            }
        }

        fun openBlock() {
            _state.update { it.copy(blockConfirmOpen = true) }
        }

        fun dismissBlock() {
            _state.update { if (it.blockInFlight) it else it.copy(blockConfirmOpen = false) }
        }

        /**
         * Blocks [expertId] after the confirm dialog: `chats/initiate` (idempotent, D-05) then the block action on the
         * returned thread. A re-block is a 200 no-op on the server, so no block state is read (D-07). Single flight.
         * On success the thread is marked blocked by me for this session, which covers the gap where an empty thread
         * is not in `GET /chats` yet, so a later Message shows the blocked state (A4, RESEARCH Pitfall 2). The user
         * stays on the profile.
         */
        fun confirmBlock(
            expertId: String,
            name: String?,
        ) {
            if (_state.value.blockInFlight) return
            _state.update { it.copy(blockInFlight = true) }
            viewModelScope.launch {
                try {
                    val threadId = initiate(expertId) ?: return@launch
                    repository.block(threadId)
                    sessionState.markBlockedByMe(threadId)
                    _state.update {
                        it.copy(
                            blockInFlight = false,
                            blockConfirmOpen = false,
                            snackbar = ProfileSafetySnackbar.Blocked(name?.takeIf { n -> n.isNotBlank() }),
                        )
                    }
                } catch (e: ApiException) {
                    _state.update {
                        it.copy(
                            blockInFlight = false,
                            blockConfirmOpen = false,
                            dialog = failureDialog(R.string.block_error_title, e, ApiException::toBlockFailure),
                        )
                    }
                }
            }
        }

        /** The thread id for [expertId], or null after surfacing an initiate failure (404 is "unavailable"). */
        private suspend fun initiate(expertId: String): String? =
            try {
                repository.initiate(expertId).threadId
            } catch (e: ApiException) {
                _state.update {
                    it.copy(
                        blockInFlight = false,
                        blockConfirmOpen = false,
                        dialog = failureDialog(R.string.block_error_title, e, ApiException::toChatEntryError),
                    )
                }
                null
            }

        /** The error dialog for [e], or null when the global suspended-account dialog owns the failure. */
        private fun failureDialog(
            titleRes: Int,
            e: ApiException,
            message: (ApiException) -> FieldError,
        ): SafetyDialog? = if (e.isAccountSuspended()) null else SafetyDialog(titleRes, message(e))

        fun dismissDialog() {
            _state.update { it.copy(dialog = null) }
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
