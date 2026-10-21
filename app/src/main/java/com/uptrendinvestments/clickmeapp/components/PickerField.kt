package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val FieldMinHeight = 56.dp
private val RestBorder = 1.dp
private val ErrorBorder = 2.dp
private val IconSize = 24.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * Read-only field that opens a picker (country, language, timezone). Looks like [ValidatedField] (pill, 56 dp,
 * outline border, surfaceContainer fill, error row underneath) but acts as a button: the whole row is clickable and
 * TalkBack reads "[placeholder], [value]" when a value is set, or just the placeholder when it is not.
 */
@Composable
fun PickerField(
    value: String?,
    placeholder: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
) {
    val extras = MaterialTheme.extras
    val colorScheme = MaterialTheme.colorScheme
    val hasError = error != null
    val borderColor = if (hasError) extras.destructive else colorScheme.outline
    val borderWidth = if (hasError) ErrorBorder else RestBorder

    Column(modifier = modifier.padding(bottom = Spacing.md)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = FieldMinHeight)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainer)
                    .border(borderWidth, borderColor, CircleShape)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    .semantics(mergeDescendants = true) {
                        if (value != null) contentDescription = "$placeholder, $value"
                        if (error != null) error(error)
                    }.padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = if (hasError) extras.destructive else extras.onSurfaceMuted,
            )
            Text(
                text = value ?: placeholder,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) colorScheme.onSurface else extras.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = extras.onSurfaceMuted,
            )
        }
        if (error != null) {
            Text(
                text = error,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs),
                style = MaterialTheme.typography.labelMedium,
                color = extras.destructive,
            )
        }
    }
}

@Preview(name = "PickerField")
@Composable
private fun PickerFieldPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                PickerField(
                    value = null,
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = {},
                )
                PickerField(
                    value = "Canada",
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = {},
                )
                PickerField(
                    value = null,
                    placeholder = "Country",
                    leadingIcon = Icons.Outlined.Language,
                    onClick = {},
                    error = "Required",
                )
            }
        }
    }
}
