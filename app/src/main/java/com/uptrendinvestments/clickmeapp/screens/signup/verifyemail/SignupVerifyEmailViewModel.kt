package com.uptrendinvestments.clickmeapp.screens.signup.verifyemail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.AuthValidators
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import com.uptrendinvestments.clickmeapp.screens.signup.VerificationSendState
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignupVerifyEmailUiState(
    val email: String = "",
    val verified: Boolean = false,
    val codeSent: Boolean = false,
    val code: String = "",
    val codeError: FieldError? = null,
    val dialogError: FieldError? = null,
    val dialogTitleRes: Int? = null,
    val sendState: SaveState = SaveState.Idle,
    val verifyState: SaveState = SaveState.Idle,
    val isResending: Boolean = false,
    val resendSecondsRemaining: Int = 0,
)

/**
 * Port of iOS `SignupVerifyEmailViewModel` with the backend's send-on-request contract: nothing is sent when the
 * step opens (D-07); the code goes out only from [send] or [resend]. A 429 `RATE_LIMITED` on send means a code
 * was sent moments ago and counts as sent (D-08).
 *
 * `codeSent` and the cooldown end time live in the singleton [SignupSetupRepository], so leaving and re-entering
 * the step cannot restart the 30 s window. They are also mirrored to [SavedStateHandle] so a process death does
 * not lose them (the repository is empty in a new process and is seeded from there). The end time is stored on
 * the injected [MonotonicClock], never as a remaining-seconds value. Only those two are mirrored: the typed code
 * is a secret, held in memory only and never saved, so after a process death the field comes back empty
 * (Phase 15 CONTEXT D-09).
 */
