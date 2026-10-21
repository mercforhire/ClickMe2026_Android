package com.uptrendinvestments.clickmeapp.constants

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** A timezone the user can pick. [id] is the IANA identifier sent to the server; [label] is shown in the picker. */
data class TimezoneEntry(
    val id: String,
    val label: String,
)

/** Port of iOS `Timezones` (Constants/Timezones.swift). */
object Timezones {
    val all: List<TimezoneEntry> =
        listOf(
            TimezoneEntry("Pacific/Midway", "(GMT-11:00) Midway"),
            TimezoneEntry("America/Anchorage", "(GMT-09:00) Alaska"),
            TimezoneEntry("America/Los_Angeles", "(GMT-08:00) Pacific Time"),
            TimezoneEntry("America/Denver", "(GMT-07:00) Mountain Time"),
            TimezoneEntry("America/Chicago", "(GMT-06:00) Central Time"),
            TimezoneEntry("America/New_York", "(GMT-05:00) Eastern Time"),
            TimezoneEntry("UTC", "(GMT+00:00) UTC"),
            TimezoneEntry("Europe/London", "(GMT+00:00) London"),
            TimezoneEntry("Europe/Paris", "(GMT+01:00) Paris"),
            TimezoneEntry("Asia/Kolkata", "(GMT+05:30) Mumbai"),
        )

    private val availableIds: Set<String> by lazy { ZoneId.getAvailableZoneIds() }

    /** `(GMT+hh:mm) City`, from the zone's offset at [now] and the last IANA path segment. */
    fun label(
        zone: ZoneId,
        now: Instant,
    ): String {
        val totalSeconds = zone.rules.getOffset(now).totalSeconds
        val sign = if (totalSeconds < 0) "-" else "+"
        val abs = kotlin.math.abs(totalSeconds)
        val hours = abs / SECONDS_PER_HOUR
        val minutes = abs % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val city = zone.id.substringAfterLast('/').replace('_', ' ')
        return String.format(Locale.ROOT, "(GMT%s%02d:%02d) %s", sign, hours, minutes, city)
    }

    /**
     * Best-effort match of the device zone: exact bundled id; else the device's own valid region id (so the default
     * is always visible); else the first bundled zone whose current offset equals the device's; else UTC.
     */
    fun detected(
        device: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): TimezoneEntry {
        all.firstOrNull { it.id == device.id }?.let { return it }
        if (device.id in availableIds) return TimezoneEntry(device.id, label(device, now))
        val deviceOffset = device.rules.getOffset(now)
        all.firstOrNull { ZoneId.of(it.id).rules.getOffset(now) == deviceOffset }?.let { return it }
        return all.first { it.id == "UTC" }
    }

    /** The bundled rows, preceded by the detected zone when it is not one of them. */
    fun pickerEntries(
        device: ZoneId = ZoneId.systemDefault(),
        now: Instant = Instant.now(),
    ): List<TimezoneEntry> {
        val detected = detected(device, now)
        return if (all.any { it.id == detected.id }) all else listOf(detected) + all
    }

    /** Entry for an IANA id from a server response: bundled, else a runtime label, else the id itself. */
    fun entryFor(
        id: String,
        now: Instant = Instant.now(),
    ): TimezoneEntry {
        all.firstOrNull { it.id == id }?.let { return it }
        if (id in availableIds) return TimezoneEntry(id, label(ZoneId.of(id), now))
        return TimezoneEntry(id, id)
    }

    private const val SECONDS_PER_HOUR = 3600
    private const val SECONDS_PER_MINUTE = 60
}
