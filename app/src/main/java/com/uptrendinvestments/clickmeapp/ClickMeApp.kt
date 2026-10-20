package com.uptrendinvestments.clickmeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertScreen

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
    when (route) {
        // The OS splash covers Splash; Login and SignupSetup are unreachable until Phases 3 and 4.
        AppRoute.Splash,
        AppRoute.Login,
        AppRoute.SignupSetup,
        -> EmptySurface(modifier)

        AppRoute.ClientHome ->
            HomeClientScreen(onDebugSwitchScaffold = appViewModel::debugSwitchScaffold)

        AppRoute.ExpertHome ->
            HomeExpertScreen(onDebugSwitchScaffold = appViewModel::debugSwitchScaffold)
    }
}

@Composable
private fun EmptySurface(modifier: Modifier) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
}
