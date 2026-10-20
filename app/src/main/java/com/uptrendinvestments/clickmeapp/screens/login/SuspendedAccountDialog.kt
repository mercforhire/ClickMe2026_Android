package com.uptrendinvestments.clickmeapp.screens.login

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.config.AppLinks

/**
 * Shown over Login when the account was suspended at launch or mid-session (FND-06, D-08).
 * iOS has no suspension UI; the copy follows backend USER-REPORTING.md section 4.
 */
@Composable
fun SuspendedAccountDialog(
    onDismiss: () -> Unit,
    onContactSupport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.suspended_title)) },
        text = { Text(stringResource(R.string.suspended_body, AppLinks.SUPPORT_EMAIL)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.suspended_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onContactSupport) { Text(stringResource(R.string.suspended_contact_support)) }
        },
    )
}

/** Compose-a-mail intent to the support address; static, no user data attached. */
fun supportEmailIntent(): Intent = Intent(Intent.ACTION_SENDTO, "mailto:${AppLinks.SUPPORT_EMAIL}".toUri())
