package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test tag on the Join Call button of Upcoming Booking Details: live, enabled by the join window (D-01..D-03). */
const val UPCOMING_BOOKING_JOIN_TAG = "upcoming_booking_join"

/** Test tag on the copy button of the Skype / Zoom meeting link row. */
const val UPCOMING_BOOKING_COPY_LINK_TAG = "upcoming_booking_copy_link"

private val IconSize = 20.dp
private val LinkIconSize = 24.dp
private val LinkTileSize = 48.dp
private const val COPIED_RESET_MS = 2_000L

/**
 * Block 8: how to join. A live Join Call for both meeting types (D-01), enabled by [joinEnabled] (the shared join
 * window, D-02 / D-03). A Skype / Zoom booking keeps the meeting-link row with its copy button and gets Join Call
 * below it (UI-SPEC A3). The link is displayed and copied on an explicit tap only; opening it happens from the call
 * layer (D-05, T-08-43).
 */
@Composable
fun UpcomingBookingJoinCard(
    meetingType: String?,
    joinLink: String?,
    joinEnabled: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            if (meetingType == MeetingType.SkypeZoom.wire) {
                MeetingLinkRow(joinLink)
                Spacer(Modifier.height(Spacing.md))
            }
            JoinCallButton(
                enabled = joinEnabled,
                onClick = onJoin,
                modifier = Modifier.testTag(UPCOMING_BOOKING_JOIN_TAG),
            )
        }
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
                UpcomingBookingJoinCard("in_app_voice", null, joinEnabled = true, onJoin = {})
                Spacer(Modifier.size(Spacing.md))
                UpcomingBookingJoinCard("skype_zoom", "https://zoom.us/j/123456789", joinEnabled = false, onJoin = {})
                Spacer(Modifier.size(Spacing.md))
                UpcomingBookingPendingBanner("Ada Lovelace")
            }
        }
    }
}
