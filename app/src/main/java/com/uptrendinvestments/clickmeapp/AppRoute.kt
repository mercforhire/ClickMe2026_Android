package com.uptrendinvestments.clickmeapp

/**
 * Root route of the app, mirroring iOS `AppRoute` in `ClickMe2026App.swift`.
 *
 * The root route is app state, not a navigation destination (D-01). [Splash] means
 * "destination not resolved yet" and is covered by the OS splash screen.
 */
enum class AppRoute {
    Splash,
    Login,
    SignupSetup,
    ClientHome,
    ExpertHome,
}
