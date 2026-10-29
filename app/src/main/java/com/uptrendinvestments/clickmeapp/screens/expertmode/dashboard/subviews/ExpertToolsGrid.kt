package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val CellMinHeight = 112.dp
private val TileSize = 48.dp
private val GlyphSize = 24.dp
private const val TILE_ALPHA = 0.12f
private const val LABEL_MAX_LINES = 2
private const val TOOLS_PER_ROW = 2

/**
 * The six Expert Tools in display order (D-11, UI-SPEC A2). [tag] completes the test tag `dashboard_tool_{tag}`.
 * Profile Settings opens Expert Settings and Payouts opens Payouts (Phase 14).
 */
enum class ExpertTool(
    @StringRes val label: Int,
    val icon: ImageVector,
    val tag: String,
) {
    PendingRequests(R.string.dashboard_tool_pending, Icons.Outlined.Inbox, "pending"),
    Calendar(R.string.dashboard_tool_calendar, Icons.Outlined.CalendarMonth, "calendar"),
    Availability(R.string.dashboard_tool_availability, Icons.Outlined.EventAvailable, "availability"),
    Topics(R.string.dashboard_tool_topics, Icons.Outlined.EditNote, "topics"),
    ProfileSettings(R.string.dashboard_tool_profile, Icons.Outlined.Settings, "profile"),
    Payouts(R.string.dashboard_tool_payouts, Icons.Outlined.Payments, "payouts"),
}

/** Test tag of the grid cell for [tool]. */
fun expertToolTag(tool: ExpertTool): String = "dashboard_tool_${tool.tag}"

/** "Expert Tools" heading over three rows of two cells with `md` gaps. */
@Composable
fun ExpertToolsGrid(
    onToolClick: (ExpertTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_expert_tools),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ExpertTool.entries.chunked(TOOLS_PER_ROW).forEach { rowTools ->
                // Equal-height cells so a two-line label does not make its neighbour shorter.
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    rowTools.forEach { tool ->
                        ExpertToolCell(
                            tool = tool,
                            onClick = { onToolClick(tool) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/** One Expert Tools cell: a centered 48 dp tile with the glyph over a label of up to two lines, one button. */
@Composable
fun ExpertToolCell(
    tool: ExpertTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier =
            modifier
                .testTag(expertToolTag(tool))
                .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().heightIn(min = CellMinHeight).padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TileSize)
                        .background(primary.copy(alpha = TILE_ALPHA), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    modifier = Modifier.size(GlyphSize),
                    tint = primary,
                )
            }
            Text(
                text = stringResource(tool.label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = LABEL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
