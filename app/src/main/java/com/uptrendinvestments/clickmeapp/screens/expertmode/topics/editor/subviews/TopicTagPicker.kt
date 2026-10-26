package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SelectableChip
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.util.Locale

private val AddIconSize = 16.dp
private val SpinnerSize = 16.dp
private val SpinnerStroke = 2.dp
private val SearchIconSize = 20.dp
private val ClearIconSize = 20.dp
private val MinTouchTarget = 48.dp

/** Test tag of the header "Add" button that opens the Edit Expertise sheet. */
const val TOPIC_ADD_EXPERTISE_TAG = "topic_editor_add_expertise"

/** Test tag of the tag filter field. */
const val TOPIC_TAG_FILTER_TAG = "topic_editor_tag_filter"

/**
 * Expertise tag section of the topic editor. Only the expert's own profile tags are offered (the caller passes them
 * as [visibleTags], already ordered selected-first and filtered by `TopicEditorRules.visibleTags`). Once three tags
 * are chosen ([atCap]) unselected chips are disabled and announce "Limit reached"; selected chips stay tappable so
 * they can be removed. States, in priority order: [isLoading], [loadFailed], [hasProfileTags] false (offers
 * "Add Expertise"), then the filter plus chips. In the [loadFailed] state the topic's selected tags stay visible as
 * removable chips above the retry row, because Save still sends them (WR-05). Stateless.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicTagPicker(
    isLoading: Boolean,
    loadFailed: Boolean,
    hasProfileTags: Boolean,
    visibleTags: List<ExpertiseTagItem>,
    selectedIds: Set<String>,
    atCap: Boolean,
    filter: String,
    onFilterChange: (String) -> Unit,
    onToggle: (ExpertiseTagItem) -> Unit,
    onAddExpertise: () -> Unit,
    onRetry: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TagHeader(onAddExpertise = onAddExpertise, enabled = enabled)
        when {
            isLoading -> LoadingRow()
            loadFailed -> {
                val selectedIdsLower = selectedIds.map { it.lowercase(Locale.ROOT) }.toSet()
                val selectedTags = visibleTags.filter { it.id.lowercase(Locale.ROOT) in selectedIdsLower }
                if (selectedTags.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        selectedTags.forEach { tag ->
                            SelectableChip(
                                label = tag.label,
                                selected = true,
                                onClick = { onToggle(tag) },
                                enabled = enabled,
                                removeContentDescription = stringResource(R.string.topic_editor_tags_remove, tag.label),
                            )
                        }
                    }
                    Spacer(Modifier.size(Spacing.sm))
                }
                FailedRow(onRetry = onRetry)
            }
            !hasProfileTags -> NoTagsBody(onAddExpertise = onAddExpertise, enabled = enabled)
            else -> {
                TagFilterField(filter = filter, onFilterChange = onFilterChange, enabled = enabled)
                val limitReached = stringResource(R.string.topic_editor_tags_limit_reached)
                val selectedIdsLower = selectedIds.map { it.lowercase() }.toSet()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    visibleTags.forEach { tag ->
                        val selected = tag.id.lowercase() in selectedIdsLower
                        val capped = atCap && !selected
                        SelectableChip(
                            label = tag.label,
                            selected = selected,
                            onClick = { onToggle(tag) },
                            enabled = enabled && !capped,
                            modifier =
                                if (capped) {
                                    Modifier.semantics { stateDescription = limitReached }
                                } else {
                                    Modifier
                                },
                            removeContentDescription =
                                if (selected) stringResource(R.string.topic_editor_tags_remove, tag.label) else null,
                        )
                    }
                }
                if (visibleTags.isEmpty() && filter.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.topic_editor_tags_no_match, filter.trim()),
                        modifier = Modifier.padding(top = Spacing.sm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagHeader(
    onAddExpertise: () -> Unit,
    enabled: Boolean,
) {
    val addDescription = stringResource(R.string.topic_editor_tags_add_a11y)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.topic_editor_tags_label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.topic_editor_tags_limit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(Spacing.sm))
        TextButton(
            onClick = onAddExpertise,
            modifier =
                Modifier
                    .heightIn(min = MinTouchTarget)
                    .testTag(TOPIC_ADD_EXPERTISE_TAG)
                    .semantics { contentDescription = addDescription },
            enabled = enabled,
            contentPadding = ButtonDefaults.TextButtonContentPadding,
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(AddIconSize))
            Spacer(Modifier.size(Spacing.xs))
            Text(
                text = stringResource(R.string.topic_editor_tags_add),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(SpinnerSize),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = SpinnerStroke,
        )
        Text(
            text = stringResource(R.string.topic_editor_tags_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Composable
private fun FailedRow(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.topic_editor_tags_load_error),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.destructive,
        )
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(
                text = stringResource(R.string.expert_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun NoTagsBody(
    onAddExpertise: () -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier.padding(top = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.topic_editor_tags_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        PressScaleButton(
            onClick = onAddExpertise,
            enabled = enabled,
            shape = CircleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Transparent,
                ),
            modifier =
                Modifier
                    .heightIn(min = MinTouchTarget)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(AddIconSize))
            Spacer(Modifier.size(Spacing.xs))
            Text(text = stringResource(R.string.topic_editor_tags_add_expertise))
        }
    }
}

@Composable
private fun TagFilterField(
    filter: String,
    onFilterChange: (String) -> Unit,
    enabled: Boolean,
) {
    ValidatedField(
        value = filter,
        onValueChange = onFilterChange,
        placeholder = stringResource(R.string.topic_editor_tags_filter),
        leadingIcon = Icons.Outlined.Search,
        error = null,
        modifier = Modifier.padding(top = Spacing.sm).testTag(TOPIC_TAG_FILTER_TAG),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        enabled = enabled,
        trailingBadge =
            if (filter.isNotEmpty()) {
                {
                    IconButton(onClick = { onFilterChange("") }, modifier = Modifier.size(MinTouchTarget)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.topic_editor_tags_clear_filter),
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

@Preview(name = "TopicTagPicker")
@Composable
private fun TopicTagPickerPreview() {
    val law = ExpertiseTagItem(id = "law", label = "Law")
    val audit = ExpertiseTagItem(id = "audit", label = "Audit")
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicTagPicker(
                isLoading = false,
                loadFailed = false,
                hasProfileTags = true,
                visibleTags = listOf(law, audit),
                selectedIds = setOf("law"),
                atCap = false,
                filter = "",
                onFilterChange = {},
                onToggle = {},
                onAddExpertise = {},
                onRetry = {},
                enabled = true,
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
