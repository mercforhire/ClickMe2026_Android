package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.screens.call.calling.callRemainingChipText
import com.uptrendinvestments.clickmeapp.screens.call.calling.callTimerSpokenDescription
import com.uptrendinvestments.clickmeapp.screens.call.calling.callTimerText
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId

const val MEETING_CALL_TIMER_TAG = "meeting_call_timer"
const val MEETING_CALL_CHIP_TAG = "meeting_call_chip"
const val MEETING_CALL_STATUS_TAG = "meeting_call_status"
const val MEETING_CALL_NAME_TAG = "meeting_call_name"

private const val CHIP_FILL_ALPHA = 0.12f
private const val CHIP_BORDER_ALPHA = 0.45f

/** The status line is read first inside the call layer's traversal group (UI-SPEC Flow 3). */
private const val STATUS_TRAVERSAL_INDEX = -1f

/** The status line copy for a call state. */
@Composable
fun callStatusText(status: CallStatus): String =
    stringResource(
        when (status) {
            CallStatus.Connecting -> R.string.call_status_connecting
            CallStatus.WaitingForPeer -> R.string.call_status_waiting
            CallStatus.Active -> R.string.call_status_connected
            CallStatus.Ended -> R.string.call_status_ended
        },
    )

/**
 * The schedule-anchored timer plus the `M:SS left` chip in the final stretch. The timer turns tertiary while ending
 * soon or past the scheduled end. It is deliberately not a live region (it ticks every second); TalkBack reads the
 * spoken description only when the node is focused.
 */
@Composable
fun MeetingCallTimer(
    timer: CallTimerState,
    modifier: Modifier = Modifier,
) {
    val timerColor by animateColorAsState(
        targetValue =
            if (timer.isWarning) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
        label = "callTimerColor",
    )
    val spoken = callTimerSpokenDescription(timer)
    val chipText = callRemainingChipText(timer)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = callTimerText(timer),
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = timerColor,
            modifier =
                Modifier
                    .testTag(MEETING_CALL_TIMER_TAG)
                    .semantics { contentDescription = spoken },
        )
        AnimatedVisibility(visible = chipText != null) {
            val tertiary = MaterialTheme.colorScheme.tertiary
            Text(
                text = chipText.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = tertiary,
                modifier =
                    Modifier
                        .testTag(MEETING_CALL_CHIP_TAG)
                        .clip(CircleShape)
                        .background(tertiary.copy(alpha = CHIP_FILL_ALPHA), CircleShape)
                        .border(BorderStroke(1.dp, tertiary.copy(alpha = CHIP_BORDER_ALPHA)), CircleShape)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }
    }
}

/**
 * Name, topic, scheduled range, timer and status line under the hero. Stateless: the screen passes the resolved
 * [timer] and [statusText]. A null [statusText] hides the status line.
 *
 * The status line is the first node TalkBack reads (traversal index -1) and takes focus once when the layer first
 * shows, so a user who joins hears "Connecting" before anything else (UI-SPEC Flow 3). It is a polite live region;
 * the timer is not one.
 */
@Composable
fun MeetingCallInfo(
    peerName: String?,
    topic: String?,
    scheduledStart: Instant,
    scheduledEnd: Instant?,
    timer: CallTimerState,
    statusText: String?,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
) {
    val statusFocus = remember { FocusRequester() }
    val hasStatus = statusText != null
    // Keyed on whether the line exists, never on its text: focus moves once on entry, not on every status change.
    LaunchedEffect(hasStatus) {
        if (hasStatus) runCatching { statusFocus.requestFocus() }
    }
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.call_notification_title_fallback),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(MEETING_CALL_NAME_TAG).semantics { heading() },
        )
        if (!topic.isNullOrBlank()) {
            Text(
                text = topic,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (scheduledEnd != null) {
            Text(
                text = formatSessionTimeRange(scheduledStart, scheduledEnd, zone, timeFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        MeetingCallTimer(timer)
        if (statusText != null) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .testTag(MEETING_CALL_STATUS_TAG)
                        .focusRequester(statusFocus)
                        .focusable()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            traversalIndex = STATUS_TRAVERSAL_INDEX
                        },
            )
        }
    }
}
