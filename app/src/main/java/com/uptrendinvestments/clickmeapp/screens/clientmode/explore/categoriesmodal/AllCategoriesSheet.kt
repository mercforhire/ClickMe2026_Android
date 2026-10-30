package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.categoriesmodal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerLoading
import com.uptrendinvestments.clickmeapp.components.PickerRow
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.constants.CategoryIconMap
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val CategoryRowMinHeight = 56.dp
private val StateIconSize = 48.dp
private val StateBodyMaxWidth = 320.dp

/**
 * All Categories bottom sheet (DISC-02, D-06, D-10, D-11): searchable multi-select over the cached category catalog.
 * Apply hands the draft back through [onApply]; every other dismissal (drag, scrim, Back) calls only [onDismiss], so
 * the draft is discarded. The caller closes the sheet and applies the selection.
 */
@Composable
fun AllCategoriesSheet(
    viewModel: CategoriesSheetViewModel,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.ensureLoaded() }
    PickerSheet(
        title = stringResource(R.string.all_categories_title),
        onDismiss = onDismiss,
        searchQuery = state.query,
        searchPlaceholder = stringResource(R.string.all_categories_search),
        onSearchQueryChange = viewModel::onQueryChange,
        footer = { AllCategoriesFooter(onClear = viewModel::clear, onApply = { onApply(state.draft) }) },
        content = allCategoriesItems(state, viewModel::toggle, viewModel::retry),
    )
}

/** Stateless body of [AllCategoriesSheet], testable without a window (ModalBottomSheet needs one). */
@Composable
fun AllCategoriesSheetContent(
    state: CategoriesSheetUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PickerSheetContent(
        title = stringResource(R.string.all_categories_title),
        modifier = modifier,
        searchQuery = state.query,
        searchPlaceholder = stringResource(R.string.all_categories_search),
        onSearchQueryChange = onQueryChange,
        footer = { AllCategoriesFooter(onClear = onClear, onApply = onApply) },
        content = allCategoriesItems(state, onToggle, onRetry),
    )
}

/** Clear (tonal, empties the draft only) and Apply (the sheet's one filled accent control, always enabled). */
@Composable
private fun AllCategoriesFooter(
    onClear: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PressScaleButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            colors = PressScaleButtonDefaults.tonalColors(),
        ) { Text(stringResource(R.string.all_categories_clear)) }
        PressScaleButton(onClick = onApply, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.all_categories_apply))
        }
    }
}

@Composable
private fun allCategoriesItems(
    state: CategoriesSheetUiState,
    onToggle: (String) -> Unit,
    onRetry: () -> Unit,
): LazyListScope.() -> Unit {
    val loadingText = stringResource(R.string.all_categories_loading)
    val noMatchHeading = stringResource(R.string.all_categories_no_match_heading, state.query.trim())
    return {
        when (state.catalog) {
            CategoryCatalog.Loading -> item { PickerLoading(text = loadingText) }
            CategoryCatalog.Failed -> item { CategoriesError(onRetry = onRetry) }
            is CategoryCatalog.Loaded ->
                if (state.catalog.items.isEmpty()) {
                    item { CategoriesEmpty() }
                } else if (state.noMatch) {
                    item { CategoriesNoMatch(heading = noMatchHeading) }
                } else {
                    items(state.visible, key = { it.id }) { category ->
                        PickerRow(
                            label = category.name,
                            selected = category.id in state.draft,
                            onClick = { onToggle(category.id) },
                            minHeight = CategoryRowMinHeight,
                            leadingIcon = CategoryIconMap.icon(category.id),
                            role = Role.Checkbox,
                        )
                    }
                }
        }
    }
}

@Composable
private fun CategoriesError(onRetry: () -> Unit) {
    StateColumn(
        icon = Icons.Outlined.ErrorOutline,
        heading = stringResource(R.string.all_categories_error_heading),
        body = stringResource(R.string.all_categories_error_body),
    ) {
        Spacer(Modifier.height(Spacing.lg))
        PressScaleButton(onClick = onRetry, colors = PressScaleButtonDefaults.tonalColors()) {
            Text(stringResource(R.string.section_retry))
        }
    }
}

/** An empty catalog: nothing to retry or change, so no action (UI-SPEC archetype E). */
@Composable
private fun CategoriesEmpty() {
    StateColumn(
        icon = Icons.Outlined.SearchOff,
        heading = stringResource(R.string.all_categories_empty_heading),
        body = stringResource(R.string.all_categories_empty_body),
    )
}

@Composable
private fun CategoriesNoMatch(heading: String) {
    StateColumn(
        icon = Icons.Outlined.SearchOff,
        heading = heading,
        body = stringResource(R.string.all_categories_no_match_body),
    )
}

@Composable
private fun StateColumn(
    icon: ImageVector,
    heading: String,
    body: String,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(StateIconSize),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = heading,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = body,
            modifier = Modifier.widthIn(max = StateBodyMaxWidth),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        action()
    }
}
