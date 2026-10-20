package com.uptrendinvestments.clickmeapp.screens.expertmode.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * State for the expert shell. Phase 1 has no profile, so [avatarUrl] stays null; the /me profile
 * feeds it once a session exists.
 */
@HiltViewModel
class HomeExpertViewModel
    @Inject
    constructor() : ViewModel() {
        private val _avatarUrl = MutableStateFlow<String?>(null)
        val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()
    }
