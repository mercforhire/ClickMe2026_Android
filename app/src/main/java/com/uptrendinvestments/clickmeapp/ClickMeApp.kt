package com.uptrendinvestments.clickmeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertScreen
import com.uptrendinvestments.clickmeapp.screens.login.LoginScreen
import com.uptrendinvestments.clickmeapp.screens.login.SuspendedAccountDialog
import com.uptrendinvestments.clickmeapp.screens.login.supportEmailIntent
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupPlaceholder

/**
 * Root switch on [AppRoute]. The route is app state owned by [AppViewModel], not a navigation
 * destination (D-01), so this is a plain `when`.
 */
@Composable
fun ClickMeApp(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val route by appViewModel.route.collectAsStateWithLifecycle()
    val showSuspendedDialog by appViewModel.showSuspendedDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (route) {
        // The OS splash covers Splash.
        AppRoute.Splash -> EmptySurface(modifier)

        // Temporary login screen (D-07); Phase 3 (AUTH-01) replaces it.
        AppRoute.Login -> LoginScreen(onLoggedIn = appViewModel::onLoggedIn)

        // Placeholder until Phase 4 builds the signup flow (D-05).
        AppRoute.SignupSetup -> SignupSetupPlaceholder(onLogout = appViewModel::logout)

        AppRoute.ClientHome ->
            HomeClientScreen(onDebugSwitchScaffold = appViewModel::debugSwitchScaffold)

        AppRoute.ExpertHome ->
            HomeExpertScreen(onDebugSwitchScaffold = appViewModel::debugSwitchScaffold)
    }

    if (showSuspendedDialog) {
        SuspendedAccountDialog(
            onDismiss = appViewModel::onSuspendedDialogDismissed,
            onContactSupport = {
                // No mail app installed throws ActivityNotFoundException; nothing to recover.
                runCatching { context.startActivity(supportEmailIntent()) }
                appViewModel.onSuspendedDialogDismissed()
            },
        )
    }
}

@Composable
private fun EmptySurface(modifier: Modifier) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
}
