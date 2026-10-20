package com.uptrendinvestments.clickmeapp.realtime

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionState { Disconnected, Connecting, Connected }

enum class HandshakeFailureReason {
    MissingToken,
    InvalidToken,
    AccountSuspended,
    ServerDisconnected,
    Other,
}

/** A rejected handshake or server-initiated disconnect, carrying the token the attempt used. */
data class HandshakeFailure(
    val reason: HandshakeFailureReason,
    val token: String,
) {
    /** The token is a credential and is never printed. */
    override fun toString(): String = "HandshakeFailure(reason=$reason)"
}

/**
 * Maps the backend handshake middleware message to a reason. `ACCOUNT_SUSPENDED` is real even though
 * SOCKET-CONTRACT.md omits it (socket.js rejects suspended users).
 */
fun classifyHandshakeMessage(message: String?): HandshakeFailureReason =
    when (message) {
        "MISSING_TOKEN" -> HandshakeFailureReason.MissingToken
        "INVALID_TOKEN" -> HandshakeFailureReason.InvalidToken
        "ACCOUNT_SUSPENDED" -> HandshakeFailureReason.AccountSuspended
        else -> HandshakeFailureReason.Other
    }

/**
 * Socket settings. [reconnectDelayMs] is the library's own reconnect delay; the retry bounds belong
 * to the connection manager's backoff for handshake failures the library does not retry.
 */
data class RealtimeConfig(
    val socketUrl: String,
    val reconnectDelayMs: Long = 2_000,
    val retryInitialDelayMs: Long = 2_000,
    val retryMaxDelayMs: Long = 30_000,
)

/**
 * The seam over the socket.
 *
 * One fresh socket is created per connection. [connect] with the same token while the state is not
 * [ConnectionState.Disconnected] is a no-op (iOS parity). Emits are dropped unless [ConnectionState.Connected].
 * [handshakeFailures] carries middleware rejections (the library does not retry those) and
 * server-initiated disconnects.
 */
interface RealtimeConnection {
    val events: SharedFlow<RealtimeEvent>
    val state: StateFlow<ConnectionState>
    val handshakeFailures: SharedFlow<HandshakeFailure>

    fun connect(token: String)

    fun disconnect()

    fun emitTypingStart(
        threadId: String,
        recipientId: String,
    )

    fun emitTypingStop(
        threadId: String,
        recipientId: String,
    )

    fun emitMessageRead(
        threadId: String,
        messageIds: List<String>,
    )
}
