package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.CallPermissionGate
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import kotlinx.coroutines.CompletableDeferred

/**
 * Scriptable [CallPermissionGate]. Returns [result], or suspends on [gate] when set. Log entry:
 * "permission".
 */
class FakeCallPermissionGate(
    private val log: MutableList<String>? = null,
) : CallPermissionGate {
    @Volatile
    var result: MicPermissionResult = MicPermissionResult.Granted

    /** When set, [ensureMicrophone] returns the value this completes with. */
    @Volatile
    var gate: CompletableDeferred<MicPermissionResult>? = null

    @Volatile
    var calls: Int = 0
        private set

    override suspend fun ensureMicrophone(): MicPermissionResult {
        log?.let { synchronized(it) { it += "permission" } }
        calls++
        return gate?.await() ?: result
    }
}
