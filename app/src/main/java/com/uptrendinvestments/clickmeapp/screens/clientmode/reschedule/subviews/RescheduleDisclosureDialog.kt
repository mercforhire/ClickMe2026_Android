package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_DISCLOSURE_CONTINUE_TAG
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * "Your card hold will be released": the payment disclosure the client confirms before a reschedule request is sent
 * (D-11). It is shown from state before any request goes out, for the client only. [onContinue] is the only path that
 * sends; Cancel and a scrim tap call [onDismiss] and leave the picker untouched. When [chargesSoon] the body gains the
 * "may be charged sooner than 24 hours from now" caveat. The message scrolls so both buttons stay reachable at large
 * font scales.
 */
@Composable
fun RescheduleDisclosureDialog(
    chargesSoon: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.reschedule_start_disclosure_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.reschedule_start_disclosure_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (chargesSoon) {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(R.string.reschedule_accept_dialog_charges_soon),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.reschedule_dialog_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                modifier = Modifier.testTag(RESCHEDULE_DISCLOSURE_CONTINUE_TAG),
            ) {
                Text(
                    text = stringResource(R.string.reschedule_start_disclosure_continue),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}
