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
 * state with [setState], handshake rejections with [emitFailure] and realtime events with [emit].
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

    /**
     * Emits [event] on [events]. Replay is 0, so a collector must already be subscribed (call `runCurrent()`
     * after creating the ViewModel).
     */
    fun emit(event: RealtimeEvent) {
        check(_events.tryEmit(event)) { "event buffer full" }
    }

    fun emitFailure(failure: HandshakeFailure) {
        check(_handshakeFailures.tryEmit(failure)) { "handshake failure buffer full" }
    }

    fun setState(state: ConnectionState) {
        _state.value = state
    }

    private val _typingStarts = mutableListOf<Pair<String, String>>()
    private val _typingStops = mutableListOf<Pair<String, String>>()
    private val _messageReads = mutableListOf<Pair<String, List<String>>>()

    /** Every `typing.start` emit as (threadId, recipientId), in call order. */
    val typingStarts: List<Pair<String, String>> get() = synchronized(_typingStarts) { _typingStarts.toList() }

    /** Every `typing.stop` emit as (threadId, recipientId), in call order. */
    val typingStops: List<Pair<String, String>> get() = synchronized(_typingStops) { _typingStops.toList() }

    /** Every `message.read` emit as (threadId, messageIds), in call order. */
    val messageReads: List<Pair<String, List<String>>> get() = synchronized(_messageReads) { _messageReads.toList() }

    // The emit overrides below record every call regardless of state. The real connection drops emits unless it is
    // Connected, so ViewModels that care check `state.value` themselves (RESEARCH Pitfall 5).
    override fun emitTypingStart(
        threadId: String,
        recipientId: String,
    ) {
        synchronized(_typingStarts) { _typingStarts += threadId to recipientId }
    }

    override fun emitTypingStop(
        threadId: String,
        recipientId: String,
    ) {
        synchronized(_typingStops) { _typingStops += threadId to recipientId }
    }

    override fun emitMessageRead(
        threadId: String,
        messageIds: List<String>,
    ) {
        synchronized(_messageReads) { _messageReads += threadId to messageIds }
    }
}
