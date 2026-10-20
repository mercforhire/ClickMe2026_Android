package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.AppMode

/**
 * Where the app should route after auto-login or login. Ports iOS `SplashDestination`
 * (`login`, `dashboard(AppMode)`, `setupIncomplete`). AppViewModel maps it to a route.
 */
sealed interface LaunchDestination {
    /** No usable session. */
    data object Login : LaunchDestination

    /** Expert whose `setup_completed` is false: resume the signup flow (D-05). */
    data object SignupSetup : LaunchDestination

    /** Land on the home scaffold of [mode]. */
    data class Home(
        val mode: AppMode,
    ) : LaunchDestination
}
