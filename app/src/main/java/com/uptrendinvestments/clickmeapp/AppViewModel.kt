package com.uptrendinvestments.clickmeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

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
 * [destinationResolved] feeds `setKeepOnScreenCondition` in MainActivity.
 */
@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
    ) : ViewModel() {
        private val _route = MutableStateFlow(AppRoute.Splash)
        val route: StateFlow<AppRoute> = _route.asStateFlow()

        private var switchJob: Job? = null

        private val _destinationResolved = MutableStateFlow(false)
        val destinationResolved: StateFlow<Boolean> = _destinationResolved.asStateFlow()

        /** True while the "account suspended" dialog should show (D-08). */
        val showSuspendedDialog: StateFlow<Boolean> = userManager.wasSuspended

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
         * cancels it; the session check drops that late result (WR-01).
         */
        fun switchMode(target: AppMode) {
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

        /** Ends the session; the isLoggedIn collector then switches the route to Login (FND-08). */
        fun logout() {
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

            private val HOME_ROUTES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome)

            private val SESSION_ROUTES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome, AppRoute.SignupSetup)
        }
    }
