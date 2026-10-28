package com.uptrendinvestments.clickmeapp.screens.push

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the push route hand-off to the Hilt-aware home screens, which pass it into the Hilt-free shells (the
 * `ChatLaunchViewModel` shape). A shell consumes the slot only for its own mode.
 */
@HiltViewModel
class ShellRouteViewModel
    @Inject
    constructor(
        private val request: ShellRouteRequest,
    ) : ViewModel() {
        val pending: StateFlow<ShellRoute?> = request.pending

        /** Returns the pending route and clears it when it belongs to [mode], or null otherwise. */
        fun consumeFor(mode: AppMode): ShellRoute? = request.consumeFor(mode)
    }
