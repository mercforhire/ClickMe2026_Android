package com.uptrendinvestments.clickmeapp.managers

import kotlinx.coroutines.flow.Flow

/**
 * Port over the voice SDK (Agora). Keeps `CallCenter` free of SDK types so every call rule except real
 * background audio runs in JVM tests.
 *
 * Implementations must not log the token, channel name or uid (T-09-02).
 */
interface CallEngine {
    /**
     * Engine events. Thread-safe: SDK callbacks arrive on SDK threads and the implementation only
     * `tryEmit`s into a buffered flow.
     */
    val events: Flow<EngineEvent>

    /** Joins the channel. Returns false when the SDK rejects the request; the outcome arrives via [events]. */
    suspend fun join(
        token: String,
        channelName: String,
        uid: Int,
    ): Boolean

    /** Leaves the channel, awaiting the SDK leave callback with a bounded timeout. */
    suspend fun leave()

    /** Mutes or unmutes the local microphone. Returns false when the SDK rejects the request. */
    fun setMuted(muted: Boolean): Boolean

    /** Routes audio to the loudspeaker or the earpiece. Returns false when the SDK rejects the request. */
    fun setSpeakerOn(on: Boolean): Boolean

    /**
     * Releases the engine. Runs off the main thread and never from inside an SDK callback (RESEARCH
     * Pitfall 10).
     */
    suspend fun destroy()
}

/** Engine-level events, already reduced to what the call state machine needs. */
sealed interface EngineEvent {
    /** The local user joined the channel. */
    data object LocalJoined : EngineEvent

    /** The local join failed or the local connection dropped terminally. */
    data object LocalFailed : EngineEvent

    /** A remote user joined the channel. */
    data class RemoteJoined(
        val uid: Int,
    ) : EngineEvent

    /** A remote user left the channel; [reason] is the SDK offline reason code. */
    data class RemoteOffline(
        val uid: Int,
        val reason: Int,
    ) : EngineEvent

    /** An SDK error with its [code]. */
    data class Error(
        val code: Int,
    ) : EngineEvent
}
