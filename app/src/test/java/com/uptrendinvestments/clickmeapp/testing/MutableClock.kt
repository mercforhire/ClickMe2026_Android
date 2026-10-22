package com.uptrendinvestments.clickmeapp.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Test wall clock whose [instant] can be set or advanced by tests. */
class MutableClock(
    var instant: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = fixed(instant, zone)

    override fun instant(): Instant = instant

    override fun millis(): Long = instant.toEpochMilli()

    fun advanceBy(duration: Duration) {
        instant = instant.plus(duration)
    }
}
