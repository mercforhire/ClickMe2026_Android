package com.uptrendinvestments.clickmeapp.screens.chat

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429

/** A safety-action error dialog: a title resource and the message to show. Shared with the Expert Profile (12-10). */
data class SafetyDialog(
    @StringRes val titleRes: Int,
    val message: FieldError,
)

/** True when the failure is the account-suspended answer, which the global suspended-account dialog owns. */
fun ApiException.isAccountSuspended(): Boolean = this is ApiException.Http && code == ErrorCode.AccountSuspended

/**
 * Message for a failed report (chat action or profile report), per the RESEARCH Error Mapping table: 429 (the rate
 * limiter, with or without `TOO_MANY_REQUESTS`) and 404 have fixed copy, everything else shows the server message or the
 * generic line, and a network failure shows the connection line. `403 ACCOUNT_SUSPENDED` is handled globally
 * (SuspensionInterceptor), and 422 `CANNOT_REPORT_SELF` is unreachable because the self controls are hidden, so it
 * falls through to the server message. Branches on the code and status only; server text is wrapped, never parsed.
 */
fun ApiException.toReportFailure(): FieldError =
    when (this) {
        is ApiException.Http ->
            when {
                status == HTTP_TOO_MANY_REQUESTS || code == ErrorCode.TooManyRequests ->
                    FieldError.Res(R.string.report_error_rate_limited)
                isNotFound() -> FieldError.Res(R.string.report_error_unavailable)
                else -> serverTextOrUnknown()
            }
        is ApiException.Network -> FieldError.Res(R.string.login_error_network)
        is ApiException.Decoding -> FieldError.Res(R.string.login_error_unknown)
    }

/**
 * Message for a failed block: 404 means the conversation is gone, otherwise the server message or the generic line.
 * 422 `CANNOT_BLOCK_SELF` is unreachable (self controls are hidden) and shows the server message.
 */
fun ApiException.toBlockFailure(): FieldError =
    when (this) {
        is ApiException.Http ->
            if (isNotFound()) {
                FieldError.Res(R.string.block_error_conversation_unavailable)
            } else {
                serverTextOrUnknown()
            }
        is ApiException.Network -> FieldError.Res(R.string.login_error_network)
        is ApiException.Decoding -> FieldError.Res(R.string.login_error_unknown)
    }

/**
 * Message for a failed unblock: `403 NOT_BLOCKER` shows the server message (or the fixed copy when the server sent
 * none), 404 means the conversation is gone, otherwise the server message or the generic line.
 */
fun ApiException.toUnblockFailure(): FieldError =
    when (this) {
        is ApiException.Http ->
            when {
                code == ErrorCode.NotBlocker ->
                    serverMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.unblock_error_not_blocker)
                isNotFound() -> FieldError.Res(R.string.block_error_conversation_unavailable)
                else -> serverTextOrUnknown()
            }
        is ApiException.Network -> FieldError.Res(R.string.login_error_network)
        is ApiException.Decoding -> FieldError.Res(R.string.login_error_unknown)
    }

private fun ApiException.Http.isNotFound(): Boolean = status == HTTP_NOT_FOUND || code == ErrorCode.NotFound

private fun ApiException.Http.serverTextOrUnknown(): FieldError =
    userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)
