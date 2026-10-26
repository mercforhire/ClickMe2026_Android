package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.uptrendinvestments.clickmeapp.components.web.findActivity

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
 * Builds the event for [key]. [title] is the localized "ClickMe · {topic} with {client}" and [meetingTypeLabel] the
 * localized "In-App Voice" / "Skype/Zoom". The description is the meeting type, plus a new line and the client's
 * message when that message is not blank. Times are UTC epoch millis and no time zone extra is sent: the calendar app
 * shows them in the device zone (Pitfall 9).
 */
fun calendarEventSpec(
    key: BookingConfirmedKey,
    title: String,
    meetingTypeLabel: String,
): CalendarEventSpec {
    val message = key.clientMessage?.trim()?.takeIf { it.isNotEmpty() }
    return CalendarEventSpec(
        title = title,
        beginMillis = key.startMillis,
        endMillis = key.endMillis,
        description = if (message == null) meetingTypeLabel else "$meetingTypeLabel\n$message",
    )
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
