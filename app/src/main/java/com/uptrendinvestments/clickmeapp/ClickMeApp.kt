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
import com.uptrendinvestments.clickmeapp.navigation.SignupSetupNavHost
import com.uptrendinvestments.clickmeapp.screens.call.calling.CallOverlayHost
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientScreen
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertScreen
import com.uptrendinvestments.clickmeapp.screens.login.SuspendedAccountDialog
import com.uptrendinvestments.clickmeapp.screens.login.supportEmailIntent
import com.uptrendinvestments.clickmeapp.screens.push.PushPermissionRequester
import com.uptrendinvestments.clickmeapp.screens.push.PushRoutingHost
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.AccountDeletionFarewellHost
import com.uptrendinvestments.clickmeapp.ui.theme.Motion

/**
 * Root switch on [AppRoute]. The route is app state owned by [AppViewModel], not a navigation
 * destination (D-01), so this is a plain `when`.
 *
 * The route switch is wrapped in [CallOverlayHost], the root call overlay: the expanded call and the pill sit
 * above every route and survive tab changes and the Client/Expert switch because the call lives in `CallCenter`
 * (D-08, CALL-04). The suspended-account dialog stays outside it. The root push router lives here too, because a
 * tapped notification may need a mode switch before any shell exists (RESEARCH F-7).
 *
 * The account-deleted Farewell is rendered last, above the route switch, because deleting an account revokes every
 * session and the resulting forced logout changes the route to Login underneath it (RESEARCH Pitfall 3).
 */
@Composable
fun ClickMeApp(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val route by appViewModel.route.collectAsStateWithLifecycle()
    val showSuspendedDialog by appViewModel.showSuspendedDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    PushRoutingHost(route = route, onSwitchMode = appViewModel::switchMode)
    PushPermissionRequester(route = route)
    Box(modifier.fillMaxSize()) {
        // Only a client <-> expert swap fades through; every other root change stays instant (UI-SPEC Motion).
        CallOverlayHost {
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
                    AppRoute.Splash -> EmptySurface()

                    AppRoute.Login -> AuthNavHost(onLoggedIn = appViewModel::onLoggedIn)

                    // Publish success sets mode Client and routes to client home (D-22).
                    AppRoute.SignupSetup ->
                        SignupSetupNavHost(
                            onPublished = appViewModel::onSetupPublished,
                            onNotNow = appViewModel::onSetupDeferred,
                        )

                    AppRoute.ClientHome -> HomeClientScreen(onSwitchMode = appViewModel::switchMode)

                    AppRoute.ExpertHome -> HomeExpertScreen(onSwitchMode = appViewModel::switchMode)
                }
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

        // Last child, so it draws above the route switch and the call overlay. A forced logout after the account
        // deletion swaps the route underneath it without removing it (RESEARCH Pitfall 3).
        AccountDeletionFarewellHost()
    }
}

private val HOMES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome)

@Composable
private fun EmptySurface() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
}
