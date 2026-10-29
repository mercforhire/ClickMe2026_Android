package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.network.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The four states of the app-bar save indicator. */
enum class AutoSaveStatus { Idle, Saving, Saved, Error }

/** Quiet period after the last edit before the PATCH is sent (iOS `autoSaveDebounce`). */
const val AUTO_SAVE_DEBOUNCE_MS = 1_200L

/** How long "Saved" stays visible before the indicator returns to idle. */
const val SAVED_HOLD_MS = 2_000L

/**
 * Debounced auto-save engine, a port of the iOS Profile Settings auto-save with its two bugs fixed: a pending edit is
 * flushed when the user leaves ([flush] runs on [flushScope], which outlives the screen) and the patch builder sends
 * every group that changed, languages included.
 *
 * It saves a snapshot of type [S] by calling [persist] with the last successfully saved snapshot and the newest one.
 * Rules:
 * - Every [onEdited] restarts a [AUTO_SAVE_DEBOUNCE_MS] timer; a burst of edits gives one save of the final value.
 * - A snapshot equal to the saved baseline is never persisted, nor is one that [sendsNothing] says would produce no
 *   request (for example a whitespace-only edit): it becomes the baseline silently, with no Saving/Saved flash.
 * - The baseline moves only after [persist] returns, so a failure keeps it and [retry] re-sends the newest snapshot.
 * - Saves are serialized by a mutex: two PATCHes never overlap, and an edit that lands mid-save is saved after it.
 * - [ApiException] sets [AutoSaveStatus.Error] and calls [onFailure]; cancellation always propagates.
 */
class AutoSaver<S : Any>(
    private val scope: CoroutineScope,
    private val flushScope: CoroutineScope,
    baseline: S,
    private val persist: suspend (old: S, new: S) -> Unit,
    private val onFailure: (ApiException) -> Unit,
    private val sendsNothing: (old: S, new: S) -> Boolean = { _, _ -> false },
) {
    private val _status = MutableStateFlow(AutoSaveStatus.Idle)
    val status: StateFlow<AutoSaveStatus> = _status.asStateFlow()

    private val saveLock = Mutex()

    @Volatile
    private var saved: S = baseline

    @Volatile
    private var latest: S = baseline

    private var debounceJob: Job? = null
    private var holdJob: Job? = null

    /** Adopts [snapshot] as both the saved baseline and the newest value, dropping any pending debounce. */
    fun rebase(snapshot: S) {
        debounceJob?.cancel()
        saved = snapshot
        latest = snapshot
    }

    /** Records the newest [snapshot] and (re)starts the debounce. */
    fun onEdited(snapshot: S) {
        latest = snapshot
        debounceJob?.cancel()
        debounceJob =
            scope.launch {
                delay(AUTO_SAVE_DEBOUNCE_MS)
                // A separate job, so a later edit cancelling this debounce never cancels a request in flight.
                scope.launch { save() }
            }
    }

    /**
     * Saves a pending edit now, on [flushScope] so leaving the screen cannot cancel it. A no-op when nothing differs
     * from the saved baseline.
     *
     * Known limitation: if this save fails, [onFailure] runs against a screen that is already gone, so the user is not
     * told (no app-level channel for such messages exists yet). The edit stays unsaved until the screen is opened again.
     */
    fun flush() {
        debounceJob?.cancel()
        if (latest == saved) return
        flushScope.launch { save() }
    }

    /** Re-sends the newest snapshot after a failure. */
    fun retry() {
        debounceJob?.cancel()
        scope.launch { save() }
    }

    private suspend fun save() {
        saveLock.withLock {
            val old = saved
            val new = latest
            if (new == old) {
                if (_status.value == AutoSaveStatus.Error) _status.value = AutoSaveStatus.Idle
                return
            }
            if (sendsNothing(old, new)) {
                saved = new
                if (_status.value == AutoSaveStatus.Error) _status.value = AutoSaveStatus.Idle
                return
            }
            holdJob?.cancel()
            _status.value = AutoSaveStatus.Saving
            try {
                persist(old, new)
            } catch (e: ApiException) {
                _status.value = AutoSaveStatus.Error
                onFailure(e)
                return
            }
            saved = new
            _status.value = AutoSaveStatus.Saved
            holdJob =
                scope.launch {
                    delay(SAVED_HOLD_MS)
                    if (_status.value == AutoSaveStatus.Saved) _status.value = AutoSaveStatus.Idle
                }
        }
    }
}
