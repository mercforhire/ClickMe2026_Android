package com.uptrendinvestments.clickmeapp

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Holds the root [AppRoute] and the splash hold condition.
 *
 * [destinationResolved] feeds `setKeepOnScreenCondition` in MainActivity: the OS splash stays up
 * until it turns true, so there is no fixed delay.
 */
@HiltViewModel
class AppViewModel
    @Inject
    constructor() : ViewModel() {
        private val _route = MutableStateFlow(AppRoute.Splash)
        val route: StateFlow<AppRoute> = _route.asStateFlow()

        private val _destinationResolved = MutableStateFlow(false)
        val destinationResolved: StateFlow<Boolean> = _destinationResolved.asStateFlow()

        init {
            resolveInitialRoute()
        }

        /**
         * Phase 1 has no session, so the destination is the stub [AppRoute.ClientHome], resolved
         * synchronously (iOS's 3-second display time is intentionally not ported).
         *
         * This is the hook Phase 2 replaces with session-based auto-login (FND-07). Phase 2 must
         * also add an upper bound on how long the splash holds once resolution is asynchronous.
         */
        private fun resolveInitialRoute() {
            _route.value = AppRoute.ClientHome
            _destinationResolved.value = true
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
    }
