package com.uptrendinvestments.clickmeapp.screens.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Host-level ViewModel of the setup back stack. It triggers the draft hydration when the stack first
 * composes, so a deep entry restored after process death fills from the server instead of acting on an
 * empty draft (D-10, RESEARCH Pitfall 5). The repository runs the fetch in its own scope and is
 * single-flight, so repeated calls are harmless.
 */
@HiltViewModel
class SignupSetupHostViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
    ) : ViewModel() {
        fun ensureHydrated() {
            viewModelScope.launch { repository.ensureHydrated() }
        }
    }
