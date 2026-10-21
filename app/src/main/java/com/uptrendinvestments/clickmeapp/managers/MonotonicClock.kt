package com.uptrendinvestments.clickmeapp.managers

/**
 * A clock that only moves forward, injectable so tests can drive it with virtual time. The production
 * implementation is `SystemClock.elapsedRealtime()`, which keeps counting through device sleep and survives
 * process death within one boot, so an end time saved before a process death is still valid afterwards.
 */
fun interface MonotonicClock {
    fun nowMillis(): Long
}
