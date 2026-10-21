package com.uptrendinvestments.clickmeapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.navigation.AuthNavHost
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertScreen
import com.uptrendinvestments.clickmeapp.screens.login.SuspendedAccountDialog
import com.uptrendinvestments.clickmeapp.screens.login.supportEmailIntent
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupPlaceholder
import com.uptrendinvestments.clickmeapp.ui.theme.Motion

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
    // Only a client <-> expert swap fades through; every other root change stays instant (UI-SPEC Motion).
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            if (initialState in HOMES && targetState in HOMES) {
                fadeIn(tween(Motion.TAB_FADE_IN_MS, delayMillis = Motion.TAB_FADE_OUT_MS)) togetherWith
                    fadeOut(tween(Motion.TAB_FADE_OUT_MS))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "root-route",
    ) { targetRoute ->
        when (targetRoute) {
            // The OS splash covers Splash.
            AppRoute.Splash -> EmptySurface(modifier)

            AppRoute.Login -> AuthNavHost(onLoggedIn = appViewModel::onLoggedIn)

            // Replaced by the real signup setup hub in plan 04-19.
            AppRoute.SignupSetup -> SignupSetupPlaceholder(onLogout = appViewModel::logout)

            AppRoute.ClientHome -> HomeClientScreen(onSwitchMode = appViewModel::switchMode)

            AppRoute.ExpertHome -> HomeExpertScreen(onSwitchMode = appViewModel::switchMode)
        }
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

private val HOMES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome)

@Composable
private fun EmptySurface(modifier: Modifier) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
}
