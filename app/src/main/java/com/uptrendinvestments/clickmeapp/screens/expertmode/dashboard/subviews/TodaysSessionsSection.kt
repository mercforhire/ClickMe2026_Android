package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews

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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ExpertRescheduleBadge
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.managers.isStartingNow
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews.StartingNowBadge
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.hasClientProposedReschedule
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId

/** Test tag on the "View Calendar" action. */
const val DASHBOARD_VIEW_CALENDAR_TAG = "dashboard_view_calendar"

/** Test tag on the Join Call button of a Today's Sessions card. */
const val DASHBOARD_SESSION_JOIN_TAG = "dashboard_session_join"

private val AvatarSize = 48.dp
private val RowIconSize = 20.dp
private val EmptyIconSize = 32.dp
private val ActionMinHeight = 48.dp

/** "Today's Sessions" heading with the "View Calendar" text action that selects the Bookings tab (D-12). */
@Composable
fun TodaysSessionsHeader(
    onViewCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_todays_sessions),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            onClick = onViewCalendar,
            modifier = Modifier.heightIn(min = ActionMinHeight).testTag(DASHBOARD_VIEW_CALENDAR_TAG),
        ) {
            Text(
                text = stringResource(R.string.dashboard_view_calendar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * One of today's sessions (UI-SPEC surface 1, block 4). The card body is one button that opens the booking Summary;
 * the Join button is its own target so a Join tap never opens the Summary. The button is enabled by the shared join
 * window evaluated at [now], and a "Starting now" badge shows within the window around the start.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodaysSessionCard(
    item: ExpertBookingItem,
    now: Instant,
    onOpen: () -> Unit,
    onJoin: () -> Unit,
    zone: ZoneId,
    timeFormat: DateFormat,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = stringResource(R.string.dashboard_open_booking),
                            role = Role.Button,
                            onClick = onOpen,
                        ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(url = item.client.avatarUrl, contentDescription = null, size = AvatarSize)
                        Column {
                            Text(
                                text = clientDisplayName(item.client.name),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            item.topic?.takeIf { it.isNotBlank() }?.let { topic ->
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.extras.onSurfaceMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (isStartingNow(item.startTime, now)) StartingNowBadge()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(RowIconSize),
                        tint = MaterialTheme.extras.onSurfaceMuted,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = formatSessionTimeRange(item.startTime, item.endTime, zone, timeFormat),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (item.hasClientProposedReschedule()) ExpertRescheduleBadge(initiatedByClient = true)
            }
            JoinCallButton(
                enabled = isWithinJoinWindow(item.statusCode, item.startTime, item.endTime, now),
                onClick = onJoin,
                modifier = Modifier.testTag(DASHBOARD_SESSION_JOIN_TAG),
            )
        }
    }
}

/** The empty Today's Sessions card (UI-SPEC surface 1, block 4): "No sessions today" with the breather line. */
@Composable
fun TodaysSessionsEmpty(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(EmptyIconSize),
                tint = MaterialTheme.extras.onSurfaceMuted,
            )
            Text(
                text = stringResource(R.string.dashboard_no_sessions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.dashboard_no_sessions_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
