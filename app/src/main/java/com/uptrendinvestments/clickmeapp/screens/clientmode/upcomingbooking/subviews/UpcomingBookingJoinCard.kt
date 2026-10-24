package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test tag on the (always disabled) Join Call button of Upcoming Booking Details. */
const val UPCOMING_BOOKING_JOIN_TAG = "upcoming_booking_join"

/** Test tag on the copy button of the Skype / Zoom meeting link row. */
const val UPCOMING_BOOKING_COPY_LINK_TAG = "upcoming_booking_copy_link"

private val IconSize = 20.dp
private val LinkIconSize = 24.dp
private val LinkTileSize = 48.dp
private val JoinButtonMinHeight = 56.dp
private const val COPIED_RESET_MS = 2_000L

/**
 * Block 8: how to join. Inert in Phase 8 (D-02, UI-SPEC A2): an in-app voice booking shows a disabled Join Call, and a
 * Skype / Zoom booking shows the meeting link with a copy button. The link is displayed and copied on an explicit tap
 * only; it is never opened from here (T-08-43, Phase 9).
 */
@Composable
fun UpcomingBookingJoinCard(
    meetingType: String?,
    joinLink: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.padding(Spacing.md)) {
            if (meetingType == MeetingType.SkypeZoom.wire) {
                MeetingLinkRow(joinLink)
            } else {
                DisabledJoinButton()
            }
        }
    }
}

@Composable
private fun DisabledJoinButton() {
    val joinState = stringResource(R.string.explore_join_call_state)
    // PHASE-9: join window + click (D-02, D-12)
    PressScaleButton(
        onClick = {},
        enabled = false,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = JoinButtonMinHeight)
                .testTag(UPCOMING_BOOKING_JOIN_TAG)
                .semantics { stateDescription = joinState },
    ) {
        Icon(Icons.Outlined.Videocam, contentDescription = null, modifier = Modifier.size(IconSize))
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.explore_join_call))
    }
}

@Composable
private fun MeetingLinkRow(joinLink: String?) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val hasLink = !joinLink.isNullOrBlank()
    val label = stringResource(R.string.upcoming_booking_meeting_link)

    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_RESET_MS)
            copied = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(LinkTileSize),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(LinkIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    if (!joinLink.isNullOrBlank()) {
                        joinLink
                    } else {
                        stringResource(
                            R.string.upcoming_booking_meeting_link_unavailable,
                        )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
        IconButton(
            onClick = {
                if (joinLink != null) {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, joinLink)))
                        copied = true
                    }
                }
            },
            enabled = hasLink,
            modifier = Modifier.size(LinkTileSize).testTag(UPCOMING_BOOKING_COPY_LINK_TAG),
        ) {
            Icon(
                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription =
                    stringResource(
                        if (copied) R.string.upcoming_booking_link_copied else R.string.upcoming_booking_copy_link,
                    ),
                modifier = Modifier.size(IconSize),
            )
        }
    }
}

/** Block 7 alternative: the booking awaits expert approval, so there is nothing to join yet. */
@Composable
fun UpcomingBookingPendingBanner(
    expertName: String?,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = tertiary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, tertiary.copy(alpha = 0.45f)),
    ) {
        Row(modifier = Modifier.padding(Spacing.md)) {
            Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(LinkIconSize),
                tint = tertiary,
            )
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = stringResource(R.string.upcoming_booking_pending_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(Spacing.xs))
                Text(
                    text =
                        stringResource(
                            R.string.upcoming_booking_pending_body,
                            expertName ?: stringResource(R.string.upcoming_booking_pending_expert_fallback),
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "UpcomingBookingJoinCard")
@Composable
private fun UpcomingBookingJoinCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                UpcomingBookingJoinCard("in_app_voice", null)
                Spacer(Modifier.size(Spacing.md))
                UpcomingBookingJoinCard("skype_zoom", "https://zoom.us/j/123456789")
                Spacer(Modifier.size(Spacing.md))
                UpcomingBookingPendingBanner("Ada Lovelace")
            }
        }
    }
}
