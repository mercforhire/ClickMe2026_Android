package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallDialog
import com.uptrendinvestments.clickmeapp.managers.GuardMessage
import com.uptrendinvestments.clickmeapp.managers.JoinErrorReason
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog

const val CALL_DIALOG_OPEN_SETTINGS_TAG = "call_dialog_open_settings"
const val CALL_DIALOG_RATIONALE_CONTINUE_TAG = "call_dialog_rationale_continue"
const val CALL_DIALOG_RATIONALE_NOT_NOW_TAG = "call_dialog_rationale_not_now"

/** Body text of the join-error dialog. A server message is shown as plain text (T-09-35). */
@Composable
fun joinErrorMessage(reason: JoinErrorReason): String =
    when (reason) {
        is JoinErrorReason.Server -> reason.message
        JoinErrorReason.NotSetUpForInAppVoice -> stringResource(R.string.call_join_error_not_in_app)
        JoinErrorReason.MissingMeetingLink -> stringResource(R.string.call_join_error_missing_link)
        JoinErrorReason.InvalidMeetingLink -> stringResource(R.string.call_join_error_invalid_link)
        JoinErrorReason.Network -> stringResource(R.string.login_error_network)
        JoinErrorReason.Unknown -> stringResource(R.string.call_join_error_unknown)
    }

/** The action phrase dropped into the guard sentence ("switch modes", "log out", ...). */
@Composable
fun restrictedActionText(action: RestrictedAction): String =
    stringResource(
        when (action) {
            RestrictedAction.SwitchModes -> R.string.call_action_switch_modes
            RestrictedAction.LogOut -> R.string.call_action_log_out
            RestrictedAction.AddPaymentMethod -> R.string.call_action_add_payment_method
            RestrictedAction.CancelBooking -> R.string.call_action_cancel_booking
            RestrictedAction.EditAvailability -> R.string.call_action_edit_availability
            RestrictedAction.SetUpPayouts -> R.string.call_action_set_up_payouts
            RestrictedAction.RespondToReschedule -> R.string.call_action_respond_reschedule
            RestrictedAction.RescheduleBooking -> R.string.call_action_reschedule_booking
            RestrictedAction.ChangePassword -> R.string.call_action_change_password
            RestrictedAction.DeleteAccount -> R.string.call_action_delete_account
        },
    )

/** Body text of the guard dialog (D-12). */
@Composable
fun guardMessageText(message: GuardMessage): String =
    when (message) {
        is GuardMessage.EndCallTo ->
            stringResource(R.string.call_guard_end_call_to, restrictedActionText(message.action))
        is GuardMessage.SessionInProgress ->
            stringResource(R.string.call_guard_session_in_progress, restrictedActionText(message.action))
        GuardMessage.AlreadyOnCall -> stringResource(R.string.call_guard_already_on_call)
    }

/** App-details settings page for our own package only; no server data reaches the intent (T-09-33). */
fun appDetailsSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

/**
 * Renders the head of the call dialog queue, or nothing when [dialog] is null. The caller passes only one
 * dialog at a time (first in, first shown).
 */
@Composable
fun CallDialogHost(
    dialog: CallDialog?,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        is CallDialog.JoinError ->
            AuthErrorDialog(
                message = joinErrorMessage(dialog.reason),
                onDismiss = onDismiss,
                title = stringResource(R.string.call_join_error_title),
            )
        is CallDialog.Guard ->
            AuthErrorDialog(
                message = guardMessageText(dialog.message),
                onDismiss = onDismiss,
                title = stringResource(R.string.call_guard_title),
            )
        CallDialog.TimeLimit ->
            AuthErrorDialog(
                message = stringResource(R.string.call_time_limit_message),
                onDismiss = onDismiss,
                title = stringResource(R.string.call_time_limit_title),
            )
        CallDialog.ExternalLinkFailed ->
            AuthErrorDialog(
                message = stringResource(R.string.call_external_open_failed_message),
                onDismiss = onDismiss,
                title = stringResource(R.string.call_external_open_failed_title),
            )
        CallDialog.MicPermissionDenied -> MicDeniedDialog(onDismiss = onDismiss)
    }
}

@Composable
private fun MicDeniedDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    MicAlertDialog(
        message = stringResource(R.string.call_mic_denied_message),
        confirmText = stringResource(R.string.call_mic_open_settings),
        dismissText = stringResource(R.string.call_mic_cancel),
        confirmTag = CALL_DIALOG_OPEN_SETTINGS_TAG,
        dismissTag = null,
        onConfirm = {
            runCatching { context.startActivity(settingsIntentFor(context)) }
            onDismiss()
        },
        onDismissClick = onDismiss,
        onDismissRequest = onDismiss,
    )
}

/** Asks for the mic once more after a first denial, before the system prompt (UI-SPEC A4). */
@Composable
fun MicRationaleDialog(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
) {
    MicAlertDialog(
        message = stringResource(R.string.call_mic_rationale_message),
        confirmText = stringResource(R.string.call_mic_continue),
        dismissText = stringResource(R.string.call_mic_not_now),
        confirmTag = CALL_DIALOG_RATIONALE_CONTINUE_TAG,
        dismissTag = CALL_DIALOG_RATIONALE_NOT_NOW_TAG,
        onConfirm = onContinue,
        onDismissClick = onNotNow,
        onDismissRequest = onNotNow,
    )
}

private fun settingsIntentFor(context: Context): Intent {
    val intent = appDetailsSettingsIntent(context.packageName)
    return if (context is Activity) intent else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

@Composable
private fun MicAlertDialog(
    message: String,
    confirmText: String,
    dismissText: String,
    confirmTag: String,
    dismissTag: String?,
    onConfirm: () -> Unit,
    onDismissClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.call_mic_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(confirmTag)) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissClick,
                modifier = if (dismissTag != null) Modifier.testTag(dismissTag) else Modifier,
            ) {
                Text(
                    text = dismissText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}
