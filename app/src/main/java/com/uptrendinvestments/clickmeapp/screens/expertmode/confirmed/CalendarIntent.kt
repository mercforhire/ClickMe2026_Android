package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.uptrendinvestments.clickmeapp.components.web.findActivity
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import java.time.Instant

/**
 * The fields handed to the calendar app for one confirmed session (D-06): title, UTC epoch start and end, and a
 * description. There is no location and no reminder.
 */
data class CalendarEventSpec(
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val description: String,
)

/**
 * Builds the event handed to the calendar app. It serves Booking Confirmed, the expert Booking Summary and the client
 * Upcoming Booking, so it takes plain values rather than a screen's nav key. [title] is the localized
 * "ClickMe · {topic} with {name}" and [meetingTypeLabel] the localized "In-App Voice" / "Skype/Zoom". The description is
 * the meeting type, plus a new line and the trimmed [message] when that message is not blank. Times are UTC epoch
 * millis and no time zone extra is sent: the calendar app shows them in the device zone (Pitfall 9).
 */
fun calendarEventSpec(
    title: String,
    beginMillis: Long,
    endMillis: Long,
    meetingTypeLabel: String,
    message: String?,
): CalendarEventSpec {
    val note = message?.trim()?.takeIf { it.isNotEmpty() }
    return CalendarEventSpec(
        title = title,
        beginMillis = beginMillis,
        endMillis = endMillis,
        description = if (note == null) meetingTypeLabel else "$meetingTypeLabel\n$note",
    )
}

/**
 * Whether a booking offers "Add to Calendar": it is booked ([JOINABLE_BOOKING_STATUSES]) and the session is not over.
 * Parity with iOS `BookingCalendarEventContent.isOffered`. Callers pass the ticking join clock as [now] so the button
 * disappears once the scheduled [end] passes.
 */
fun isCalendarEventOffered(
    statusCode: String,
    end: Instant,
    now: Instant,
): Boolean = statusCode in JOINABLE_BOOKING_STATUSES && now.isBefore(end)

/**
 * Returns the tap handler for "Add to Calendar". It ignores taps while the calendar app is in front (the flag clears
 * when the screen resumes) and calls [onNoHandler] when nothing can handle the intent, after re-enabling the button.
 */
@Composable
fun rememberCalendarInsertLauncher(onNoHandler: () -> Unit): (CalendarEventSpec) -> Unit {
    val context = LocalContext.current
    val currentOnNoHandler by rememberUpdatedState(onNoHandler)
    var launching by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        launching = false
        onPauseOrDispose {}
    }
    return remember(context) {
        { spec ->
            if (!launching) {
                launching = true
                context.launchCalendarInsert(buildCalendarInsertIntent(spec)) {
                    launching = false
                    currentOnNoHandler()
                }
            }
        }
    }
}

/**
 * The `ACTION_INSERT` intent for [spec]. It targets the fixed `CalendarContract.Events.CONTENT_URI` and carries only
 * begin, end, title and description as plain extras: nothing built from user text goes into a URI, there is no
 * location, and `ACTION_INSERT` cannot set a reminder (the calendar app's default applies, D-06). No calendar
 * permission is needed or requested: the calendar app owns the editor and the user's consent.
 */
fun buildCalendarInsertIntent(spec: CalendarEventSpec): Intent =
    Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, spec.beginMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, spec.endMillis)
        .putExtra(CalendarContract.Events.TITLE, spec.title)
        .putExtra(CalendarContract.Events.DESCRIPTION, spec.description)

/**
 * Starts [intent], or calls [onNoHandler] when no app can handle it.
 *
 * The launch is not pre-resolved against the package manager because the manifest declares no package-visibility
 * queries, so a resolve call would report "none" on Android 11+ even when a calendar exists; catching
 * [ActivityNotFoundException] is the only reliable check. Title and description are plain strings from untrusted
 * users and are never put into a URI.
 */
fun Context.launchCalendarInsert(
    intent: Intent,
    onNoHandler: () -> Unit,
) {
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onNoHandler()
    }
}