@HiltViewModel
class SignupVerifyEmailViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
        private val clock: MonotonicClock,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** Everything the repository does not own: typed code (memory only, D-09), errors and save states. */
        private data class Local(
            val code: String = "",
            val codeError: FieldError? = null,
            val dialogError: FieldError? = null,
            val dialogTitleRes: Int? = null,
            val sendState: SaveState = SaveState.Idle,
            val verifyState: SaveState = SaveState.Idle,
            val isResending: Boolean = false,
        )

        private val local = MutableStateFlow(Local())

        /** True only while the first send's "Email Sent!" confirmation is on screen. */
        private val holdingFirstSend = MutableStateFlow(false)
        private val secondsRemaining = MutableStateFlow(0)

        private val _done = MutableStateFlow(false)
        val done: StateFlow<Boolean> = _done.asStateFlow()

        val uiState: StateFlow<SignupVerifyEmailUiState>

        init {
            // A new process: the repository is empty but the saved state still knows a code was sent.
            if (!repository.verificationSend.value.codeSent && savedStateHandle.get<Boolean>(KEY_CODE_SENT) == true) {
                repository.recordCodeSent(savedStateHandle.get<Long>(KEY_COOLDOWN_END) ?: 0L)
            }
            mirror(repository.verificationSend.value)
            secondsRemaining.value = secondsLeft(repository.verificationSend.value.cooldownEndsAt)
            uiState =
                combine(
                    repository.draft,
                    repository.verificationSend,
                    local,
                    holdingFirstSend,
                    secondsRemaining,
                ) { draft, send, local, holding, seconds ->
                    compose(draft.email, draft.emailVerified, send, local, holding, seconds)
                }.stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    compose(
                        repository.draft.value.email,
                        repository.draft.value.emailVerified,
                        repository.verificationSend.value,
                        local.value,
                        holdingFirstSend.value,
                        secondsRemaining.value,
                    ),
                )
            viewModelScope.launch {
                repository.verificationSend
                    .map { it.cooldownEndsAt }
                    .distinctUntilChanged()
                    .collectLatest { end -> countDown(end) }
            }
        }

        private fun compose(
            email: String,
            emailVerified: Boolean,
            send: VerificationSendState,
            local: Local,
            holding: Boolean,
            seconds: Int,
        ) = SignupVerifyEmailUiState(
            email = email,
            // While the "Verified!" confirmation is showing, keep the form instead of flipping to the done variant.
            verified = emailVerified && local.verifyState == SaveState.Idle,
            codeSent = send.codeSent && !holding,
            code = local.code,
            codeError = local.codeError,
            dialogError = local.dialogError,
            dialogTitleRes = local.dialogTitleRes,
            sendState = local.sendState,
            verifyState = local.verifyState,
            isResending = local.isResending,
            resendSecondsRemaining = seconds,
        )

        private fun secondsLeft(endsAt: Long): Int {
            val left = endsAt - clock.nowMillis()
            return if (left <= 0) 0 else ((left + MS_PER_SECOND - 1) / MS_PER_SECOND).toInt()
        }

        /** Ticks [secondsRemaining] down to zero, sleeping exactly until the displayed second changes. */
        private suspend fun countDown(endsAt: Long) {
            while (true) {
                val seconds = secondsLeft(endsAt)
                secondsRemaining.value = seconds
                if (seconds == 0) return
                val untilChange = endsAt - clock.nowMillis() - (seconds - 1) * MS_PER_SECOND
                delay(untilChange.coerceAtLeast(1L))
            }
        }

        private fun mirror(send: VerificationSendState) {
            savedStateHandle[KEY_CODE_SENT] = send.codeSent
            savedStateHandle[KEY_COOLDOWN_END] = send.cooldownEndsAt
        }

        private fun cooldownActive(): Boolean = repository.verificationSend.value.cooldownEndsAt > clock.nowMillis()

        fun onCodeChange(raw: String) {
            val code = AuthValidators.sanitizeCode(raw)
            local.update { it.copy(code = code, codeError = null) }
        }

        fun dismissDialog() {
            local.update { it.copy(dialogError = null, dialogTitleRes = null) }
        }

        fun onDoneConsumed() {
            _done.value = false
        }

        /** The "Send Verification Email" button. Never called automatically (D-07). */
        fun send() = requestCode()

        /** The "Resend code" button, enabled only when the countdown reached zero. */
        fun resend() = requestCode()

        private fun requestCode() {
            if (repository.draft.value.emailVerified) return
            val current = local.value
            if (current.sendState == SaveState.Saving || current.isResending) return
            if (cooldownActive()) return
            val first = !repository.verificationSend.value.codeSent
            local.update {
                if (first) {
                    it.copy(sendState = SaveState.Saving, dialogError = null, dialogTitleRes = null)
                } else {
                    it.copy(isResending = true, dialogError = null, dialogTitleRes = null)
                }
            }
            viewModelScope.launch {
                val sent =
                    try {
                        repository.sendVerificationCode()
                        true
                    } catch (e: ApiException) {
                        if (e is ApiException.Http && e.code == ErrorCode.RateLimited) {
                            true
                        } else {
                            val dialog = e.toDialogError()
                            local.update {
                                it.copy(
                                    sendState = SaveState.Idle,
                                    isResending = false,
                                    dialogError = dialog,
                                    dialogTitleRes = dialog?.let { R.string.signup_verify_error_send_title },
                                )
                            }
                            false
                        }
                    }
                if (sent) onCodeSent(first)
            }
        }

        private suspend fun onCodeSent(first: Boolean) {
            if (first) holdingFirstSend.value = true
            // The server's 30 s window starts at the request, so measuring from the response never ends early.
            val send = VerificationSendState(codeSent = true, cooldownEndsAt = clock.nowMillis() + COOLDOWN_MS)
            mirror(send)
            repository.recordCodeSent(send.cooldownEndsAt)
            if (first) {
                local.update { it.copy(sendState = SaveState.Saved) }
                delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                holdingFirstSend.value = false
            } else {
                // A new code invalidates the old one.
                local.update { it.copy(isResending = false, code = "", codeError = null) }
            }
        }

        fun verify() {
            if (repository.draft.value.emailVerified) return
            val current = local.value
            if (current.verifyState != SaveState.Idle) return
            val codeError = AuthValidators.resetCode(current.code)
            if (codeError != null) {
                local.update { it.copy(codeError = codeError) }
                return
            }
            local.update { it.copy(verifyState = SaveState.Saving, codeError = null, dialogError = null) }
            viewModelScope.launch {
                try {
                    repository.verifyCode(current.code.trim())
                    local.update { it.copy(verifyState = SaveState.Saved) }
                    delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                    _done.value = true
                } catch (e: ApiException) {
                    local.update { it.copy(verifyState = SaveState.Idle) }
                    handleVerifyError(e)
                }
            }
        }

        /** No automatic retry: the server allows 5 attempts per 15 minutes (T-04-39). */
        private fun handleVerifyError(e: ApiException) {
            val http = e as? ApiException.Http
            when {
                http != null && (http.code == ErrorCode.InvalidCode || http.status == STATUS_UNPROCESSABLE) ->
                    local.update { it.copy(codeError = FieldError.Res(R.string.error_code_expired)) }
                http != null && (http.code == ErrorCode.TooManyRequests || http.status == STATUS_TOO_MANY) ->
                    local.update {
                        it.copy(
                            codeError =
                                http.serverMessage?.takeIf { m -> m.isNotBlank() }?.let { m -> FieldError.Text(m) }
                                    ?: FieldError.Res(R.string.signup_verify_error_too_many),
                        )
                    }
                else -> local.update { it.copy(dialogError = e.toDialogError(), dialogTitleRes = null) }
            }
        }

        companion object {
            const val COOLDOWN_MS = 30_000L
            private const val MS_PER_SECOND = 1_000L
            private const val STATUS_UNPROCESSABLE = 422
            private const val STATUS_TOO_MANY = 429
            private const val KEY_CODE_SENT = "verify_code_sent"
            private const val KEY_COOLDOWN_END = "verify_cooldown_end"
        }
    }
