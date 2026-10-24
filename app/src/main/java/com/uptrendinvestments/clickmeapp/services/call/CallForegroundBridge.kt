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
import java.util.concurrent.atomic.AtomicLong
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
 *
 * Every start carries an attempt id. The service reports back with it, so a late callback from an earlier,
 * timed-out attempt never completes or tears down the current one (WR-05).
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

        private class PendingStart(
            val attempt: Long,
            val result: CompletableDeferred<Boolean>,
        )

        private val attemptIds = AtomicLong(NO_ATTEMPT)

        @Volatile
        private var currentAttempt: Long = NO_ATTEMPT

        @Volatile
        private var pendingStart: PendingStart? = null

        override val hangUpRequests: Flow<Unit> = hangUpFlow.asSharedFlow()

        override suspend fun startAndAwait(info: CallNotificationInfo): Boolean {
            val attempt = attemptIds.incrementAndGet()
            currentAttempt = attempt
            _active.value = true
            val pending = PendingStart(attempt, CompletableDeferred())
            pendingStart = pending
            try {
                ContextCompat.startForegroundService(context, CallService.startIntent(context, info, attempt))
            } catch (e: IllegalStateException) {
                // Includes ForegroundServiceStartNotAllowedException.
                clearPending(pending)
                stop()
                return false
            } catch (e: SecurityException) {
                clearPending(pending)
                stop()
                return false
            }
            val started =
                try {
                    withTimeoutOrNull(START_TIMEOUT_MS) { pending.result.await() } ?: false
                } catch (e: CancellationException) {
                    clearPending(pending)
                    stop()
                    throw e
                }
            clearPending(pending)
            if (!started) stop()
            return started
        }

        private fun clearPending(pending: PendingStart) {
            if (pendingStart === pending) pendingStart = null
        }

        /**
         * Called by the service once `startForeground` succeeded (true) or was refused (false) for [attempt].
         * A report for any other attempt is ignored.
         */
        fun onServiceStarted(
            attempt: Long,
            success: Boolean,
        ) {
            pendingStart?.takeIf { it.attempt == attempt }?.result?.complete(success)
        }

        /** True while [attempt] is the latest start and the call has not stopped; anything else is stale. */
        fun isCurrentAttempt(attempt: Long): Boolean = _active.value && attempt == currentAttempt

        /** Called by the service when the notification's End call action is tapped. */
        fun requestHangUp() {
            hangUpFlow.tryEmit(Unit)
        }

        override fun stop() {
            _active.value = false
        }

        companion object {
            const val START_TIMEOUT_MS = 3_000L

            /** Attempt id of a start intent that carries none; never a real attempt. */
            const val NO_ATTEMPT = 0L
        }
    }
