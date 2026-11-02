package com.uptrendinvestments.clickmeapp

/**
 * Root route of the app, mirroring iOS `AppRoute` in `ClickMe2026App.swift`.
 *
 * The root route is app state, not a navigation destination (D-01). [Splash] means "destination not resolved
 * yet". On a cold start `SplashScreen` shows it, held for at least 3 s through `shownRootRoute`; on a served
 * launch the OS splash covers it.
 */
enum class AppRoute {
    Splash,
    Login,
    SignupSetup,
    ClientHome,
    ExpertHome,
}
