package com.uptrendinvestments.clickmeapp.screens.login

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException

/**
 * A user-facing error message. Local validation messages are string resources ([Res]); server messages
 * are shown verbatim ([Text]).
 */
sealed interface FieldError {
    data class Res(
        @StringRes val id: Int,
    ) : FieldError

    data class Text(
        val value: String,
    ) : FieldError
}

@Composable
fun FieldError.asString(): String =
    when (this) {
        is FieldError.Res -> stringResource(id)
        is FieldError.Text -> value
    }

/**
 * Maps a failure to the message for the shared error dialog, or null when nothing should be shown here:
 * an ACCOUNT_SUSPENDED response is owned by the global suspended-account dialog (Phase 2 D-08).
 */
fun ApiException.toDialogError(): FieldError? =
    when (this) {
        is ApiException.Http ->
            when {
                code == ErrorCode.AccountSuspended -> null
                else -> userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)
            }
        is ApiException.Network -> FieldError.Res(R.string.login_error_network)
        is ApiException.Decoding -> FieldError.Res(R.string.login_error_unknown)
    }

/** The message of the first field error whose field name is one of [fields], or null. */
fun ApiException.Http.fieldMessage(vararg fields: String): String? =
    fieldErrors.firstOrNull { it.field in fields }?.message
