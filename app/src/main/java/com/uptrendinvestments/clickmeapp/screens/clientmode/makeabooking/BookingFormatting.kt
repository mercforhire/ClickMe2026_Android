package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Notes length cap shown in the Message card (matches iOS). */
const val NOTES_MAX_CHARS = 500

/**
 * Caps [text] at [max] UTF-16 units without leaving a trailing lone high surrogate. Android counts UTF-16 units while
 * iOS counts graphemes, so the counter can differ slightly on emoji; the server limit is 1000, so the cap is UX
 * parity only.
 */
fun capNotes(
    text: String,
    max: Int = NOTES_MAX_CHARS,
): String {
    if (text.length <= max) return text
    val cut = text.substring(0, max)
    return if (cut.isNotEmpty() && Character.isHighSurrogate(cut.last())) cut.dropLast(1) else cut
}

/**
 * [start] in [zone], formatted with [timeFormat]. Pass the device time format ([rememberDeviceTimeFormat]) so the
 * 12/24 hour setting wins. [timeFormat] is cloned and not mutated.
 */
fun formatSlotTime(
    start: Instant,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
): String {
    val format = (timeFormat.clone() as java.text.DateFormat).apply { timeZone = TimeZone.getTimeZone(zone) }
    return format.format(Date.from(start))
}

/** "Thursday, October 22". */
fun formatSlotDateLine(
    date: LocalDate,
    locale: Locale,
): String = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale).format(date)

/** "Oct 24, 2026" (summary date). */
fun formatShortDate(
    date: LocalDate,
    locale: Locale,
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)

/** "October 2026" (calendar header). */
fun formatMonthYear(
    month: YearMonth,
    locale: Locale,
): String =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale).format(month.atDay(1))

/** "Thursday, October 22, 2026" (calendar day accessibility label). */
fun formatDayLabel(
    date: LocalDate,
    locale: Locale,
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)

/** The device 12/24 hour time format, refreshed when the configuration changes (Phase 5 WR-05). */
@Composable
fun rememberDeviceTimeFormat(): java.text.DateFormat {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { DateFormat.getTimeFormat(context) }
}
