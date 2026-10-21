package com.uptrendinvestments.clickmeapp.screens.clientmode.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the client shell. [avatarUrl] comes from the cached full profile held by
 * [UserManager] (null until a profile is cached). [logout] and [debugForceRefresh] back the Profile tab
 * placeholder buttons (D-09); the refresh check is only reachable from debug-gated UI.
 */
@HiltViewModel
class HomeClientViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
    ) : ViewModel() {
        val avatarUrl: StateFlow<String?> =
            userManager.profile
                .map { it?.personalDetails?.avatarUrl }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        /** Mirrors [UserManager.hasSeenOnboarding]; the carousel shows while this is false (D-21). */
        val hasSeenOnboarding: StateFlow<Boolean> = userManager.hasSeenOnboarding

        private val _debugRefreshStatus = MutableStateFlow<String?>(null)
        val debugRefreshStatus: StateFlow<String?> = _debugRefreshStatus.asStateFlow()

        fun markOnboardingShown() {
            viewModelScope.launch { userManager.markOnboardingShown() }
        }

        /** UserManager runs the logout non-cancellable, so the route change cannot interrupt it. */
        fun logout() {
            viewModelScope.launch { userManager.logout() }
        }

        fun debugForceRefresh() {
            viewModelScope.launch {
                _debugRefreshStatus.value = "Refreshing..."
                val result = userManager.debugForceTokenRefresh()
                _debugRefreshStatus.value =
                    result.fold(
                        onSuccess = { "Token refresh OK" },
                        onFailure = { "Refresh failed: ${it.message}" },
                    )
            }
        }
    }
