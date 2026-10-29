package com.uptrendinvestments.clickmeapp.screens.settings.help

import android.content.Intent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HelpContent(
    state: HelpUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureShown: () -> Unit,
    launchEmail: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { }
}
