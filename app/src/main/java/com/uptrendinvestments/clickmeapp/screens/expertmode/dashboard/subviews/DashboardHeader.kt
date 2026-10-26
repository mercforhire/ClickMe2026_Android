package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the pending-requests banner. */
const val DASHBOARD_PENDING_BANNER_TAG = "dashboard_pending_banner"

/** Test tag on the reschedule-requests banner. */
const val DASHBOARD_RESCHEDULE_BANNER_TAG = "dashboard_reschedule_banner"

private val TileSize = 48.dp
private val TileIconSize = 24.dp
private const val TILE_ALPHA = 0.12f
private const val BANNER_FILL_ALPHA = 0.10f
private const val BANNER_BORDER_ALPHA = 0.45f
private const val VISIBILITY_MS = Motion.STRENGTH_METER_VISIBILITY_MS

/**
 * The greeting at the top of the Dashboard (UI-SPEC surface 1, block 1): the "Dashboard overview" caption over
 * "Hello, {first name}". A null or blank [firstName] reads "Hello, there".
 */
@Composable
fun DashboardHero(
    firstName: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_overview),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text =
                if (firstName.isNullOrBlank()) {
                    stringResource(R.string.dashboard_hello_there)
                } else {
                    stringResource(R.string.dashboard_hello, firstName)
                },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * Pending-requests banner (EXP-05, D-11): "{n} Pending Requests" with a chevron, one button that opens Incoming
 * Requests. At zero it reads "No pending requests" and stays tappable so the expert can still open the empty list.
 */
@Composable
fun PendingRequestsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPending = count > 0
    DashboardBanner(
        icon = Icons.Outlined.NotificationsNone,
        title =
            if (hasPending) {
                pluralStringResource(R.plurals.dashboard_pending_requests, count, count)
            } else {
                stringResource(R.string.dashboard_pending_zero_title)
            },
        subtitle =
            stringResource(
                if (hasPending) R.string.dashboard_pending_subtitle else R.string.dashboard_pending_zero_subtitle,
            ),
        onClickLabel = stringResource(R.string.dashboard_review_requests),
        onClick = onClick,
        accent = MaterialTheme.colorScheme.primary,
        container = MaterialTheme.colorScheme.surfaceContainerLow,
        border = MaterialTheme.colorScheme.outlineVariant,
        subtitleColor = MaterialTheme.extras.onSurfaceMuted,
        modifier = modifier.testTag(DASHBOARD_PENDING_BANNER_TAG),
    )
}

/**
 * "N Reschedule Requests" banner (D-12): shown only while [count] (client-proposed reschedules) is above zero, with a
 * 200 ms fade and expand. It keeps the last positive count while it animates out so the title never flashes "0". The
 * `lg` gap to the block above lives inside the animated content, so a hidden banner leaves no empty gap.
 */
@Composable
fun RescheduleRequestsBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shownCount by remember { mutableIntStateOf(count) }
    if (count > 0) shownCount = count
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = fadeIn(tween(VISIBILITY_MS)) + expandVertically(tween(VISIBILITY_MS)),
        exit = fadeOut(tween(VISIBILITY_MS)) + shrinkVertically(tween(VISIBILITY_MS)),
    ) {
        val tertiary = MaterialTheme.colorScheme.tertiary
        DashboardBanner(
            icon = Icons.Outlined.Update,
            title = pluralStringResource(R.plurals.dashboard_reschedule_requests, shownCount, shownCount),
            subtitle = stringResource(R.string.dashboard_reschedule_subtitle),
            onClickLabel = stringResource(R.string.dashboard_review_reschedules),
            onClick = onClick,
            accent = tertiary,
            container = tertiary.copy(alpha = BANNER_FILL_ALPHA),
            border = tertiary.copy(alpha = BANNER_BORDER_ALPHA),
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.lg).testTag(DASHBOARD_RESCHEDULE_BANNER_TAG),
        )
    }
}

/**
 * One tappable banner row (UI-SPEC surface 1, blocks 2 and 3): a 48 dp icon tile tinted with [accent], a title and
 * subtitle, and a chevron. The whole card is a single button with [onClickLabel] so TalkBack announces one action.
 */
@Composable
internal fun DashboardBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClickLabel: String,
    onClick: () -> Unit,
    accent: Color,
    container: Color,
    border: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TileSize)
                        .background(accent.copy(alpha = TILE_ALPHA), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(TileIconSize),
                    tint = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(TileIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "DashboardHero and PendingRequestsBanner")
@Composable
private fun DashboardHeaderPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                DashboardHero(firstName = "Ada")
                PendingRequestsBanner(count = 3, onClick = {})
                PendingRequestsBanner(count = 0, onClick = {})
            }
        }
    }
}
