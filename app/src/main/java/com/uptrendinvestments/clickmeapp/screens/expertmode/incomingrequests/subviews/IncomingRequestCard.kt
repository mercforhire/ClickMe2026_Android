package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestClientDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.TimeSlotDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestRow
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.RequestCardEarnings
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.requestCardEarnings
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on an Incoming Requests card. */
const val INCOMING_REQUEST_CARD_TAG = "incoming_request_card"

private val AvatarSize = 56.dp
private val AvatarRing = 2.dp
private val SmallIconSize = 16.dp
private const val BADGE_FILL_ALPHA = 0.12f
private const val BADGE_BORDER_ALPHA = 0.60f
private val PREVIEW_START: Instant = Instant.parse("2026-10-24T18:00:00Z")

/**
 * One pending (or just expired) booking request (iOS `IncomingRequestCard`). The whole card opens Request Decision,
 * an expired one too, where it is read-only. An expired card is not dimmed (UI-SPEC A5): the "Expired" badge and the
 * amber avatar ring carry the state.
 *
 * The client name and topic are untrusted server text and are drawn as plain, truncated `Text` only.
 *
 * @param zone zone the date and times are shown in; defaults to the device zone.
 * @param timeFormat the device time format so the 12 or 24 hour setting applies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingRequestCard(
    row: IncomingRequestRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val item = row.item
    val name = clientDisplayName(item.client.fullName)
    val topic = item.topic ?: stringResource(R.string.explore_session_topic_fallback)
    val slot = item.timeSlot
    val date = formatShortDate(slot.startTime.atZone(zone).toLocalDate(), locale)
    val timeRange = formatSessionTimeRange(slot.startTime, slot.endTime, zone, timeFormat)
    val whenText = stringResource(R.string.my_bookings_date_time, date, timeRange)
    val a11y = cardDescription(row, name, topic, date, timeRange)
    val earnings = requestCardEarnings(item, locale)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(INCOMING_REQUEST_CARD_TAG)
                .clickable(
                    onClickLabel = stringResource(R.string.incoming_requests_open),
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) { contentDescription = a11y },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    RingedAvatar(url = item.client.avatarUrl, expired = row.isExpired)
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                EarningsColumn(earnings)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (row.isExpired) {
                    ExpiredBadge()
                } else {
                    ExpiryLine(row.hoursLeft)
                }
            }
        }
    }
}

/** The avatar inside a 2dp ring: neutral for a pending request, amber (tertiary) for an expired one. */
@Composable
private fun RingedAvatar(
    url: String?,
    expired: Boolean,
) {
    val ring = if (expired) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
    Box(modifier = Modifier.border(AvatarRing, ring, CircleShape).padding(AvatarRing)) {
        Avatar(url = url, contentDescription = null, size = AvatarSize)
    }
}

@Composable
private fun EarningsColumn(earnings: RequestCardEarnings) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = stringResource(R.string.incoming_requests_earnings),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.size(Spacing.xs))
        when (earnings) {
            is RequestCardEarnings.Net ->
                Text(
                    text = earnings.amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            RequestCardEarnings.Free ->
                Text(
                    text = stringResource(R.string.price_free),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            RequestCardEarnings.PayoutPending ->
                Text(
                    text = stringResource(R.string.incoming_requests_payout_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
        }
    }
}

/** "Expires in 5h" or "Expires in under 1h"; nothing for a request without an expiry. */
@Composable
private fun ExpiryLine(hoursLeft: Long?) {
    if (hoursLeft == null) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.size(SmallIconSize),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text =
                if (hoursLeft < 1) {
                    stringResource(R.string.incoming_requests_expires_under_hour)
                } else {
                    stringResource(R.string.incoming_requests_expires_in, hoursLeft)
                },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun ExpiredBadge() {
    val tint = MaterialTheme.colorScheme.tertiary
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = BADGE_FILL_ALPHA),
        border = BorderStroke(1.dp, tint.copy(alpha = BADGE_BORDER_ALPHA)),
    ) {
        Text(
            text = stringResource(R.string.incoming_requests_expired),
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}

/** "{client}, {topic}, {date}, {time range}, expires in {n} hours" or "..., expired". */
@Composable
private fun cardDescription(
    row: IncomingRequestRow,
    name: String,
    topic: String,
    date: String,
    timeRange: String,
): String {
    val hours = row.hoursLeft
    return when {
        row.isExpired -> stringResource(R.string.incoming_requests_card_a11y_expired, name, topic, date, timeRange)
        hours == null -> stringResource(R.string.incoming_requests_card_a11y_no_expiry, name, topic, date, timeRange)
        hours < 1 ->
            stringResource(R.string.incoming_requests_card_a11y_under_hour, name, topic, date, timeRange)
        else ->
            pluralStringResource(
                R.plurals.incoming_requests_card_a11y_pending,
                hours.toInt(),
                name,
                topic,
                date,
                timeRange,
                hours.toInt(),
            )
    }
}

private fun previewRow(
    hoursLeft: Long?,
    expired: Boolean,
) = IncomingRequestRow(
    item =
        BookingRequestItem(
            requestId = "r1",
            client = BookingRequestClientDto(id = "c1", fullName = "Ada Lovelace"),
            topic = "Algorithms",
            earnings = 75.0,
            currency = "USD",
            timeSlot = TimeSlotDto(PREVIEW_START, PREVIEW_START.plusSeconds(3600)),
        ),
    isExpired = expired,
    hoursLeft = hoursLeft,
)

@Preview(name = "IncomingRequestCard pending")
@Composable
private fun IncomingRequestCardPendingPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomingRequestCard(previewRow(5, expired = false), onClick = {}, modifier = Modifier.padding(Spacing.md))
        }
    }
}

@Preview(name = "IncomingRequestCard expired")
@Composable
private fun IncomingRequestCardExpiredPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomingRequestCard(previewRow(null, expired = true), onClick = {}, modifier = Modifier.padding(Spacing.md))
        }
    }
}
