package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.constants.CategoryIconMap
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorRules
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val ChipSize = 48.dp
private val GlyphSize = 24.dp
private val RestBorder = 1.dp
private val SelectedBorder = 2.dp
private const val SELECTED_FILL_ALPHA = 0.12f

/** Test tag of the icon chip for [slug]. */
fun topicIconTag(slug: String): String = "topic_icon_$slug"

/**
 * Horizontally scrolling strip of the curated topic icons (iOS topic editor icon picker). Tapping the selected icon
 * calls [onToggle] with the same slug and the ViewModel clears it. The selected chip is scrolled to the center once,
 * without animation, when the strip first appears. Stateless.
 */
@Composable
fun TopicIconPicker(
    options: List<String>,
    selected: String?,
    onToggle: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.topic_editor_icon_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val centerOffsetPx = with(LocalDensity.current) { (maxWidth / 2 - ChipSize / 2).roundToPx() }
            LaunchedEffect(Unit) {
                val index = options.indexOf(selected)
                if (index >= 0) listState.scrollToItem(index, -centerOffsetPx)
            }
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(options, key = { it }) { slug ->
                    IconChip(
                        slug = slug,
                        isSelected = slug == selected,
                        enabled = enabled,
                        onClick = { onToggle(slug) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IconChip(
    slug: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val description = stringResource(R.string.topic_editor_icon_a11y, TopicEditorRules.iconLabel(slug))
    val state =
        stringResource(
            if (isSelected) R.string.topic_editor_icon_selected else R.string.topic_editor_icon_not_selected,
        )
    val tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
    Surface(
        modifier =
            Modifier
                .size(ChipSize)
                .testTag(topicIconTag(slug))
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    stateDescription = state
                },
        shape = MaterialTheme.shapes.small,
        color =
            if (isSelected) {
                colorScheme.primary.copy(alpha = SELECTED_FILL_ALPHA)
            } else {
                colorScheme.surfaceContainerLow
            },
        border =
            if (isSelected) {
                BorderStroke(SelectedBorder, colorScheme.primary)
            } else {
                BorderStroke(RestBorder, colorScheme.outlineVariant)
            },
    ) {
        Box(
            modifier = Modifier.size(ChipSize).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CategoryIconMap.icon(slug),
                contentDescription = null,
                modifier = Modifier.size(GlyphSize),
                tint = tint,
            )
        }
    }
}

@Preview(name = "TopicIconPicker")
@Composable
private fun TopicIconPickerPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicIconPicker(
                options = TopicEditorRules.CURATED_ICON_SLUGS,
                selected = "tech",
                onToggle = {},
                enabled = true,
            )
        }
    }
}
