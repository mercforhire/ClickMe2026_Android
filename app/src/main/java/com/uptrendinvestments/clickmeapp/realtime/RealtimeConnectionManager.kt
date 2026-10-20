package com.uptrendinvestments.clickmeapp.realtime

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.network.ApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
        private var retryAttempt = 0

        /** The access token a /me probe was already made for; at most one probe per token (D-15). */
        private var lastProbedToken: String? = null

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
                            retryAttempt = 0
                            connection.disconnect()
                            if (!userManager.isLoggedIn.value) lastProbedToken = null
                        }
                    }
            }

            policyScope.launch {
                sessionEvents.events
                    .filter { it == SessionEvent.Refreshed }
                    .collect { if (isDesired()) connectWithStoredToken() }
            }

            policyScope.launch {
                connection.state.collect { if (it == ConnectionState.Connected) retryAttempt = 0 }
            }

            policyScope.launch { connection.handshakeFailures.collect(::onFailure) }
        }

        /** The one place that decides whether the socket should exist (Phase 9 ORs a keep-alive input here). */
        private fun isDesired(): Boolean = userManager.isLoggedIn.value && foreground.isForeground.value

        private suspend fun connectWithStoredToken() {
            cancelRetry()
            val token = withContext(io) { tokens.accessToken }
            if (token == null) connection.disconnect() else connection.connect(token)
        }

        /**
         * Handshake rejections and server disconnects. The library retries none of these, so the policy does.
         *
         * An INVALID_TOKEN or ACCOUNT_SUSPENDED rejection triggers one authenticated `GET /me` per access
         * token through [UserManager.refreshMe]. An expired token then goes through the existing
         * single-flight TokenAuthenticator refresh, and a suspended account through SuspensionInterceptor
         * (Phase 2 D-03, D-08). This class never refreshes by hand: refresh-token reuse revokes the session.
         */
        private suspend fun onFailure(failure: HandshakeFailure) {
            when (failure.reason) {
                HandshakeFailureReason.MissingToken -> connection.disconnect()
                HandshakeFailureReason.InvalidToken,
                HandshakeFailureReason.AccountSuspended,
                -> onTokenRejected(failure.token)
                HandshakeFailureReason.ServerDisconnected,
                HandshakeFailureReason.Other,
                -> scheduleRetry()
            }
        }

        private suspend fun onTokenRejected(rejected: String) {
            val stored = withContext(io) { tokens.accessToken }
            when {
                stored == null -> connection.disconnect()
                stored != rejected -> connectWithStoredToken()
                rejected != lastProbedToken -> {
                    lastProbedToken = rejected
                    try {
                        userManager.refreshMe()
                    } catch (e: ApiException) {
                        // The transport already acted: a rejected refresh or suspension cleared the tokens
                        // and logs out through SessionEvents; a soft failure is handled by afterProbe.
                    }
                    afterProbe(rejected)
                }
                else -> scheduleRetry()
            }
        }

        private suspend fun afterProbe(probedToken: String) {
            if (!isDesired()) return
            val stored = withContext(io) { tokens.accessToken }
            if (stored != probedToken) connectWithStoredToken() else scheduleRetry()
        }

        /** Own capped backoff (2, 4, 8, 16, 30, 30 s), reset by Connected, logout and background. */
        private fun scheduleRetry() {
            cancelRetry()
            retryAttempt++
            val shift = (retryAttempt - 1).coerceAtMost(MAX_BACKOFF_SHIFT)
            val delayMs = minOf(config.retryInitialDelayMs shl shift, config.retryMaxDelayMs)
            // The delay runs on io, not on the limited view: a limitedParallelism view does not forward
            // delay to a test scheduler, so virtual time would never advance. Policy state is only touched
            // after hopping back onto serial.
            retryJob =
                scope.launch(io) {
                    delay(delayMs)
                    withContext(serial) {
                        // Detach first: connectWithStoredToken cancels the pending retry, which is this job.
                        retryJob = null
                        if (isDesired()) connectWithStoredToken()
                    }
                }
        }

        private fun cancelRetry() {
            retryJob?.cancel()
            retryJob = null
        }

        private companion object {
            /** Keeps the shift from overflowing; far beyond the point where the cap applies. */
            const val MAX_BACKOFF_SHIFT = 20
        }
    }
