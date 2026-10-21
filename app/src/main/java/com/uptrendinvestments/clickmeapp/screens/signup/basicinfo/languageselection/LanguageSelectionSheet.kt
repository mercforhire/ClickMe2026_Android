package com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.languageselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.PickerLoading
import com.uptrendinvestments.clickmeapp.components.PickerRow
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageCatalog
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageSheetState
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * Multi-select Spoken Languages sheet (iOS Language Selection). Rows toggle on tap; the order the user picks them in
 * is kept by the ViewModel (D-14). Cancel discards the working selection, Done applies it. There is no
 * primary-language helper line (D-24).
 */
@Composable
fun LanguageSelectionSheet(
    state: LanguageSheetState,
    onQueryChange: (String) -> Unit,
    onToggle: (LanguageItem) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    PickerSheet(
        title = stringResource(R.string.signup_basic_info_language_sheet_title),
        onDismiss = onCancel,
        searchQuery = state.query,
        searchPlaceholder = stringResource(R.string.signup_basic_info_language_search),
        onSearchQueryChange = onQueryChange,
        footer = { LanguageSheetFooter(onCancel = onCancel, onDone = onDone) },
        content = languageRows(state, onToggle, onRetry),
    )
}

/** Stateless body of [LanguageSelectionSheet], testable without a window. */
@Composable
fun LanguageSelectionSheetContent(
    state: LanguageSheetState,
    onQueryChange: (String) -> Unit,
    onToggle: (LanguageItem) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PickerSheetContent(
        title = stringResource(R.string.signup_basic_info_language_sheet_title),
        modifier = modifier,
        searchQuery = state.query,
        searchPlaceholder = stringResource(R.string.signup_basic_info_language_search),
        onSearchQueryChange = onQueryChange,
        footer = { LanguageSheetFooter(onCancel = onCancel, onDone = onDone) },
        content = languageRows(state, onToggle, onRetry),
    )
}

@Composable
private fun LanguageSheetFooter(
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PressScaleButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            colors = PressScaleButtonDefaults.tonalColors(),
        ) { Text(stringResource(R.string.picker_cancel)) }
        PressScaleButton(onClick = onDone, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.picker_done))
        }
    }
}

@Composable
private fun languageRows(
    state: LanguageSheetState,
    onToggle: (LanguageItem) -> Unit,
    onRetry: () -> Unit,
): LazyListScope.() -> Unit {
    val loadingText = stringResource(R.string.signup_basic_info_languages_loading)
    val errorHeading = stringResource(R.string.signup_basic_info_languages_error)
    val errorBody = stringResource(R.string.picker_load_error_body)
    val retryLabel = stringResource(R.string.picker_retry)
    return {
        when (state.catalog) {
            LanguageCatalog.Loading -> item { PickerLoading(text = loadingText) }
            LanguageCatalog.Failed ->
                item {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = errorHeading,
                        body = errorBody,
                        action = EmptyStateAction(label = retryLabel, onClick = onRetry),
                    )
                }
            is LanguageCatalog.Loaded ->
                items(state.visibleItems, key = { it.id }) { item ->
                    PickerRow(
                        label = item.label,
                        selected = state.selection.any { it.id == item.id },
                        onClick = { onToggle(item) },
                    )
                }
        }
    }
}
