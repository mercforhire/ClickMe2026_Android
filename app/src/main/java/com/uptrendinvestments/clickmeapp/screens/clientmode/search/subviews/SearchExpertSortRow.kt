package com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchSort
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/** Test tag on the "Sort by" trigger button. */
const val SEARCH_SORT_BUTTON_TAG = "search_sort_button"

private val TriggerMinHeight = 48.dp
private val TriggerSpacing = 4.dp

/** The user-facing name of a sort option (iOS `sortOptions`). */
@Composable
internal fun SearchSort.label(): String =
    when (this) {
        SearchSort.MostPopular -> stringResource(R.string.search_sort_most_popular)
        SearchSort.HighestRated -> stringResource(R.string.search_sort_highest_rated)
    }

/**
 * Port of iOS `SearchExpertSortRow` plus its "Sort by" confirmation dialog: a bold "Sort by" label on the left, the
 * current option and a chevron on the right. Tapping the right side opens an anchored menu with one item per
 * [SearchSort] in the iOS order and the current one marked selected; picking one reports it and closes the menu.
 * The anchored menu replaces the iOS dialog (Claude's discretion, recorded in the 16.1-05 plan).
 */
@Composable
fun SearchExpertSortRow(
    sort: SearchSort,
    onSortSelected: (SearchSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val currentLabel = sort.label()
    val buttonDescription = stringResource(R.string.search_sort_button_semantics, currentLabel)

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.search_sort_by),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box {
            Row(
                modifier =
                    Modifier
                        .heightIn(min = TriggerMinHeight)
                        .clickable(role = Role.Button) { expanded = true }
                        .semantics(mergeDescendants = true) { contentDescription = buttonDescription }
                        .testTag(SEARCH_SORT_BUTTON_TAG),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TriggerSpacing),
            ) {
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SearchSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label()) },
                        onClick = {
                            expanded = false
                            onSortSelected(option)
                        },
                        modifier = Modifier.semantics { selected = option == sort },
                        trailingIcon =
                            if (option == sort) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}

@Preview(name = "SearchExpertSortRow")
@Composable
private fun SearchExpertSortRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SearchExpertSortRow(sort = SearchSort.MostPopular, onSortSelected = {})
        }
    }
}
