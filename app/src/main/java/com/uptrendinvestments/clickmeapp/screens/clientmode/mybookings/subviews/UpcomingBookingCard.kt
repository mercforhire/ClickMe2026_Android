package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_EXPERT
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_APPROVAL
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on an Upcoming card. */
const val UPCOMING_BOOKING_CARD_TAG = "my_bookings_upcoming_card"

/** Test tag on the (always disabled) Join placeholder of an Upcoming card. */
const val MY_BOOKINGS_JOIN_TAG = "my_bookings_join"

private val IconSize = 20.dp
private val SmallIconSize = 16.dp
private val AvatarSize = 56.dp
private val JoinButtonMinHeight = 56.dp
private const val BADGE_FILL_ALPHA = 0.10f
private const val BADGE_BORDER_ALPHA = 0.45f
private val PREVIEW_START: Instant = Instant.parse("2026-10-24T18:00:00Z")

/**
 * An Upcoming booking card (iOS `UpcomingBookingCard`). The whole card opens Upcoming Booking Details (D-12). The only
 * action is the Join placeholder: Reschedule, Message and Private Note are not on the card (D-12, D-16), and Join stays
 * disabled until Phase 9 adds the join window.
 *
 * @param zone zone the date and times are shown in; defaults to the device zone.
 * @param timeFormat the device time format so the 12 or 24 hour setting applies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpcomingBookingCard(
    item: ClientBookingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(UPCOMING_BOOKING_CARD_TAG)
                .clickable(
                    onClickLabel = stringResource(R.string.my_bookings_view_details),
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = item.expert.avatarUrl, contentDescription = null, size = AvatarSize)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.expert.fullName ?: stringResource(R.string.explore_expert_name_fallback),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.topic ?: stringResource(R.string.explore_session_topic_fallback),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                IconText(
                    icon = Icons.Outlined.CalendarMonth,
                    text = formatShortDate(item.startTime.atZone(zone).toLocalDate(), locale),
                )
                IconText(
                    icon = Icons.Outlined.Schedule,
                    text = formatSessionTimeRange(item.startTime, item.endTime, zone, timeFormat),
                )
            }
            item.reschedule?.let { reschedule ->
                val expertProposed = reschedule.initiatedBy == RESCHEDULE_INITIATED_BY_EXPERT
                TintedNotice(
                    icon = Icons.Outlined.Update,
                    text =
                        stringResource(
                            if (expertProposed) {
                                R.string.my_bookings_badge_expert_proposed
                            } else {
                                R.string.my_bookings_badge_client_proposed
                            },
                        ),
                )
            }
            if (item.status == STATUS_PENDING_APPROVAL) {
                TintedNotice(
                    icon = Icons.Outlined.HourglassEmpty,
                    text = stringResource(R.string.my_bookings_pending_banner),
                )
            } else {
                JoinPlaceholder()
            }
        }
    }
}

@Composable
private fun IconText(
    icon: ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Tertiary tinted row used for the reschedule badge and the pending-approval banner. */
@Composable
private fun TintedNotice(
    icon: ImageVector,
    text: String,
) {
    val tint = MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = tint.copy(alpha = BADGE_FILL_ALPHA),
        border = BorderStroke(1.dp, tint.copy(alpha = BADGE_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(SmallIconSize),
                tint = tint,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun JoinPlaceholder() {
    val joinState = stringResource(R.string.explore_join_call_state)
    // PHASE-9: join window + click (D-02, D-12)
    PressScaleButton(
        onClick = {},
        enabled = false,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = JoinButtonMinHeight)
                .testTag(MY_BOOKINGS_JOIN_TAG)
                .semantics { stateDescription = joinState },
    ) {
        Icon(
            imageVector = Icons.Outlined.Videocam,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.explore_join_call))
    }
}

private fun previewItem(status: String): ClientBookingItem =
    ClientBookingItem(
        bookingId = "b1",
        expert = BookingExpertDto(id = "e1", fullName = "Ada Lovelace"),
        topic = "Algorithms",
        startTime = PREVIEW_START,
        endTime = PREVIEW_START.plusSeconds(3600),
        status = status,
    )

@Preview(name = "UpcomingBookingCard confirmed")
@Composable
private fun UpcomingBookingCardConfirmedPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            UpcomingBookingCard(item = previewItem("confirmed"), onClick = {}, modifier = Modifier.padding(Spacing.md))
        }
    }
}

@Preview(name = "UpcomingBookingCard pending approval")
@Composable
private fun UpcomingBookingCardPendingPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            UpcomingBookingCard(
                item = previewItem(STATUS_PENDING_APPROVAL),
                onClick = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
