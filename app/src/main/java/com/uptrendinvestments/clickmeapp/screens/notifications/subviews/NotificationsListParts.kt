package com.uptrendinvestments.clickmeapp.screens.notifications.subviews

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationDay
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the permission-off row. */
const val NOTIFICATIONS_OFF_ROW_TAG = "notifications_off_row"

/** Test tag on the end-of-list note. */
const val NOTIFICATIONS_END_NOTE_TAG = "notifications_end_note"

/** The "Today" / "Yesterday" / "Earlier" header of a date section. */
@Composable
fun NotificationSectionHeader(
    day: NotificationDay,
    modifier: Modifier = Modifier,
) {
    val label =
        stringResource(
            when (day) {
                NotificationDay.Today -> R.string.notifications_section_today
                NotificationDay.Yesterday -> R.string.notifications_section_yesterday
                NotificationDay.Earlier -> R.string.notifications_section_earlier
            },
        )
    Text(
        text = label,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The closing note under the last section: only page 1 is fetched, so the list ends here (D-16). */
@Composable
fun NotificationsEndNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.notifications_end_note),
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.md).testTag(NOTIFICATIONS_END_NOTE_TAG),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.extras.onSurfaceMuted,
        textAlign = TextAlign.Center,
    )
}

/**
 * The hint row shown while system notifications are off (D-02): one `Role.Button` that merges to "Notifications are
 * off — Turn on" and opens the system settings. "Turn on" is neutral and underlined, not accent.
 */
@Composable
fun NotificationsOffRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.notifications_off_description)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(NOTIFICATIONS_OFF_ROW_TAG)
                .clickable(
                    onClickLabel = stringResource(R.string.notifications_off_click_label),
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) { contentDescription = description },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.notifications_off_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.notifications_off_action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

/**
 * Opens the system notification settings for this app. If no activity resolves that action, falls back to the app's
 * details screen; if that fails too, does nothing (UI-SPEC E3 error). Never throws.
 */
fun openNotificationSettings(context: Context) {
    val primary =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(primary)
        return
    } catch (_: ActivityNotFoundException) {
        // Fall through to the app details screen.
    }
    val fallback =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(fallback)
    } catch (_: ActivityNotFoundException) {
        // Nothing can show settings on this device: leave the row as it is.
    }
}

/**
 * The first-load placeholder: a section-header bar, three row blocks that copy the real row, then a second header bar
 * and two more rows. One "Loading" node for TalkBack. The permission-off row is not part of it.
 */
@Composable
fun NotificationsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SkeletonBlock(Modifier.width(80.dp).height(20.dp))
            repeat(3) { NotificationSkeletonRow() }
            SkeletonBlock(Modifier.padding(top = Spacing.md).width(80.dp).height(20.dp))
            repeat(2) { NotificationSkeletonRow() }
        }
    }
}

@Composable
private fun NotificationSkeletonRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            SkeletonBlock(Modifier.size(48.dp), shape = CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SkeletonBlock(Modifier.width(200.dp).height(16.dp))
                SkeletonBlock(Modifier.width(240.dp).height(16.dp))
                SkeletonBlock(Modifier.padding(top = Spacing.xs).width(96.dp).height(12.dp))
            }
        }
    }
}

@Preview
@Composable
private fun NotificationsOffRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            NotificationsOffRow(onClick = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
