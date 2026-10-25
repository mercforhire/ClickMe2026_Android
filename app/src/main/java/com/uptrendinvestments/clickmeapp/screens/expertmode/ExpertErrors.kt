package com.uptrendinvestments.clickmeapp.screens.expertmode

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * Maps a failed expert-mode save or delete (Topics delete, Topic Editor save, Edit Expertise save, Availability
 * save) to the message for its error dialog, or null when nothing should be shown here.
 *
 * The first field-error message wins over the envelope message: [ApiException.userMessage] would show
 * "Validation failed" for a 422 envelope (RESEARCH Finding 1). An ACCOUNT_SUSPENDED response is owned by the
 * global suspended-account dialog (Phase 2 D-08), so it maps to null. Server text is returned as
 * [FieldError.Text] and must only be rendered as plain `Text` (T-10-13).
 */
fun ApiException.toExpertDialogError(): FieldError? =
    when (this) {
        is ApiException.Http ->
            if (code == ErrorCode.AccountSuspended) {
                null
            } else {
                val message =
                    fieldErrors.firstOrNull { it.message.isNotBlank() }?.message
                        ?: serverMessage?.takeIf { it.isNotBlank() }
                message?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.expert_error_generic)
            }
        is ApiException.Network -> FieldError.Res(R.string.login_error_network)
        is ApiException.Decoding -> FieldError.Res(R.string.expert_error_generic)
    }
