package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.managers.isStartingNow
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.TodaysSessionUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Instant
import java.time.ZoneId

/** Test tag on the Join Call button of a Today's Sessions card. */
const val TODAYS_SESSION_JOIN_TAG = "todays_session_join"

/** Test tag on the time range text of a Today's Sessions card. */
const val TODAYS_SESSION_TIME_TAG = "todays_session_time"

private val IconSize = 20.dp
private val SessionAvatarSize = 40.dp
private val PREVIEW_START: Instant = Instant.parse("2026-10-21T19:00:00Z")

/**
 * Today's Sessions strip (D-01). Renders nothing for an empty list. Each card is one click target that reports its
 * booking id through [onSessionClick] (Phase 8: Upcoming Booking Details). The Join Call button inside it is live
 * (D-01..D-03): enabled by the shared join window evaluated at [now], it reports the session through [onJoin] and
 * consumes its own click, so the card click never fires for a Join tap. A "Starting now" badge sits beside the
 * expert name while the start is within the window.
 */
@Composable
fun ExploreTodaysSessionsSection(
    sessions: List<TodaysSessionUi>,
    now: Instant,
    onSessionClick: (bookingId: String) -> Unit,
    onJoin: (TodaysSessionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.explore_section_todays_sessions))
        Spacer(Modifier.height(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            sessions.forEach { session ->
                TodaysSessionCard(
                    session = session,
                    now = now,
                    onClick = { onSessionClick(session.bookingId) },
                    onJoin = { onJoin(session) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodaysSessionCard(
    session: TodaysSessionUi,
    now: Instant,
    onClick: () -> Unit,
    onJoin: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(R.string.my_bookings_view_details),
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = session.expertAvatarUrl, contentDescription = null, size = SessionAvatarSize)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    // FlowRow so the badge wraps below the name at large font scales.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = session.expertName ?: stringResource(R.string.explore_expert_name_fallback),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isStartingNow(session.startTime, now)) StartingNowBadge()
                    }
                    Text(
                        text = session.topic ?: stringResource(R.string.explore_session_topic_fallback),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = MaterialTheme.extras.onSurfaceMuted,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = sessionTimeRange(session),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(TODAYS_SESSION_TIME_TAG),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            JoinCallButton(
                // The client home feed carries no can_join, so the reconnect tier stays closed.
                enabled =
                    isWithinJoinWindow(
                        status = session.status,
                        start = session.startTime,
                        end = session.endTime,
                        now = now,
                        serverCanJoin = null,
                    ),
                onClick = onJoin,
                modifier = Modifier.testTag(TODAYS_SESSION_JOIN_TAG),
            )
        }
    }
}

/**
 * The session's time range in the device time format, so the user's 12 or 24 hour setting applies, not only the
 * locale default. Formatted in composition rather than in the ViewModel, which outlives the activity recreation a
 * locale change causes, so the text follows the new locale at once.
 */
@Composable
private fun sessionTimeRange(session: TodaysSessionUi): String {
    val context = LocalContext.current
    return formatSessionTimeRange(
        start = session.startTime,
        end = session.endTime,
        zone = ZoneId.systemDefault(),
        timeFormat = DateFormat.getTimeFormat(context),
    )
}

@Preview(name = "ExploreTodaysSessionsSection")
@Composable
private fun ExploreTodaysSessionsSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExploreTodaysSessionsSection(
                sessions =
                    listOf(
                        TodaysSessionUi(
                            bookingId = "b1",
                            expertName = "Ada Lovelace",
                            expertAvatarUrl = null,
                            topic = "Algorithms",
                            startTime = PREVIEW_START,
                            endTime = PREVIEW_START.plusSeconds(3600),
                        ),
                    ),
                now = PREVIEW_START.minusSeconds(600),
                onSessionClick = {},
                onJoin = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
