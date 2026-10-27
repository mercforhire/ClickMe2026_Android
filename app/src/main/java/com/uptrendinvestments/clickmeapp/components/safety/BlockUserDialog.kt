package com.uptrendinvestments.clickmeapp.components.safety

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.uptrendinvestments.clickmeapp.R

/** Test tags of the Block confirm dialog buttons. */
const val BLOCK_DIALOG_CONFIRM_TAG = "block_dialog_confirm"
const val BLOCK_DIALOG_CANCEL_TAG = "block_dialog_cancel"

private val SpinnerSize = 20.dp
private val SpinnerStroke = 2.dp

/**
 * "Block {name}?" confirmation (D-05, D-08), shared by the chat Report & Block screen and the Expert Profile sheet.
 * Default focus is Cancel. While [inFlight] the Block button shows a spinner and ignores taps, Cancel is disabled and
 * the dialog cannot be dismissed by Back or an outside tap.
 */
@Composable
fun BlockUserDialog(
    name: String,
    inFlight: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title =
        if (name.isBlank()) {
            stringResource(R.string.block_dialog_title_fallback)
        } else {
            stringResource(R.string.block_dialog_title, name)
        }
    val blocking = stringResource(R.string.block_in_flight)
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }
    AlertDialog(
        onDismissRequest = { if (!inFlight) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !inFlight, dismissOnClickOutside = !inFlight),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                text = stringResource(R.string.block_dialog_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(cancelFocus).testTag(BLOCK_DIALOG_CANCEL_TAG),
                enabled = !inFlight,
            ) {
                Text(
                    text = stringResource(R.string.block_dialog_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (!inFlight) onConfirm() },
                modifier =
                    Modifier
                        .testTag(BLOCK_DIALOG_CONFIRM_TAG)
                        .semantics { if (inFlight) stateDescription = blocking },
            ) {
                if (inFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SpinnerSize),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = SpinnerStroke,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.block_dialog_confirm),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}
