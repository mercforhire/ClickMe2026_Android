package com.uptrendinvestments.clickmeapp.services.call

import android.content.Context
import androidx.core.content.ContextCompat
import com.uptrendinvestments.clickmeapp.managers.CallForegroundController
import com.uptrendinvestments.clickmeapp.managers.CallNotificationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton the call foreground service observes, instead of a bound service (CONTEXT discretion: how the
 * service and CallCenter share state). CallCenter calls [startAndAwait] and [stop]; the service reports back
 * through [onServiceStarted], follows [active] to know when to tear itself down, and forwards the
 * notification's End call tap through [requestHangUp].
 *
 * The notification action only requests a hang-up. It never starts anything, so no service start is ever
 * needed from the background (RESEARCH Pitfall 1d).
 */
@Singleton
class CallForegroundBridge
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CallForegroundController {
        private val _active = MutableStateFlow(false)

        /** True from the moment a start is requested until [stop]; the service stops itself when it turns false. */
        val active: StateFlow<Boolean> = _active.asStateFlow()

        private val hangUpFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        @Volatile
        private var pendingStart: CompletableDeferred<Boolean>? = null

        override val hangUpRequests: Flow<Unit> = hangUpFlow.asSharedFlow()

        override suspend fun startAndAwait(info: CallNotificationInfo): Boolean {
            _active.value = true
            val deferred = CompletableDeferred<Boolean>()
            pendingStart = deferred
            try {
                ContextCompat.startForegroundService(context, CallService.startIntent(context, info))
            } catch (e: IllegalStateException) {
                // Includes ForegroundServiceStartNotAllowedException.
                pendingStart = null
                stop()
                return false
            } catch (e: SecurityException) {
                pendingStart = null
                stop()
                return false
            }
            val started =
                try {
                    withTimeoutOrNull(START_TIMEOUT_MS) { deferred.await() } ?: false
                } catch (e: CancellationException) {
                    pendingStart = null
                    stop()
                    throw e
                }
            pendingStart = null
            if (!started) stop()
            return started
        }

        /** Called by the service once `startForeground` succeeded (true) or was refused (false). */
        fun onServiceStarted(success: Boolean) {
            pendingStart?.complete(success)
        }

        /** Called by the service when the notification's End call action is tapped. */
        fun requestHangUp() {
            hangUpFlow.tryEmit(Unit)
        }

        override fun stop() {
            _active.value = false
        }

        companion object {
            const val START_TIMEOUT_MS = 3_000L
        }
    }
