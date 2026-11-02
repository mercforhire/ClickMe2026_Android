package com.uptrendinvestments.clickmeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.splash.SplashPlayedFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * The route the UI shows: [AppRoute.Splash] while the Compose splash holds, else the resolved [route]. [route]
 * itself keeps meaning "resolved destination", so [AppViewModel.destinationResolved] and the push router's
 * contract are unchanged.
 */
internal fun shownRootRoute(
    route: AppRoute,
    held: Boolean,
): AppRoute = if (held) AppRoute.Splash else route

/**
 * Holds the root [AppRoute] and the splash hold condition, driven by the session in [UserManager].
 *
 * Launch is session-based auto-login (FND-07): [UserManager.restore] runs while the OS splash is up and
 * the splash is released as soon as it answers, or after [SPLASH_HOLD_CAP_MS] at the latest (D-02). On
 * timeout the route comes from [UserManager.offlineDestination] (cached home or Login) and the restore
 * keeps running; its late result never changes the route, except that a session that ends afterwards
 * (rejection, suspension, logout) sends the app to Login through the [UserManager.isLoggedIn] collector.
 * The cap path marks the setup check pending, so the next foreground applies a late "setup incomplete"
 * answer (D-11, see [onAppForegrounded]).
 *
 * While on a home route the root route follows [UserManager.currentMode] (D-17): the mode switch, the
 * post-publish landing and relaunch restore all flow through that one collector.
 *
 * The OS splash is released by [keepSystemSplash], at the first composition of the Compose splash; the Compose
 * splash then stays for [SPLASH_MIN_DISPLAY_MS] (D-08) while auth runs alongside. [destinationResolved] still
 * says when [route] holds the resolved destination.
 */
