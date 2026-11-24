package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews

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
import androidx.compose.material.icons.outlined.Schedule
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
import com.uptrendinvestments.clickmeapp.components.ExpertRescheduleBadge
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.booknote.PrivateNoteZone
import com.uptrendinvestments.clickmeapp.components.buttonstyles.ActionButtonPair
import com.uptrendinvestments.clickmeapp.components.buttonstyles.ActionButtonPairDefaults
import com.uptrendinvestments.clickmeapp.components.buttonstyles.RescheduleActionButton
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.components.chat.MessageButton
import com.uptrendinvestments.clickmeapp.components.chat.MessageButtonVariant
import com.uptrendinvestments.clickmeapp.managers.isStartingNow
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingClientDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingSessionDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertStatusMeta
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.StartingNowBadge
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_RESCHEDULE
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.expertShortBookingId
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.netEarningsText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on an expert Upcoming card. */
const val EXPERT_UPCOMING_CARD_TAG = "expert_bookings_upcoming_card"

/** Test tag on the Join button of an expert Upcoming card: enabled by the join window, live on the clock tick. */
const val EXPERT_BOOKINGS_JOIN_TAG = "expert_bookings_join"

/** Test tag on the Message icon button of an expert Upcoming card. */
const val EXPERT_BOOKINGS_MESSAGE_TAG = "expert_bookings_message"

/** Test tag on the Reschedule button of an expert Upcoming card (shown when the list row allows it). */
const val EXPERT_BOOKINGS_RESCHEDULE_TAG = "expert_bookings_reschedule"

private val MessageIconTarget = 56.dp
private val IconSize = 20.dp
private val AvatarSize = 56.dp
private val PREVIEW_START: Instant = Instant.parse("2026-10-24T18:00:00Z")

/**
 * An expert Upcoming booking card (iOS expert session card, UI-SPEC surface 5). The card body opens the expert Booking
 * Summary; Join is its own target, live within the join window (D-14). By default the icon-form Message sits beside
 * Join (Phase 12 D-14). Reschedule is shown when the caller passes [onReschedule] (Phase 16.2, gated by the list
 * row's `actions.can_reschedule`): Join then fills the first row and the second row holds the text-form Message at the
 * start and Reschedule at the end (iOS expert order). The note zone is the card's last child (Phase 16.4, D-07, iOS
 * session card); tapping it does not open the summary.
 *
 * @param now the shared join clock value; drives the Join button and the "Starting now" badge.
 * @param onOpen opens the summary; tapping Join does not.
 * @param onJoin starts the call with the client as the peer.
 * @param messageBusy true while the chat with this client is being opened; Message shows a spinner and ignores taps.
 * @param onMessage opens the chat with the client; tapping it does not open the summary.
 * @param onPrivateNote opens the private note editor for this booking; the card's own tap is not triggered.
 * @param onReschedule opens the expert Reschedule picker; null hides the control. Tapping it does not open the summary.
 * @param zone zone the date and times are shown in; defaults to the device zone.
 * @param timeFormat the device time format so the 12 or 24 hour setting applies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertUpcomingCard(
    item: ExpertBookingItem,
    now: Instant,
    onOpen: () -> Unit,
    onJoin: () -> Unit,
    messageBusy: Boolean,
    onMessage: () -> Unit,
    onPrivateNote: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
    onReschedule: (() -> Unit)? = null,
) {
    val earnings = item.netEarningsText(locale)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(EXPERT_UPCOMING_CARD_TAG)
                .clickable(
                    onClickLabel = stringResource(R.string.expert_bookings_open_details),
                    role = Role.Button,
                    onClick = onOpen,
                ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(url = item.client.avatarUrl, contentDescription = null, size = AvatarSize)
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = clientDisplayName(item.client.name),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.topic ?: stringResource(R.string.explore_session_topic_fallback),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                        Text(
                            text = stringResource(R.string.expert_summary_id, expertShortBookingId(item.bookingId)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                }
                if (earnings != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.expert_bookings_earnings),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                        Text(
                            text = earnings,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                if (isStartingNow(item.startTime, now)) StartingNowBadge()
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
            if (item.statusCode == STATUS_PENDING_RESCHEDULE) {
                ExpertRescheduleBadge(
                    initiatedByClient = item.reschedule?.initiatedBy == RESCHEDULE_INITIATED_BY_CLIENT,
                )
            }
            val clientName = clientDisplayName(item.client.name)
            val joinEnabled =
                isWithinJoinWindow(
                    status = item.statusCode,
                    start = item.startTime,
                    end = item.endTime,
                    now = now,
                    serverCanJoin = item.actions.canJoin,
                )
            if (onReschedule == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    JoinCallButton(
                        enabled = joinEnabled,
                        onClick = onJoin,
                        modifier = Modifier.weight(1f).testTag(EXPERT_BOOKINGS_JOIN_TAG),
                    )
                    val messageLabel = stringResource(R.string.chat_entry_message_name, clientName)
                    MessageButton(
                        label = messageLabel,
                        onClick = onMessage,
                        modifier = Modifier.testTag(EXPERT_BOOKINGS_MESSAGE_TAG),
                        busy = messageBusy,
                        variant = MessageButtonVariant.Icon,
                        contentDescription = messageLabel,
                        iconTarget = MessageIconTarget,
                    )
                }
            } else {
                JoinCallButton(
                    enabled = joinEnabled,
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth().testTag(EXPERT_BOOKINGS_JOIN_TAG),
                )
                ActionButtonPair(
                    start = {
                        MessageButton(
                            label = stringResource(R.string.chat_entry_message),
                            onClick = onMessage,
                            modifier = it.testTag(EXPERT_BOOKINGS_MESSAGE_TAG),
                            busy = messageBusy,
                            contentPadding = ActionButtonPairDefaults.ContentPadding,
                        )
                    },
                    end = {
                        RescheduleActionButton(
                            onClick = onReschedule,
                            modifier = it.testTag(EXPERT_BOOKINGS_RESCHEDULE_TAG),
                            contentDescription = stringResource(R.string.reschedule_start_action_a11y, clientName),
                        )
                    },
                    gap = Spacing.sm,
                )
            }
            PrivateNoteZone(
                preview = item.privateNotePreview,
                onClick = onPrivateNote,
                modifier = Modifier.fillMaxWidth(),
            )
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
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "ExpertUpcomingCard")
@Composable
private fun ExpertUpcomingCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertUpcomingCard(
                item =
                    ExpertBookingItem(
                        bookingId = "b1",
                        client = ExpertBookingClientDto(id = "c1", name = "Ada Lovelace"),
                        session =
                            ExpertBookingSessionDto(
                                topic = "Algorithms",
                                startTime = PREVIEW_START,
                                endTime = PREVIEW_START.plusSeconds(3600),
                            ),
                        status = ExpertStatusMeta(code = "confirmed"),
                    ),
                now = PREVIEW_START,
                onOpen = {},
                onJoin = {},
                messageBusy = false,
                onMessage = {},
                onPrivateNote = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
