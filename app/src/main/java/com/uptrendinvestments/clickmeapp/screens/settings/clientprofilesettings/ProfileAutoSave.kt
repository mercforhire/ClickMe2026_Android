package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.network.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The four states of the app-bar save indicator. */
enum class AutoSaveStatus { Idle, Saving, Saved, Error }

/** Quiet period after the last edit before the PATCH is sent (iOS `autoSaveDebounce`). */
const val AUTO_SAVE_DEBOUNCE_MS = 1_200L

/** How long "Saved" stays visible before the indicator returns to idle. */
const val SAVED_HOLD_MS = 2_000L

/** Debounced auto-save engine (stub, implemented in the GREEN step). */
class AutoSaver<S : Any>(
    private val scope: CoroutineScope,
    private val flushScope: CoroutineScope,
    baseline: S,
    private val persist: suspend (old: S, new: S) -> Unit,
    private val onFailure: (ApiException) -> Unit,
) {
    private val _status = MutableStateFlow(AutoSaveStatus.Idle)
    val status: StateFlow<AutoSaveStatus> = _status.asStateFlow()

    fun rebase(snapshot: S) {
        TODO("not implemented")
    }

    fun onEdited(snapshot: S) {
        TODO("not implemented")
    }

    fun flush() {
        TODO("not implemented")
    }

    fun retry() {
        TODO("not implemented")
    }
}
