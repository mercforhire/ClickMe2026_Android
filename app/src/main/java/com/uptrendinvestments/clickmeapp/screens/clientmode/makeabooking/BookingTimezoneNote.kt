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
 * [expertZoneId] yields [TimezoneNote.Same] and never throws. The composable maps [TimezoneNote.Same] to
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
    if (expert == null || expert.id == client.id) return TimezoneNote.Same(clientAbbr)
    return TimezoneNote.Different(clientAbbr, zoneCity(expert.id), zoneAbbreviation(expert, on, locale))
}

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
