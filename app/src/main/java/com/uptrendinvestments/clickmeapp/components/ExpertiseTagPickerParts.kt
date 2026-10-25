package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val ClearIconSize = 24.dp

/**
 * Search pill for the expertise tag pickers (signup tags step and the Edit Expertise sheet): leading search icon and
 * a clear button once [query] is non-empty.
 */
@Composable
fun ExpertiseTagSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    ValidatedField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        leadingIcon = Icons.Outlined.Search,
        error = null,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        trailingBadge =
            if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.picker_clear_search),
                            modifier = Modifier.size(ClearIconSize),
                            tint = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
            } else {
                null
            },
    )
}

/**
 * Header, removable chips in tap order and an optional [hint] line for the selected expertise tags. The defaults
 * keep the signup look; the Edit Expertise sheet passes a smaller, muted header.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedTagChips(
    header: String,
    selected: List<ExpertiseTagItem>,
    onRemove: (String) -> Unit,
    hint: String?,
    modifier: Modifier = Modifier,
    headerStyle: TextStyle = MaterialTheme.typography.titleMedium,
    headerColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = header,
            style = headerStyle,
            color = headerColor,
        )
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            selected.forEach { tag ->
                SelectableChip(
                    label = tag.label,
                    selected = true,
                    onClick = { onRemove(tag.id) },
                    removeContentDescription = stringResource(R.string.signup_tags_remove, tag.label),
                )
            }
        }
        if (hint != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

/** "No tags match “query”" block shown when a search leaves nothing to pick. */
@Composable
fun ExpertiseTagNoMatch(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.signup_tags_no_match_title, query),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.signup_tags_no_match_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
}
