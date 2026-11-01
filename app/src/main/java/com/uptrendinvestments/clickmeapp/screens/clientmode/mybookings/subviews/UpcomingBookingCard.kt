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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Update
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.ActionButtonPair
import com.uptrendinvestments.clickmeapp.components.buttonstyles.RescheduleActionButton
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.components.chat.MessageButton
import com.uptrendinvestments.clickmeapp.components.chat.MessageButtonVariant
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

/** Test tag on the Join Call button of an Upcoming card: live, enabled by the join window (D-01..D-03). */
const val MY_BOOKINGS_JOIN_TAG = "my_bookings_join"

/** Test tag on the Message control of an Upcoming card, in both its icon and its text form (D-14). */
const val BOOKING_CARD_MESSAGE_TAG = "booking_card_message"

/** Test tag on the Reschedule button of an Upcoming card (Phase 16.2, D-05). */
const val BOOKING_CARD_RESCHEDULE_TAG = "booking_card_reschedule"

private val IconSize = 20.dp
private val MessageIconTarget = 56.dp
private val SmallIconSize = 16.dp
private val AvatarSize = 56.dp
private const val BADGE_FILL_ALPHA = 0.10f
private const val BADGE_BORDER_ALPHA = 0.45f
private val PREVIEW_START: Instant = Instant.parse("2026-10-24T18:00:00Z")

/**
 * An Upcoming booking card (iOS `UpcomingBookingCard`). The whole card opens Upcoming Booking Details (D-12). The
 * actions are Join Call, live within the join window (Android goes beyond iOS here, whose card passes an empty
 * `onJoinCall`, D-01), and Message (D-14): a 56 dp icon button beside Join, or the full-width text form on a
 * pending-approval card, which has no Join. Reschedule is offered when the caller passes [onReschedule] (Phase 16.2,
 * D-05, D-04 as amended 2026-11-01): a confirmed card shows Join full width and then Reschedule at the start with the
 * text-form Message at the end; a pending-approval card shows the banner and then that same Reschedule + Message row
 * (iOS arrangement). Private Note is still not on the card (D-16).
 *
 * @param joinEnabled whether the booking is inside the join window (`isWithinJoinWindow`).
 * @param onJoin starts the call; tapping Join does not open the details.
 * @param messageBusy true while the chat with this card's expert is being opened.
 * @param onMessage opens the chat with the expert; tapping Message does not open the details.
 * @param zone zone the date and times are shown in; defaults to the device zone.
 * @param timeFormat the device time format so the 12 or 24 hour setting applies.
 * @param onReschedule opens the reschedule picker; null (the default) means Reschedule is not offered for this card.
 *   Tapping it does not open the details.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UpcomingBookingCard(
    item: ClientBookingItem,
    onClick: () -> Unit,
    joinEnabled: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    messageBusy: Boolean = false,
    onMessage: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
    onReschedule: (() -> Unit)? = null,
) {
    val expertName = item.expert.fullName ?: stringResource(R.string.explore_expert_name_fallback)
    val messageEnabled = item.expert.id.isNotBlank()
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
                if (onReschedule != null) {
                    RescheduleMessageRow(
                        expertName = expertName,
                        onReschedule = onReschedule,
                        onMessage = onMessage,
                        messageBusy = messageBusy,
                        messageEnabled = messageEnabled,
                    )
                } else {
                    MessageButton(
                        label = stringResource(R.string.chat_entry_message),
                        onClick = onMessage,
                        busy = messageBusy,
                        enabled = messageEnabled,
                        modifier = Modifier.fillMaxWidth().testTag(BOOKING_CARD_MESSAGE_TAG),
                    )
                }
            } else if (onReschedule == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
                    JoinCallButton(joinEnabled, onJoin, Modifier.weight(1f).testTag(MY_BOOKINGS_JOIN_TAG))
                    MessageButton(
                        label = stringResource(R.string.chat_entry_message),
                        onClick = onMessage,
                        busy = messageBusy,
                        enabled = messageEnabled,
                        variant = MessageButtonVariant.Icon,
                        contentDescription = stringResource(R.string.chat_entry_message_name, expertName),
                        iconTarget = MessageIconTarget,
                        modifier = Modifier.testTag(BOOKING_CARD_MESSAGE_TAG),
                    )
                }
            } else {
                JoinCallButton(joinEnabled, onJoin, Modifier.fillMaxWidth().testTag(MY_BOOKINGS_JOIN_TAG))
                RescheduleMessageRow(
                    expertName = expertName,
                    onReschedule = onReschedule,
                    onMessage = onMessage,
                    messageBusy = messageBusy,
                    messageEnabled = messageEnabled,
                )
            }
        }
    }
}

/** The Reschedule + text-form Message row shared by the confirmed and pending-approval branches (D-05). */
@Composable
private fun RescheduleMessageRow(
    expertName: String,
    onReschedule: () -> Unit,
    onMessage: () -> Unit,
    messageBusy: Boolean,
    messageEnabled: Boolean,
) {
    ActionButtonPair(
        start = {
            RescheduleActionButton(
                onClick = onReschedule,
                contentDescription = stringResource(R.string.reschedule_start_action_a11y, expertName),
                modifier = it.testTag(BOOKING_CARD_RESCHEDULE_TAG),
            )
        },
        end = {
            MessageButton(
                label = stringResource(R.string.chat_entry_message),
                onClick = onMessage,
                busy = messageBusy,
                enabled = messageEnabled,
                modifier = it.testTag(BOOKING_CARD_MESSAGE_TAG),
            )
        },
        gap = Spacing.sm,
    )
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
            UpcomingBookingCard(
                item = previewItem("confirmed"),
                onClick = {},
                joinEnabled = true,
                onJoin = {},
                modifier = Modifier.padding(Spacing.md),
            )
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
                joinEnabled = false,
                onJoin = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
