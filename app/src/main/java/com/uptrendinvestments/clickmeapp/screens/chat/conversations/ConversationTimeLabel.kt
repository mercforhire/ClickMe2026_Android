package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import android.text.format.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DAY_MONTH_SKELETON = "MMMd"

/**
 * The time label on a Conversations row (iOS `relativeTimeLabel`): today shows the short time, yesterday shows
 * [yesterday], anything older a short month and day for [locale] ("Oct 24"). Null [at] (no last message) gives "".
 *
 * [timeFormat] is the device short time format (`DateFormat.getTimeFormat(context)`), so the user's 12 or 24 hour
 * setting wins. It is cloned and never mutated. "Today" is judged in [zone], from [now].
 */
fun relativeTimeLabel(
    at: Instant?,
    now: Instant,
    zone: ZoneId,
    locale: Locale,
    timeFormat: java.text.DateFormat,
    yesterday: String,
): String {
    if (at == null) return ""
    val day = at.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when (day) {
        today -> {
            val format = (timeFormat.clone() as java.text.DateFormat).apply { timeZone = TimeZone.getTimeZone(zone) }
            format.format(Date.from(at))
        }
        today.minusDays(1) -> yesterday
        else ->
            DateTimeFormatter
                .ofPattern(DateFormat.getBestDateTimePattern(locale, DAY_MONTH_SKELETON), locale)
                .withZone(zone)
                .format(at)
    }
}
