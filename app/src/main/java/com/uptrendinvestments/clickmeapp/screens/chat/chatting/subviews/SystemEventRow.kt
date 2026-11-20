package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.SystemEventKind
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ThreadItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EventTile = 40.dp
private val EventGlyph = 24.dp
private const val POSITIVE_TILE_ALPHA = 0.12f
private const val SUBTITLE_MAX_LINES = 2
private const val SUBTITLE_SEPARATOR = " · "

/** The row title of [this] kind ("Booking Request" ... "Session Completed"). */
@StringRes
fun SystemEventKind.titleRes(): Int =
    when (this) {
        SystemEventKind.BookingRequest -> R.string.chat_event_booking_request
        SystemEventKind.BookingConfirmed -> R.string.chat_event_booking_confirmed
        SystemEventKind.BookingDeclined -> R.string.chat_event_booking_declined
        SystemEventKind.BookingCancelled -> R.string.chat_event_booking_cancelled
        SystemEventKind.BookingRescheduled -> R.string.chat_event_booking_rescheduled
        SystemEventKind.BookingCompleted -> R.string.chat_event_booking_completed
        SystemEventKind.BookingExpired -> R.string.chat_event_booking_expired
        SystemEventKind.Unknown -> R.string.chat_event_unknown
    }

/** The row title of [this] event: the kind's title, except a reschedule decline reads "Reschedule Declined". */
@StringRes
fun ThreadItem.SystemEvent.titleRes(): Int =
    if (isRescheduleDecline) R.string.chat_event_reschedule_declined else kind.titleRes()

/** The outlined glyph of [this] kind. */
fun SystemEventKind.icon(): ImageVector =
    when (this) {
        SystemEventKind.BookingRequest -> Icons.Outlined.MailOutline
        SystemEventKind.BookingConfirmed -> Icons.Outlined.EventAvailable
        SystemEventKind.BookingDeclined -> Icons.Outlined.Cancel
        SystemEventKind.BookingCancelled -> Icons.Outlined.EventBusy
        SystemEventKind.BookingRescheduled -> Icons.Outlined.Update
        SystemEventKind.BookingCompleted -> Icons.Outlined.TaskAlt
        SystemEventKind.BookingExpired -> Icons.Outlined.HourglassEmpty
        SystemEventKind.Unknown -> Icons.Outlined.Info
    }

/**
 * A booking system event inside the thread (iOS `ChattingSystemEventRow`): the kind's title and glyph, and a subtitle
 * "{topic} · {EEE, MMM d} · {time}", the topic alone, or the server content for legacy rows. It is one `Role.Button`
 * with a chevron only when the event has a booking id; [onOpenBooking] gets that id and whether the kind is terminal.
 */
@Composable
fun SystemEventRow(
    event: ThreadItem.SystemEvent,
    zone: ZoneId,
    locale: Locale,
    timeFormat: java.text.DateFormat,
    onOpenBooking: (bookingId: String, isTerminal: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookingId = event.bookingId
    val viewBooking = stringResource(R.string.chat_event_view_booking)
    val subtitle = eventSubtitle(event, zone, locale, timeFormat)
    val clickModifier =
        if (bookingId != null) {
            Modifier.clickable(onClickLabel = viewBooking, role = Role.Button) {
                onOpenBooking(bookingId, event.isTerminal)
            }
        } else {
            Modifier.semantics(mergeDescendants = true) {}
        }
    Surface(
        modifier = modifier.fillMaxWidth().then(clickModifier),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EventTile(event.kind)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(event.titleRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                        maxLines = SUBTITLE_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Another user's free text: a plain String in a plain Text (no links, no markup), wrapping fully.
                event.reasonCodeLabel?.let { ReasonLine(it) }
                event.reasonCaption?.let { ReasonLine(it) }
            }
            if (bookingId != null) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(EventGlyph),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReasonLine(text: String) {
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.extras.onSurfaceMuted,
    )
}

@Composable
private fun EventTile(kind: SystemEventKind) {
    val positive = kind.isPositive
    Surface(
        modifier = Modifier.size(EventTile),
        shape = MaterialTheme.shapes.small,
        color =
            if (positive) {
                MaterialTheme.colorScheme.primary.copy(alpha = POSITIVE_TILE_ALPHA)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
    ) {
        Icon(
            imageVector = kind.icon(),
            contentDescription = null,
            modifier = Modifier.padding((EventTile - EventGlyph) / 2).size(EventGlyph),
            tint = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun eventSubtitle(
    event: ThreadItem.SystemEvent,
    zone: ZoneId,
    locale: Locale,
    timeFormat: java.text.DateFormat,
): String? {
    val topic = event.topic?.takeIf { it.isNotBlank() }
    val start = event.startTime
    return when {
        topic != null && start != null -> {
            val date =
                DateTimeFormatter
                    .ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), locale)
                    .format(start.atZone(zone).toLocalDate())
            topic + SUBTITLE_SEPARATOR + date + SUBTITLE_SEPARATOR + formatSlotTime(start, zone, timeFormat)
        }
        topic != null -> topic
        else -> event.content?.takeIf { it.isNotBlank() }
    }
}
