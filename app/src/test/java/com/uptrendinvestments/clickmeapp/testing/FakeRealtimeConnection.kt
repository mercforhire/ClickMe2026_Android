package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.HandshakeFailure
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Scriptable [RealtimeConnection]. Records every [connect] token and [disconnect] call; tests drive the
 * state with [setState] and handshake rejections with [emitFailure].
 */
class FakeRealtimeConnection : RealtimeConnection {
    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<RealtimeEvent> = _events

    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _handshakeFailures = MutableSharedFlow<HandshakeFailure>(extraBufferCapacity = 8)
    override val handshakeFailures: SharedFlow<HandshakeFailure> = _handshakeFailures

    private val _connectCalls = mutableListOf<String>()
    val connectCalls: List<String> get() = synchronized(_connectCalls) { _connectCalls.toList() }

    @Volatile
    var disconnectCalls: Int = 0
        private set

    override fun connect(token: String) {
        synchronized(_connectCalls) { _connectCalls += token }
        _state.value = ConnectionState.Connecting
    }

    override fun disconnect() {
        disconnectCalls++
        _state.value = ConnectionState.Disconnected
    }

    fun emitFailure(failure: HandshakeFailure) {
        check(_handshakeFailures.tryEmit(failure)) { "handshake failure buffer full" }
    }

    fun setState(state: ConnectionState) {
        _state.value = state
    }

    override fun emitTypingStart(
        threadId: String,
        recipientId: String,
    ) = Unit

    override fun emitTypingStop(
        threadId: String,
        recipientId: String,
    ) = Unit

    override fun emitMessageRead(
        threadId: String,
        messageIds: List<String>,
    ) = Unit
}
