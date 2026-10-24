package com.uptrendinvestments.clickmeapp.managers

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of asking for the microphone permission. */
enum class MicPermissionResult {
    Granted,
    DeniedOnce,
    DeniedPermanently,
    Cancelled,
}

/** Port over the UI that can show the runtime permission prompt. */
fun interface CallPermissionGate {
    /** Ensures the microphone permission, prompting if needed. Suspends until the user answers. */
    suspend fun ensureMicrophone(): MicPermissionResult
}

/** A microphone request waiting for the UI to answer, identified by [id]. */
data class PendingMicPermission(
    val id: Long,
)

/** How long a prompt may stay unanswered before the request is treated as cancelled. */
const val MIC_PERMISSION_TIMEOUT_MS = 60_000L

/**
 * [CallPermissionGate] that hands the request to the UI. The root overlay collects [pending], runs the
 * permission launcher and calls [complete]. The timeout covers a user who leaves during the prompt
 * (RESEARCH Pattern 4, T-09-13). Only one request is in flight; a newer request cancels the older one.
 */
@Singleton
class CallPermissionBroker
    @Inject
    constructor() : CallPermissionGate {
        private val nextId = AtomicLong(0)
        private val lock = Any()
        private var current: Pair<Long, CompletableDeferred<MicPermissionResult>>? = null

        private val _pending = MutableStateFlow<PendingMicPermission?>(null)

        /** The request the UI should fulfil, or null when none is waiting. */
        val pending: StateFlow<PendingMicPermission?> = _pending.asStateFlow()

        override suspend fun ensureMicrophone(): MicPermissionResult {
            val id = nextId.incrementAndGet()
            val deferred = CompletableDeferred<MicPermissionResult>()
            synchronized(lock) {
                current?.second?.complete(MicPermissionResult.Cancelled)
                current = id to deferred
                _pending.value = PendingMicPermission(id)
            }
            try {
                return withTimeoutOrNull(MIC_PERMISSION_TIMEOUT_MS) { deferred.await() }
                    ?: MicPermissionResult.Cancelled
            } finally {
                synchronized(lock) {
                    if (current?.first == id) {
                        current = null
                        _pending.value = null
                    }
                }
            }
        }

        /** Fulfils the request with [id]. A stale or unknown id is ignored. */
        fun complete(
            id: Long,
            result: MicPermissionResult,
        ) {
            synchronized(lock) {
                current?.takeIf { it.first == id }?.second?.complete(result)
            }
        }
    }