@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
        private val callGuard: CallGuard,
        private val splashPlayed: SplashPlayedFlag = SplashPlayedFlag(),
    ) : ViewModel() {
        private val _route = MutableStateFlow(AppRoute.Splash)
        val route: StateFlow<AppRoute> = _route.asStateFlow()

        private var switchJob: Job? = null

        private val _destinationResolved = MutableStateFlow(false)
        val destinationResolved: StateFlow<Boolean> = _destinationResolved.asStateFlow()

        /** True while the "account suspended" dialog should show (D-08). */
        val showSuspendedDialog: StateFlow<Boolean> = userManager.wasSuspended

        @Volatile
        private var splashComposed = false
        private val _splashHeld = MutableStateFlow(!splashPlayed.played)
        private val _showsAnimatedSplash = MutableStateFlow(!splashPlayed.played)
        private var holdJob: Job? = null

        /**
         * True until [SPLASH_MIN_DISPLAY_MS] after the Compose splash first composed (D-08). The UI shows
         * [shownRootRoute] of [route] and this flag, so [route] keeps meaning "resolved destination".
         */
        val splashHeld: StateFlow<Boolean> = _splashHeld.asStateFlow()

        /**
         * True when this launch plays the Compose splash. False when the process already served it (a new activity
         * in a live process) or the activity was restored (A10): those launches keep the OS splash until the route
         * resolves and show an empty surface under it.
         */
        val showsAnimatedSplash: StateFlow<Boolean> = _showsAnimatedSplash.asStateFlow()

        /**
         * The keep condition for the OS splash. It flips at composition, never at first draw, because
         * core-splashscreen 1.2.0 cancels draws in an `OnPreDrawListener` while the condition is true, so a
         * condition that waits for a draw would never flip (RESEARCH Pitfall 1).
         */
        fun keepSystemSplash(): Boolean =
            if (_showsAnimatedSplash.value) !splashComposed else !_destinationResolved.value

        /**
         * Called from `onCreate` with a non-null saved state. A rotation keeps this ViewModel and its running
         * hold, so it is a no-op there; a process restore has a new ViewModel and is treated as already served,
         * so a later activity in this process does not play the splash either.
         */
        fun onActivityRestored() {
            if (holdJob != null) return
            _showsAnimatedSplash.value = false
            _splashHeld.value = false
            splashPlayed.played = true
        }

        /**
         * Starts the minimum display time once, when the Compose splash first composes. Auth keeps running from
         * `init`, so a slow `/me` is not stacked on top of the hold.
         */
        fun onSplashComposed() {
            if (holdJob != null || !_showsAnimatedSplash.value) return
            splashComposed = true
            splashPlayed.played = true
            holdJob =
                viewModelScope.launch {
                    delay(SPLASH_MIN_DISPLAY_MS)
                    _splashHeld.value = false
                }
        }

        init {
            // Mirrors iOS onChange(of: isLoggedIn): a session that ends while on a protected route goes to Login.
            viewModelScope.launch {
                userManager.isLoggedIn.collect { loggedIn ->
                    if (!loggedIn) switchJob?.cancel()
                    if (!loggedIn && _route.value in SESSION_ROUTES) {
                        _route.value = AppRoute.Login
                    }
                }
            }
            // D-17: follow the mode while on a home route. Splash, Login and SignupSetup are ignored because
            // restore, login routing and logout also set the mode.
            viewModelScope.launch {
                userManager.currentMode.collect { mode ->
                    if (userManager.isLoggedIn.value && _route.value in HOME_ROUTES) {
                        _route.value = mode.homeRoute()
                    }
                }
            }
            resolveInitialRoute()
        }

        /**
         * Starts auto-login. The restore runs in its own coroutine so the cap only stops the waiting: a
         * cancelled refresh could lose a rotated refresh token (RESEARCH Pitfall 3).
         */
        private fun resolveInitialRoute() {
            val restoreJob = viewModelScope.async { userManager.restore() }
            viewModelScope.launch {
                val destination =
                    withTimeoutOrNull(SPLASH_HOLD_CAP_MS) { restoreJob.await() }
                        ?: userManager.offlineDestination()
                _route.value = destination.toRoute()
                _destinationResolved.value = true
            }
        }

        /** Applies the destination the login screen resolved for a freshly authenticated user. */
        fun onLoggedIn(destination: LaunchDestination) {
            _route.value = destination.toRoute()
        }

        /** Dismissing the suspension dialog clears the single-shot flag (iOS clearSuspensionFlag). */
        fun onSuspendedDialogDismissed() {
            userManager.clearSuspensionFlag()
        }

        /**
         * Runs on every STARTED (D-01, D-11). First retries `/me` after a cached-path launch (the re-check's
         * role guard needs the roles), then, only while on a home route, re-checks a setup check that failed
         * or never ran and moves to [AppRoute.SignupSetup] when setup turns out to be incomplete. A foreground
         * during Splash or SignupSetup must not consume the pending flag.
         */
        fun onAppForegrounded() {
            viewModelScope.launch {
                userManager.refreshIfStale()
                if (_route.value !in HOME_ROUTES) return@launch
                val destination = userManager.recheckSetupIfPending()
                if (destination == LaunchDestination.SignupSetup && _route.value in HOME_ROUTES) {
                    _route.value = AppRoute.SignupSetup
                }
            }
        }

        /**
         * Switches mode (D-17). Cancels any in-flight switch so only the last request lands; the result is
         * applied only while the session is alive and the route is still a home route. `logout()` flips
         * [UserManager.isLoggedIn] off the main thread, so the switch can resume before the collector above
         * cancels it; the session check drops that late result (WR-01). Blocked during a call (D-13);
         * CallCenter shows the guard dialog.
         */
        fun switchMode(target: AppMode) {
            if (!callGuard.attempt(RestrictedAction.SwitchModes)) return
            switchJob?.cancel()
            switchJob =
                viewModelScope.launch {
                    val destination = userManager.requestModeSwitch(target)
                    if (userManager.isLoggedIn.value && _route.value in HOME_ROUTES) {
                        _route.value = destination.toRoute()
                    }
                }
        }

        /**
         * Signup setup was published (D-22): mode becomes Client and the route client home. A session that ended
         * while this ran (logout, rejection) keeps the Login route the isLoggedIn collector chose (WR-01).
         */
        fun onSetupPublished() {
            viewModelScope.launch {
                userManager.completeSetup()
                if (userManager.isLoggedIn.value) _route.value = AppRoute.ClientHome
            }
        }

        /**
         * "Not now" on the setup hub (D-27): mode becomes Client, the deferral is remembered for this login, and the
         * route goes to client home. A session that already ended keeps the Login route (WR-01).
         */
        fun onSetupDeferred() {
            viewModelScope.launch {
                if (userManager.deferSetup() && userManager.isLoggedIn.value) _route.value = AppRoute.ClientHome
            }
        }

        /**
         * Ends the session; the isLoggedIn collector then switches the route to Login (FND-08). Blocked during a
         * call (D-13); CallCenter shows the guard dialog.
         */
        fun logout() {
            if (!callGuard.attempt(RestrictedAction.LogOut)) return
            viewModelScope.launch { userManager.logout() }
        }

        private fun LaunchDestination.toRoute(): AppRoute =
            when (this) {
                LaunchDestination.Login -> AppRoute.Login
                LaunchDestination.SignupSetup -> AppRoute.SignupSetup
                is LaunchDestination.Home -> mode.homeRoute()
            }

        private fun AppMode.homeRoute(): AppRoute =
            when (this) {
                AppMode.Client -> AppRoute.ClientHome
                AppMode.Expert -> AppRoute.ExpertHome
            }

        companion object {
            /** Upper bound on how long the splash waits for auto-login (D-02). */
            const val SPLASH_HOLD_CAP_MS = 5_000L

            /**
             * Minimum time the Compose splash stays up on every cold start (D-08). Mirrors iOS
             * `SplashViewModel.displaySeconds`; the single constant to change.
             */
            const val SPLASH_MIN_DISPLAY_MS = 3_000L

            private val HOME_ROUTES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome)

            private val SESSION_ROUTES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome, AppRoute.SignupSetup)
        }
    }
