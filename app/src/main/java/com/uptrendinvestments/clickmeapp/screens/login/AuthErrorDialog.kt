package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R

/**
 * Shared error dialog for auth failures (iOS alert, CLAUDE.md swap to Material AlertDialog).
 * [title] defaults to "Something went wrong"; callers may pass the UI-SPEC titles such as
 * "Couldn't create account" or "Couldn't save". The scrim tap, Back and OK all call [onDismiss]. [selectable] makes
 * the message copyable (the booking dialog's payment reference, 07-13); the default keeps every caller unchanged.
 * [confirmLabel] renames the single button (Write Review uses "Edit review"); the default is "OK".
 */
@Composable
fun AuthErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.auth_dialog_title),
    selectable: Boolean = false,
    confirmLabel: String = stringResource(R.string.auth_dialog_ok),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            val body: @Composable () -> Unit = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selectable) SelectionContainer(content = body) else body()
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}
