package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import kotlinx.coroutines.launch

/**
 * Shows the shared "refresh failed" snackbar with a Retry action (CONTEXT D-05).
 *
 * At most one refresh-failed snackbar is visible: while one with the same [message] is showing this returns without
 * doing anything (a second failure is silent), and any other snackbar is dismissed first. [onRetry] runs only when
 * the user taps the action; a timeout or a dismissal never calls it.
 */
suspend fun SnackbarHostState.showRefreshFailed(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
) {
    val current = currentSnackbarData
    if (current?.visuals?.message == message) return
    current?.dismiss()
    val result =
        showSnackbar(
            message = message,
            actionLabel = retryLabel,
            withDismissAction = false,
            duration = SnackbarDuration.Long,
        )
    if (result == SnackbarResult.ActionPerformed) onRetry()
}

/**
 * Flag-and-consume wiring for a failed refresh with content showing (UI-SPEC "Failed refresh with content showing").
 *
 * When [refreshFailed] turns true the snackbar is shown on a separate scope and [onShown] runs immediately, so
 * consuming the flag in the ViewModel never cancels the snackbar (RESEARCH Pitfall 5). [onRetry] should be the
 * screen's pull-to-refresh action: Retry re-runs the same refresh and keeps what the user already has.
 */
@Composable
fun RefreshFailedSnackbarEffect(
    refreshFailed: Boolean,
    snackbarHostState: SnackbarHostState,
    onRetry: () -> Unit,
    onShown: () -> Unit,
    message: String = stringResource(R.string.common_refresh_failed),
    retryLabel: String = stringResource(R.string.section_retry),
) {
    val scope = rememberCoroutineScope()
    val currentOnRetry by rememberUpdatedState(onRetry)
    LaunchedEffect(refreshFailed) {
        if (refreshFailed) {
            scope.launch { snackbarHostState.showRefreshFailed(message, retryLabel) { currentOnRetry() } }
            onShown()
        }
    }
}
