package com.uptrendinvestments.clickmeapp.managers

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Session-level happenings raised by the transport layer. */
sealed interface SessionEvent {
    /** A refresh succeeded; Phase 3's socket reconnects with the new token. */
    data object Refreshed : SessionEvent

    /** The server rejected the session (refresh refused, or a token-coded 401 after a refresh). */
    data object Rejected : SessionEvent

    /** The account is suspended (403 ACCOUNT_SUSPENDED). */
    data object Suspended : SessionEvent
}

/**
 * Process-wide bus between the OkHttp layer and the session manager.
 *
 * No replay, so the consumer (UserManager) must subscribe eagerly. Token clearing never depends on
 * delivery: every emitter clears the tokens synchronously before emitting.
 */
@Singleton
class SessionEvents
    @Inject
    constructor() {
        private val _events =
            MutableSharedFlow<SessionEvent>(
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

        fun emit(event: SessionEvent) {
            _events.tryEmit(event)
        }
    }
