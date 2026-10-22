package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Data for the time slots timezone note (D-05). */
sealed interface TimezoneNote {
    /** Client and expert share a zone, or the expert zone is unknown. */
    data class Same(
        val clientAbbr: String,
    ) : TimezoneNote

    /** The expert is in another zone: [expertCity] is the last IANA segment with spaces. */
    data class Different(
        val clientAbbr: String,
        val expertCity: String,
        val expertAbbr: String,
    ) : TimezoneNote
}

/**
 * Builds the timezone note for [on], the selected date, so EST/EDT switch correctly. A null, blank or invalid
 * [expertZoneId] yields [TimezoneNote.Same] and never throws, as does an alias of the client zone ([isSameZone]). The composable maps [TimezoneNote.Same] to
 * `make_a_booking_tz_same` and [TimezoneNote.Different] to `make_a_booking_tz_different`; the raw IANA id is never
 * shown (D-05).
 */
fun timezoneNote(
    client: ZoneId,
    expertZoneId: String?,
    on: LocalDate,
    locale: Locale,
): TimezoneNote {
    val expert =
        if (expertZoneId.isNullOrBlank()) {
            null
        } else {
            try {
                ZoneId.of(expertZoneId)
            } catch (_: DateTimeException) {
                null
            }
        }
    val clientAbbr = zoneAbbreviation(client, on, locale)
    if (expert == null || isSameZone(client, expert)) return TimezoneNote.Same(clientAbbr)
    return TimezoneNote.Different(clientAbbr, zoneCity(expert.id), zoneAbbreviation(expert, on, locale))
}

/**
 * True when [a] and [b] are the same zone under different ids: an alias ("Asia/Calcutta" and "Asia/Kolkata",
 * "US/Eastern" and "America/New_York") or a fixed offset spelled two ways ("UTC" and "Etc/UTC"). Zones that only
 * share today's offset ("America/Toronto" and "America/New_York") keep their own rules, so they stay different.
 */
internal fun isSameZone(
    a: ZoneId,
    b: ZoneId,
): Boolean = a.id == b.id || a.normalized() == b.normalized() || a.rules == b.rules

/** Short platform name of [zone] ("EDT", "PST") as in effect at noon of [on]. */
fun zoneAbbreviation(
    zone: ZoneId,
    on: LocalDate,
    locale: Locale,
): String {
    val tz = TimeZone.getTimeZone(zone)
    val noon = on.atTime(12, 0).atZone(zone).toInstant()
    return tz.getDisplayName(tz.inDaylightTime(Date.from(noon)), TimeZone.SHORT, locale)
}

/** "America/Argentina/Buenos_Aires" becomes "Buenos Aires". */
fun zoneCity(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')
