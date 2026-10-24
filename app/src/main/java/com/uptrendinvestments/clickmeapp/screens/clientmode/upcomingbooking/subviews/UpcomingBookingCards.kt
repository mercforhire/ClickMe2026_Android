package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.zoneAbbreviation
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.zoneCity
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val ExpertAvatarSize = 72.dp
private val RowIconSize = 20.dp
private val NoteIconSize = 16.dp

/** Shared card container of Upcoming Booking Details: medium shape, low surface, 1dp hairline. */
@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) { content() }
    }
}

/** Block 3: expert avatar, name and title. */
@Composable
fun UpcomingBookingExpertCard(
    name: String?,
    title: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = avatarUrl, contentDescription = null, size = ExpertAvatarSize)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name ?: stringResource(R.string.explore_expert_name_fallback),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Block 4: the session topic and the session fee, Money or Free from the topic-level flag (D-14). */
@Composable
fun UpcomingBookingTopicCard(
    topic: String?,
    price: PriceDisplay,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier) {
        Text(
            text = stringResource(R.string.upcoming_booking_session_topic),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Text(
            text = topic ?: stringResource(R.string.explore_session_topic_fallback),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.upcoming_booking_session_fee),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PriceText(price, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Block 5: the session date and time range in [zone], with the 12 or 24 hour setting in [timeFormat]. */
@Composable
fun UpcomingBookingDateTimeCard(
    start: Instant,
    end: Instant,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier) {
        LabelledRow(
            icon = Icons.Outlined.CalendarMonth,
            label = stringResource(R.string.upcoming_booking_date),
            value = formatShortDate(start.atZone(zone).toLocalDate(), locale),
        )
        Spacer(Modifier.height(Spacing.md))
        LabelledRow(
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.upcoming_booking_time),
            value = formatSessionTimeRange(start, end, zone, timeFormat),
        )
    }
}

@Composable
private fun LabelledRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(RowIconSize),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Block 6: "Expert timezone: Toronto (EDT)". The abbreviation is taken on [on] so EST and EDT are right. A null, blank
 * or invalid zone id renders nothing, and the raw IANA id is never shown.
 */
@Composable
fun UpcomingBookingTimezoneNote(
    expertTimezone: String?,
    on: LocalDate,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val zone =
        expertTimezone?.takeIf { it.isNotBlank() }?.let {
            try {
                ZoneId.of(it)
            } catch (_: DateTimeException) {
                null
            }
        } ?: return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = null,
            modifier = Modifier.size(NoteIconSize),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text =
                stringResource(
                    R.string.upcoming_booking_expert_timezone,
                    zoneCity(zone.id),
                    zoneAbbreviation(zone, on, locale),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

/** Block 9: the message the client wrote when booking. Plain selectable text only. */
@Composable
fun UpcomingBookingMessageCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier) {
        Text(
            text = stringResource(R.string.upcoming_booking_your_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.height(Spacing.xs))
        SelectionContainer {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "UpcomingBookingCards")
@Composable
private fun UpcomingBookingCardsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                UpcomingBookingExpertCard("Ada Lovelace", "Engineer", null)
                UpcomingBookingTopicCard("Algorithms", PriceDisplay.Free)
                UpcomingBookingMessageCard("Please prepare slides")
            }
        }
    }
}
