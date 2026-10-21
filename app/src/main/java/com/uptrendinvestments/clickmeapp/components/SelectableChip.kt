package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val CloseIconSize = 16.dp

/**
 * Removable selection chip (selected languages, expertise tags). Selected chips show a trailing close icon and use
 * the secondaryContainer fill; unselected chips use surfaceContainerHighest. [removeContentDescription] (for example
 * "Remove English") becomes the chip's TalkBack description when set. Callers lay chips out in a `FlowRow` with
 * `Spacing.sm` gaps.
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    removeContentDescription: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    if (removeContentDescription != null) contentDescription = removeContentDescription
                },
        shape = MaterialTheme.shapes.small,
        color = if (selected) colorScheme.secondaryContainer else colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(CloseIconSize),
                    tint = colorScheme.onSurface,
                )
            }
        }
    }
}

@Preview(name = "SelectableChip")
@Composable
private fun SelectableChipPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SelectableChip(
                    label = "English",
                    selected = true,
                    onClick = {},
                    removeContentDescription = "Remove English",
                )
                SelectableChip(label = "French", selected = false, onClick = {})
            }
        }
    }
}
