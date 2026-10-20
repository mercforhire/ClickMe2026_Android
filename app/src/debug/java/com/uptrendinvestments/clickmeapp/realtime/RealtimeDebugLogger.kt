package com.uptrendinvestments.clickmeapp.realtime

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only realtime logging (D-20): the proof of Phase 3 success criterion 3 via `adb logcat -s Realtime`.
 *
 * Logs every connection-state change, every [RealtimeEvent] and every [HandshakeFailure] under the tag
 * `Realtime`. It relies on the redacting `toString` of `MessageNew` (ids only) and `HandshakeFailure`
 * (reason only), so no chat content or token is ever printed. Never log raw payloads or tokens here, and
 * do not enable java.util.logging FINE for io.socket (it prints packets).
 *
 * The collectors start undispatched so they are subscribed before [install] returns (the events flow has
 * no replay). The release source set has a no-op stub with the same signature.
 */
object RealtimeDebugLogger {
    private const val TAG = "Realtime"

    fun install(connection: RealtimeConnection) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connection.state.collect { Log.d(TAG, "state=$it") }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connection.events.collect { Log.d(TAG, it.toString()) }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            connection.handshakeFailures.collect { Log.d(TAG, it.toString()) }
        }
    }
}
