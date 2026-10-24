package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.CallForegroundController
import com.uptrendinvestments.clickmeapp.managers.CallNotificationInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Scriptable [CallForegroundController]. Set [startResult] or [startGate] to control the start outcome and
 * call [emitHangUp] to simulate the notification's End call action. Log entries: "foreground.start",
 * "foreground.stop".
 */
class FakeCallForegroundController(
    private val log: MutableList<String>? = null,
) : CallForegroundController {
    private val hangUpFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    override val hangUpRequests: Flow<Unit> = hangUpFlow

    @Volatile
    var startResult: Boolean = true

    /** When set, [startAndAwait] returns the value this completes with. */
    @Volatile
    var startGate: CompletableDeferred<Boolean>? = null

    private val _starts = mutableListOf<CallNotificationInfo>()
    val starts: List<CallNotificationInfo> get() = synchronized(_starts) { _starts.toList() }

    @Volatile
    var stopCalls: Int = 0
        private set

    fun emitHangUp() {
        check(hangUpFlow.tryEmit(Unit)) { "hang-up buffer full" }
    }

    override suspend fun startAndAwait(info: CallNotificationInfo): Boolean {
        log?.let { synchronized(it) { it += "foreground.start" } }
        synchronized(_starts) { _starts += info }
        return startGate?.await() ?: startResult
    }

    override fun stop() {
        log?.let { synchronized(it) { it += "foreground.stop" } }
        stopCalls++
    }
}
