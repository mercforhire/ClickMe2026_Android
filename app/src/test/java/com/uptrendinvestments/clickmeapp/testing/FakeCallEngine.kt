package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.CallEngine
import com.uptrendinvestments.clickmeapp.managers.EngineEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Scriptable [CallEngine]. Tests drive [emit], set the result vars, and read the recorded calls. Pass a
 * shared [log] to assert cross-fake ordering ("engine.join", "engine.leave", "engine.destroy").
 */
class FakeCallEngine(
    private val log: MutableList<String>? = null,
) : CallEngine {
    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 16)
    override val events: Flow<EngineEvent> = _events

    @Volatile
    var joinResult: Boolean = true

    @Volatile
    var muteResult: Boolean = true

    @Volatile
    var speakerResult: Boolean = true

    /** When set, [leave] suspends until this completes. */
    @Volatile
    var leaveGate: CompletableDeferred<Unit>? = null

    private val _joins = mutableListOf<Triple<String, String, Int>>()
    val joins: List<Triple<String, String, Int>> get() = synchronized(_joins) { _joins.toList() }

    private val _muteCalls = mutableListOf<Boolean>()
    val muteCalls: List<Boolean> get() = synchronized(_muteCalls) { _muteCalls.toList() }

    private val _speakerCalls = mutableListOf<Boolean>()
    val speakerCalls: List<Boolean> get() = synchronized(_speakerCalls) { _speakerCalls.toList() }

    @Volatile
    var leaveCalls: Int = 0
        private set

    @Volatile
    var destroyCalls: Int = 0
        private set

    /**
     * Emits [event] on [events]. Replay is 0, so a collector must already be subscribed (call `runCurrent()`
     * first).
     */
    fun emit(event: EngineEvent) {
        check(_events.tryEmit(event)) { "event buffer full" }
    }

    override suspend fun join(
        token: String,
        channelName: String,
        uid: Int,
    ): Boolean {
        log?.let { synchronized(it) { it += "engine.join" } }
        synchronized(_joins) { _joins += Triple(token, channelName, uid) }
        return joinResult
    }

    override suspend fun leave() {
        log?.let { synchronized(it) { it += "engine.leave" } }
        leaveCalls++
        leaveGate?.await()
    }

    override fun setMuted(muted: Boolean): Boolean {
        synchronized(_muteCalls) { _muteCalls += muted }
        return muteResult
    }

    override fun setSpeakerOn(on: Boolean): Boolean {
        synchronized(_speakerCalls) { _speakerCalls += on }
        return speakerResult
    }

    override suspend fun destroy() {
        log?.let { synchronized(it) { it += "engine.destroy" } }
        destroyCalls++
    }
}
