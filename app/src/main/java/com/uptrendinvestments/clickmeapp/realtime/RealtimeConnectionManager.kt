package com.uptrendinvestments.clickmeapp.realtime

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.UserManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped connection policy for the realtime socket: when it should be connected and with which token.
 *
 * The socket is wanted while the user is logged in and the process is in the foreground (D-13, D-14).
 * Logout, session rejection and suspension all end in `UserManager.logout`, which flips `isLoggedIn`, so
 * one condition covers all of them. Every `SessionEvent.Refreshed` reconnects with the stored token (RT-02).
 *
 * Deliberately has no logging (it runs in plain JVM tests) and never touches the token refresh machinery
 * directly (D-15, see handshake handling).
 */
@Singleton
class RealtimeConnectionManager
    @Inject
    constructor(
        private val connection: RealtimeConnection,
        private val userManager: UserManager,
        private val tokens: SecureTokenStore,
        private val sessionEvents: SessionEvents,
        private val foreground: ProcessForeground,
        private val config: RealtimeConfig,
        @ApplicationScope private val scope: CoroutineScope,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) {
        private val started = AtomicBoolean(false)

        /** All policy state below is touched from one coroutine at a time through this view. */
        @OptIn(ExperimentalCoroutinesApi::class)
        private val serial = io.limitedParallelism(1)

        private var retryJob: Job? = null

        /** Idempotent. Call once from application startup. */
        fun start() {
            if (!started.compareAndSet(false, true)) return
            val policyScope = scope + serial

            policyScope.launch {
                combine(userManager.isLoggedIn, foreground.isForeground) { loggedIn, fg -> loggedIn && fg }
                    .distinctUntilChanged()
                    .collectLatest { desired ->
                        if (desired) {
                            connectWithStoredToken()
                        } else {
                            cancelRetry()
                            connection.disconnect()
                        }
                    }
            }

            policyScope.launch {
                sessionEvents.events
                    .filter { it == SessionEvent.Refreshed }
                    .collect { if (isDesired()) connectWithStoredToken() }
            }
        }

        /** The one place that decides whether the socket should exist (Phase 9 ORs a keep-alive input here). */
        private fun isDesired(): Boolean = userManager.isLoggedIn.value && foreground.isForeground.value

        private suspend fun connectWithStoredToken() {
            cancelRetry()
            val token = withContext(io) { tokens.accessToken }
            if (token == null) connection.disconnect() else connection.connect(token)
        }

        private fun cancelRetry() {
            retryJob?.cancel()
            retryJob = null
        }
    }
