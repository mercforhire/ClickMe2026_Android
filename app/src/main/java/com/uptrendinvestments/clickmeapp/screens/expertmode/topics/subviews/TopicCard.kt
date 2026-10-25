package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.constants.CategoryIconMap
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicPrice
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.topicDurationText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

private val IconTileSize = 40.dp
private val GlyphSize = 24.dp
private val TrailingSize = 48.dp
private val SpinnerStroke = 2.dp
private val BorderWidth = 1.dp
private const val TILE_FILL_ALPHA = 0.12f

/** Test tag on a topic card's merged clickable node. */
fun topicCardTag(id: String) = "topic_card_$id"

/** Test tag on a topic card's overflow (three dots) button. */
fun topicOverflowTag(id: String) = "topic_overflow_$id"

/**
 * One of the expert's topics (EXP-01, D-05): icon tile, title, price ("Free" in primary) and
 * "Per session · {duration}". The whole card opens the editor; the overflow menu offers Edit and Delete. While this
 * card's delete is in flight ([isDeleting]) the overflow becomes a spinner and the card stops being tappable;
 * [menuEnabled] is false for every card while any delete runs (one operation at a time, D-06).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicCard(
    item: ExpertTopicItem,
    isDeleting: Boolean,
    menuEnabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currency = TopicPrice.resolveCurrency(item.storedCurrency, null)
    val priceDisplay = PriceDisplay.of(item.isFree, item.amountMinor, currency)
    val priceText =
        when (priceDisplay) {
            PriceDisplay.Free -> stringResource(R.string.price_free)
            is PriceDisplay.Paid -> priceDisplay.text
            PriceDisplay.Unknown -> stringResource(R.string.price_unknown)
        }
    val durationText =
        item.durationMins?.let { topicDurationText(it) } ?: stringResource(R.string.topics_per_session_plain)
    val perSessionText =
        item.durationMins?.let { stringResource(R.string.topics_per_session, durationText) } ?: durationText
    val cardDescription = stringResource(R.string.topics_card_description, item.title, priceText, durationText)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box {
            // The merged, clickable half of the card. The overflow control below is a sibling so it stays its own
            // focus stop.
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(topicCardTag(item.id))
                        .clickable(
                            enabled = !isDeleting,
                            onClickLabel = stringResource(R.string.topics_card_click_label),
                            role = Role.Button,
                            onClick = onClick,
                        ).semantics(mergeDescendants = true) { contentDescription = cardDescription }
                        .padding(Spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                IconTile(item.iconSlug)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        itemVerticalAlignment = Alignment.Bottom,
                    ) {
                        PriceText(display = priceDisplay, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = perSessionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
                // Room for the overlaid overflow control.
                Spacer(Modifier.width(TrailingSize))
            }
            Trailing(
                item = item,
                isDeleting = isDeleting,
                menuEnabled = menuEnabled,
                onEdit = onEdit,
                onDelete = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = Spacing.sm, end = Spacing.sm),
            )
        }
    }
}

@Composable
private fun IconTile(iconSlug: String?) {
    Box(
        modifier =
            Modifier
                .size(IconTileSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = TILE_FILL_ALPHA),
                    shape = MaterialTheme.shapes.small,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = iconSlug?.let { CategoryIconMap.icon(it) } ?: CategoryIconMap.fallback,
            contentDescription = null,
            modifier = Modifier.size(GlyphSize),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Trailing(
    item: ExpertTopicItem,
    isDeleting: Boolean,
    menuEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDeleting) {
        val deleting = stringResource(R.string.topics_deleting)
        Box(
            modifier = modifier.size(TrailingSize).semantics(mergeDescendants = true) { stateDescription = deleting },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(GlyphSize),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = SpinnerStroke,
            )
        }
    } else {
        OverflowMenu(item = item, enabled = menuEnabled, onEdit = onEdit, onDelete = onDelete, modifier = modifier)
    }
}

@Composable
private fun OverflowMenu(
    item: ExpertTopicItem,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    val moreOptions = stringResource(R.string.topics_more_options, item.title)
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(TrailingSize).testTag(topicOverflowTag(item.id)),
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = moreOptions,
                modifier = Modifier.size(GlyphSize),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            MenuItem(
                label = stringResource(R.string.topics_menu_edit),
                icon = Icons.Outlined.Edit,
                color = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            // The error role, not the booking-status destructive token (Phase 8 rule A5).
            MenuItem(
                label = stringResource(R.string.topics_menu_delete),
                icon = Icons.Outlined.Delete,
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color) },
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = TrailingSize),
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(GlyphSize), tint = color)
        },
    )
}

@Preview(name = "TopicCard")
@Composable
private fun TopicCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                TopicCard(
                    item =
                        ExpertTopicItem(
                            id = "t1",
                            title = "Go-to-market execution for early stage startups",
                            durationMins = 90,
                            iconSlug = "startup",
                            price = TopicPriceDto(amount = 15000, currency = "CAD", isFree = false),
                        ),
                    isDeleting = false,
                    menuEnabled = true,
                    onClick = {},
                    onEdit = {},
                    onDelete = {},
                )
                TopicCard(
                    item =
                        ExpertTopicItem(
                            id = "t2",
                            title = "Intro chat",
                            durationMins = 30,
                            price = TopicPriceDto(amount = 0, currency = "", isFree = true),
                        ),
                    isDeleting = true,
                    menuEnabled = false,
                    onClick = {},
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
    }
}
