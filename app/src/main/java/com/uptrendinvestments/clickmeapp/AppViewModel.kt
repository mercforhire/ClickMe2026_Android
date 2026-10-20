package com.uptrendinvestments.clickmeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import dagger.hilt.android.lifecycle.HiltViewModel
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

        private val _destinationResolved = MutableStateFlow(false)
        val destinationResolved: StateFlow<Boolean> = _destinationResolved.asStateFlow()

        /** True while the "account suspended" dialog should show (D-08). */
        val showSuspendedDialog: StateFlow<Boolean> = userManager.wasSuspended

        init {
            // Mirrors iOS onChange(of: isLoggedIn): a session that ends while on a protected route goes to Login.
            viewModelScope.launch {
                userManager.isLoggedIn.collect { loggedIn ->
                    if (!loggedIn && _route.value in SESSION_ROUTES) {
                        _route.value = AppRoute.Login
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

        /** Retries `/me` after a cached-path launch each time the app returns to the foreground (D-01). */
        fun onAppForegrounded() {
            viewModelScope.launch { userManager.refreshIfStale() }
        }

        /** Ends the session; the isLoggedIn collector then switches the route to Login (FND-08). */
        fun logout() {
            viewModelScope.launch { userManager.logout() }
        }

        /**
         * Toggles between [AppRoute.ClientHome] and [AppRoute.ExpertHome]; other routes are unchanged.
         * Called only from debug-gated UI; Phase 4 replaces it with the real mode switch (ONB-02).
         */
        fun debugSwitchScaffold() {
            _route.value =
                when (_route.value) {
                    AppRoute.ClientHome -> AppRoute.ExpertHome
                    AppRoute.ExpertHome -> AppRoute.ClientHome
                    else -> _route.value
                }
        }

        private fun LaunchDestination.toRoute(): AppRoute =
            when (this) {
                LaunchDestination.Login -> AppRoute.Login
                LaunchDestination.SignupSetup -> AppRoute.SignupSetup
                is LaunchDestination.Home ->
                    when (mode) {
                        AppMode.Client -> AppRoute.ClientHome
                        AppMode.Expert -> AppRoute.ExpertHome
                    }
            }

        companion object {
            /** Upper bound on how long the splash waits for auto-login (D-02). */
            const val SPLASH_HOLD_CAP_MS = 5_000L

            private val SESSION_ROUTES = setOf(AppRoute.ClientHome, AppRoute.ExpertHome, AppRoute.SignupSetup)
        }
    }
