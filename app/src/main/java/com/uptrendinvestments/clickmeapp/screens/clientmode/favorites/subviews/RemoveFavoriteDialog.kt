package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R

/**
 * "Remove {name} from favorites?" confirmation (the iOS `.confirmationDialog`, D-10). Only [onConfirm] sends the
 * DELETE; back and a scrim tap call [onDismiss]. The name is inserted through a string format argument and rendered as
 * plain text, and the title wraps for long names.
 */
@Composable
fun RemoveFavoriteDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.favorites_remove_title, name),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.favorites_remove_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.favorites_remove_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.favorites_remove_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
