package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.screens.login.LoginScreen
import com.uptrendinvestments.clickmeapp.screens.login.forgetpassword.ForgotPasswordScreen
import com.uptrendinvestments.clickmeapp.screens.login.passwordchanged.PasswordChangedScreen
import com.uptrendinvestments.clickmeapp.screens.login.setnewpassword.SetNewPasswordScreen
import com.uptrendinvestments.clickmeapp.screens.signup.initial.SignupInitialScreen

/**
 * The auth back stack that lives under `AppRoute.Login`: Login at the root with Forgot Password,
 * Set New Password, Password Changed and the Signup Initial step pushed on it (never bottom
 * sheets). Login success leaves through [onLoggedIn] to AppViewModel, which swaps the root route.
 * Each pushed entry gets its own ViewModel scope from the NavEntry decorators.
 */
@Composable
fun AuthNavHost(
    onLoggedIn: (LaunchDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberNavigationState(LoginKey, setOf(LoginKey))
    val navigator = remember(state) { Navigator(state) }
    val authNavigator = remember(navigator) { AuthNavigator(navigator) }

    // D-10: the reset email pre-fills Login; rememberSaveable keeps it across rotation.
    var loginPrefill by rememberSaveable { mutableStateOf<String?>(null) }
    val returnToLogin: (String) -> Unit = { loginPrefill = it }

    AuthNavDisplay(
        navigationState = state,
        authNavigator = authNavigator,
        onReturnToLogin = returnToLogin,
        modifier = modifier,
        entryProvider =
            entryProvider<NavKey> {
                entry<LoginKey> {
                    LoginScreen(
                        onLoggedIn = onLoggedIn,
                        onForgotPassword = authNavigator::openForgotPassword,
                        onSignUp = authNavigator::openSignUp,
                        prefillEmail = loginPrefill,
                        onPrefillConsumed = { loginPrefill = null },
                    )
                }
                entry<ForgotPasswordKey> { key ->
                    ForgotPasswordScreen(
                        initialEmail = key.email,
                        onCodeSent = authNavigator::openSetNewPassword,
                        onBack = authNavigator::back,
                    )
                }
                entry<SetNewPasswordKey> { key ->
                    SetNewPasswordScreen(
                        email = key.email,
                        onPasswordChanged = authNavigator::openPasswordChanged,
                        onBack = authNavigator::back,
                    )
                }
                entry<PasswordChangedKey> { key ->
                    PasswordChangedScreen(
                        onGoToLogin = {
                            authNavigator.returnToLogin()
                            returnToLogin(key.email)
                        },
                    )
                }
                // Signup Initial (SIGN-01). Success hands the root route to the setup hub via onLoggedIn,
                // which replaces this whole stack, so Initial cannot be resubmitted.
                entry<SignUpKey> {
                    SignupInitialScreen(onSignedUp = onLoggedIn, onBack = authNavigator::back)
                }
            },
    )
}

/**
 * Hilt-free NavDisplay over the auth stack so tests can drive it with stub entries. At the Login
 * root [AuthNavigator.handleBack] is a no-op and NavDisplay has a single entry, so system Back
 * exits the activity.
 */
@Composable
fun AuthNavDisplay(
    navigationState: NavigationState,
    authNavigator: AuthNavigator,
    onReturnToLogin: (String) -> Unit,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    val entries = navigationState.toDecoratedEntries(entryProvider)
    NavDisplay(
        entries = entries,
        onBack = { authNavigator.handleBack(onReturnToLogin) },
        modifier = modifier,
        transitionSpec = { slideTransition(forward = true) },
        popTransitionSpec = { slideTransition(forward = false) },
        predictivePopTransitionSpec = { slideTransition(forward = false) },
    )
}
