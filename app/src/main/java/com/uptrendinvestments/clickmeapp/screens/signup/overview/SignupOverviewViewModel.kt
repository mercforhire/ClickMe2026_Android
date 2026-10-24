package com.uptrendinvestments.clickmeapp.screens.signup.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.signup.HydrationState
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.screens.signup.SignupDraft
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One hub row: a setup step and whether its required fields are present. */
data class SetupStepRow(
    val step: SetupStep,
    val completed: Boolean,
)

data class SignupOverviewUiState(
    val rows: List<SetupStepRow> = emptyList(),
    val completedCount: Int = 0,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val showLogoutConfirm: Boolean = false,
)

/**
 * Port of iOS `SignupOverviewViewModel`. Rows and progress are derived from the shared repository draft (D-04),
 * so a step that saves elsewhere is reflected here without any local copy (T-04-31). The verified-email flag is
 * re-read each time the hub is shown (D-09).
 */
@HiltViewModel
class SignupOverviewViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
        private val userManager: UserManager,
        private val callGuard: CallGuard,
    ) : ViewModel() {
        private val failureAcknowledged = MutableStateFlow(false)
        private val showLogoutConfirm = MutableStateFlow(false)

        val uiState: StateFlow<SignupOverviewUiState> =
            combine(
                repository.draft,
                repository.hydration,
                failureAcknowledged,
                showLogoutConfirm,
            ) { draft, hydration, acknowledged, logoutConfirm ->
                buildState(draft, hydration, acknowledged, logoutConfirm)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    buildState(
                        repository.draft.value,
                        repository.hydration.value,
                        acknowledged = false,
                        logoutConfirm = false,
                    ),
            )

        init {
            // A new load attempt forgets that the previous failure was shown.
            repository.hydration
                .filter { it == HydrationState.Loading }
                .onEach { failureAcknowledged.value = false }
                .launchIn(viewModelScope)
        }

        /** Called every time the hub enters composition (first show and each return from a step). */
        fun onShown() {
            when (repository.hydration.value) {
                HydrationState.Loaded ->
                    viewModelScope.launch {
                        try {
                            repository.refreshEmailStatus()
                        } catch (e: ApiException) {
                            // Best effort: the rows keep their last known state.
                        }
                    }
                HydrationState.Failed -> viewModelScope.launch { repository.hydrate() }
                // The nav host's ensureHydrated owns the first load.
                HydrationState.Idle, HydrationState.Loading -> Unit
            }
        }

        /** Pull-to-refresh and the snackbar Retry. */
        fun refresh() {
            viewModelScope.launch { repository.hydrate() }
        }

        fun onRefreshErrorShown() {
            failureAcknowledged.value = true
        }

        /** Blocked during a call (D-13); CallCenter shows the guard dialog instead of the logout confirm. */
        fun requestLogout() {
            if (!callGuard.attempt(RestrictedAction.LogOut)) return
            showLogoutConfirm.value = true
        }

        fun dismissLogout() {
            showLogoutConfirm.value = false
        }

        fun confirmLogout() {
            showLogoutConfirm.value = false
            viewModelScope.launch { userManager.logout() }
        }

        private fun buildState(
            draft: SignupDraft,
            hydration: HydrationState,
            acknowledged: Boolean,
            logoutConfirm: Boolean,
        ) = SignupOverviewUiState(
            rows = SetupStep.entries.map { SetupStepRow(it, draft.isComplete(it)) },
            completedCount = draft.completedCount,
            isRefreshing = hydration == HydrationState.Loading,
            refreshFailed = hydration == HydrationState.Failed && !acknowledged,
            showLogoutConfirm = logoutConfirm,
        )
    }
