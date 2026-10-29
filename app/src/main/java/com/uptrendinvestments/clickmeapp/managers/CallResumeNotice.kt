package com.uptrendinvestments.clickmeapp.managers

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A memory-only, one-shot flag naming the booking whose detail screen shows the "you were on a call" resume note
 * (CONTEXT D-11, UI-SPEC A4).
 *
 * [CallResumeMarker] posts the booking id when the app starts after a process kill during a call. The two booking
 * detail view models call [consumeFor] once at construction: it is true exactly once for the matching booking and
 * false afterwards, so the note shows on the landing screen and never again. It never survives a process death, which
 * is intended: the persisted marker has already been consumed by then.
 */
@Singleton
class CallResumeNotice
    @Inject
    constructor() {
        private val pending = AtomicReference<String?>(null)

        /** Flags [bookingId] for the resume note; a later post replaces an unconsumed one. */
        fun post(bookingId: String) {
            pending.set(bookingId)
        }

        /** True once when [bookingId] is the flagged booking, then false; a different booking leaves the flag alone. */
        fun consumeFor(bookingId: String): Boolean = pending.compareAndSet(bookingId, null)
    }
