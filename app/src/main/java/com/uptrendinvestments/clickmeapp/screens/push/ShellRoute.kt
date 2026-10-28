package com.uptrendinvestments.clickmeapp.screens.push

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A resolved push destination, ready for a shell to insert: the key [keys] chain to put on [tab]'s stack in [mode]'s
 * shell (in order, so the last key is the screen the user lands on). Memory only, never serialized.
 */
data class ShellRoute(
    val mode: AppMode,
    val tab: NavKey,
    val keys: List<NavKey>,
)

/**
 * Stage 2 of the push hand-off (RESEARCH Pattern 2): the root host resolves a tapped notification and [post]s a
 * [ShellRoute]; the shell of the matching mode [consumeFor]s it and opens it with `Navigator.openRouted`.
 *
 * The slot is per mode in effect: during the Client/Expert cross-fade both shells are composed, so [consumeFor] takes
 * the route only when its mode matches the caller and otherwise leaves it pending for the other shell (RESEARCH F-7,
 * Pitfall 3). Newest wins when a second route is posted before the first is consumed.
 *
 * A [SessionDataCleaner], so a logout drops a resolved route that no shell consumed.
 */
@Singleton
class ShellRouteRequest
    @Inject
    constructor() : SessionDataCleaner {
        private val _pending = MutableStateFlow<ShellRoute?>(null)

        /** The route waiting for a shell, or null. */
        val pending: StateFlow<ShellRoute?> = _pending.asStateFlow()

        /** Posts [route], replacing any unconsumed one. */
        fun post(route: ShellRoute) {
            _pending.value = route
        }

        /**
         * Atomically takes the pending route when it belongs to [mode], clearing the slot. Returns null, and leaves a
         * route for another mode pending, otherwise.
         */
        fun consumeFor(mode: AppMode): ShellRoute? =
            _pending.getAndUpdate { if (it?.mode == mode) null else it }?.takeIf { it.mode == mode }

        override suspend fun clearSessionData() {
            _pending.value = null
        }
    }
