package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.ClientHistoryBucket
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.ClientHistoryRow
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.ZoneId
import java.util.Locale

/** Test tag on each history row. */
const val CLIENT_PROFILE_HISTORY_ROW_TAG = "client_profile_history_row"

private val TileSize = 44.dp
private val TileIconSize = 24.dp
private const val TILE_FILL_ALPHA = 0.15f
private const val TILE_BORDER_ALPHA = 0.45f
private const val UPCOMING_BORDER_ALPHA = 0.55f
private const val TITLE_MAX_LINES = 2

/** The "Booking History" heading. The caller hides the section when there are no rows. */
@Composable
fun ClientProfileBookingHistoryHeader(modifier: Modifier = Modifier) {
    ClientProfileSectionHeading(stringResource(R.string.client_profile_history_title), modifier)
}

/**
 * One read-only history row (iOS `ClientProfileBookingCard`): a status tile, the topic, "{bucket} - {date}" and, when
 * [ClientHistoryRow.amountText] has one, the expert's net earnings on the right (D-09; iOS shows a stub "$0").
 *
 * The row is plain: no click action, no ripple, no button role (D-10). It shows three bucket labels, not per-status
 * wording, so `BookingStatusBadge` is not used. Upcoming rows get a 1 dp primary border at 0.55 alpha in place of the
 * iOS glow (UI-SPEC C6). The whole row is one TalkBack node reading "{title}. {bucket}, {date}", plus
 * "Earnings {amount}" only when an amount shows (UI-SPEC C7). Server text stays plain [Text] (T-16.4-23).
 */
@Composable
fun ClientProfileBookingHistoryRow(
    row: ClientHistoryRow,
    zone: ZoneId,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val title = row.item.topic?.takeIf { it.isNotBlank() } ?: stringResource(R.string.client_profile_session_fallback)
    val bucket = bucketLabel(row.bucket)
    val date =
        formatShortDate(
            row.item.startTime
                .atZone(zone)
                .toLocalDate(),
            locale,
        )
    val amount = row.amountText(locale)
    val spoken = stringResource(R.string.client_profile_row_a11y, title, bucket, date)
    val earnings = stringResource(R.string.expert_bookings_earnings)
    val description =
        if (amount ==
            null
        ) {
            spoken
        } else {
            stringResource(R.string.client_profile_row_a11y_earnings, spoken, earnings, amount)
        }
    val tint = bucketTint(row.bucket)
    val border =
        if (row.bucket == ClientHistoryBucket.Upcoming) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = UPCOMING_BORDER_ALPHA))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ClientProfileCard(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(CLIENT_PROFILE_HISTORY_ROW_TAG)
                .clearAndSetSemantics { contentDescription = description },
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusTile(bucketIcon(row.bucket), tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.client_profile_row_subtitle, bucket, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (amount != null) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun StatusTile(
    icon: ImageVector,
    tint: Color,
) {
    Box(
        modifier =
            Modifier
                .size(TileSize)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(tint.copy(alpha = TILE_FILL_ALPHA))
                .border(1.dp, tint.copy(alpha = TILE_BORDER_ALPHA), MaterialTheme.shapes.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(TileIconSize),
            tint = tint,
        )
    }
}

@Composable
private fun bucketLabel(bucket: ClientHistoryBucket): String =
    stringResource(
        when (bucket) {
            ClientHistoryBucket.Upcoming -> R.string.client_profile_bucket_upcoming
            ClientHistoryBucket.Completed -> R.string.client_profile_bucket_completed
            ClientHistoryBucket.Cancelled -> R.string.client_profile_bucket_cancelled
        },
    )

private fun bucketIcon(bucket: ClientHistoryBucket): ImageVector =
    when (bucket) {
        ClientHistoryBucket.Upcoming -> Icons.Outlined.CalendarMonth
        ClientHistoryBucket.Completed -> Icons.Filled.CheckCircle
        ClientHistoryBucket.Cancelled -> Icons.Filled.Cancel
    }

@Composable
private fun bucketTint(bucket: ClientHistoryBucket): Color =
    if (bucket == ClientHistoryBucket.Cancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

@Preview(name = "ClientProfileBookingHistoryHeader")
@Composable
private fun ClientProfileBookingHistoryHeaderPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ClientProfileBookingHistoryHeader(Modifier.padding(Spacing.md))
        }
    }
}
