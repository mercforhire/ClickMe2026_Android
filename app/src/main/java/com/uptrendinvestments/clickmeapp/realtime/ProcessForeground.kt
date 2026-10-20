package com.uptrendinvestments.clickmeapp.realtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the app process has a visible activity. A seam so the connection policy runs in JVM tests. */
interface ProcessForeground {
    val isForeground: StateFlow<Boolean>
}

/**
 * [ProcessForeground] backed by `ProcessLifecycleOwner` (D-14): foreground means the process lifecycle is
 * at least STARTED.
 *
 * Construct on the main thread (it is created by Hilt injection from `Application.onCreate`, Pitfall 8).
 * ON_STOP arrives about 700 ms after the last activity stops, so a quick app switch does not drop the
 * socket (A7). A process started by FCM without an activity is never STARTED, so it does not connect
 * (RESEARCH Open Question 4).
 *
 * Phase 9 note: the call foreground service will need an extra "keep alive" input OR-ed into the desired
 * condition. That condition lives in one place (`RealtimeConnectionManager.isDesired`), so it is a
 * one-line change later. Keep-alive is deliberately not implemented now.
 */
@Singleton
class LifecycleProcessForeground
    @Inject
    constructor(
        @ApplicationScope scope: CoroutineScope,
    ) : ProcessForeground {
        private val lifecycle = ProcessLifecycleOwner.get().lifecycle

        override val isForeground: StateFlow<Boolean> =
            lifecycle.currentStateFlow
                .map { it.isAtLeast(Lifecycle.State.STARTED) }
                .stateIn(
                    scope,
                    SharingStarted.Eagerly,
                    lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
                )
    }
