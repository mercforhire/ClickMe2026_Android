package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides when the one-time system notification prompt is due (Phase 13 D-01, D-02; iOS
 * `requestPushPermissionIfNeeded`). [needsPrompt] rises for every login or signup (each [sessionStarts] increment),
 * but only while the persisted "asked" flag is unset, so a later login, an account change or a restart never
 * prompts again. A session restore does not increment [sessionStarts] and so never prompts.
 *
 * The flag lives in [PushPreferences] (the `clickme_push` DataStore), which logout does not clear.
 */
@Singleton
class PushPermissionGate(
    sessionStarts: StateFlow<Int>,
    private val preferences: PushPreferences,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        userManager: UserManager,
        preferences: PushPreferences,
        @ApplicationScope scope: CoroutineScope,
    ) : this(userManager.sessionStarts, preferences, scope)

    private val _needsPrompt = MutableStateFlow(false)

    /** True from a login or signup until the prompt is answered, dismissed or found not applicable. */
    val needsPrompt: StateFlow<Boolean> = _needsPrompt.asStateFlow()

    init {
        scope.launch {
            // Starts at 0 so a login that happened before this gate existed still counts.
            var handled = 0
            sessionStarts.collect { count ->
                if (count > handled) {
                    handled = count
                    if (!preferences.notificationPromptAsked()) _needsPrompt.value = true
                }
            }
        }
    }

    /** The system prompt is about to show: persist "asked" first so nothing can ask twice (T-13-41). */
    fun onPromptLaunched() {
        scope.launch { preferences.markNotificationPromptAsked() }
    }

    /** The user answered the system prompt (either way). */
    fun onAnswered() {
        _needsPrompt.value = false
    }

    /** The permission was already granted (for example through the call prompt): record it as answered. */
    fun onAlreadyGranted() {
        _needsPrompt.value = false
        scope.launch { preferences.markNotificationPromptAsked() }
    }

    /** Below API 33 there is no runtime permission; nothing to ask. */
    fun onNotApplicable() {
        _needsPrompt.value = false
    }
}
