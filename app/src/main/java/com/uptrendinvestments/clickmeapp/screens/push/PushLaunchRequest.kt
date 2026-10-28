package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.expertmode.OneShotSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The hand-off from a tapped push to the screen shell that acts on it.
 *
 * There is one pending [destination]: the newest tap wins (D-15). It is memory only, so it survives the splash and the
 * session restore but not a process death, and a logout drops it through [SessionDataCleaner].
 *
 * [arrivals] is the iOS `newNotificationTick` (D-12): it increments for every push that arrives in the foreground so
 * open notification lists reload. Consumers skip its initial value.
 */
@Singleton
class PushLaunchRequest
    @Inject
    constructor() : SessionDataCleaner {
        val destination = OneShotSlot<PushDestination>()

        private val _arrivals = MutableStateFlow(0L)
        val arrivals: StateFlow<Long> = _arrivals.asStateFlow()

        /** Signals that a push arrived while the app was in the foreground. */
        fun onForegroundArrival() {
            _arrivals.update { it + 1 }
        }

        override suspend fun clearSessionData() {
            destination.consume()
        }
    }
