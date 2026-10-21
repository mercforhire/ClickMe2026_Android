package com.uptrendinvestments.clickmeapp.screens.signup.tags

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.PickerLoading
import com.uptrendinvestments.clickmeapp.components.SelectableChip
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val RowMinHeight = 56.dp
private val ClearIconSize = 24.dp

/**
 * Hilt-aware Expertise Tags step (iOS `SignupTagsView`, catalog tags only). [onSaved] is called once after the
 * 600 ms "Saved!" hold.
 */
@Composable
fun SignupTagsScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupTagsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) {
            onSaved()
            viewModel.onSavedConsumed()
        }
    }

    SignupTagsContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSelect = viewModel::select,
        onRemove = viewModel::remove,
        onRetry = viewModel::retry,
        onSave = viewModel::save,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
    )
}

/** Stateless Expertise Tags body: search, selected chips in tap order, suggested rows and the pinned Save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignupTagsContent(
    state: SignupTagsUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (ExpertiseTagItem) -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = state.saveState == SaveState.Idle
    // Leaving mid-save would cancel the request and drop the confirmation; swallow Back until it finishes.
    BackHandler(enabled = !editable) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.signup_tags_title),
                onBack = onBack,
                backEnabled = editable,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg),
            ) {
                item(key = "search") {
                    SearchField(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        modifier = Modifier.padding(top = Spacing.lg),
                    )
                }
                if (state.selected.isEmpty()) {
                    item(key = "empty_hint") {
                        Text(
                            text = stringResource(R.string.signup_tags_empty_hint),
                            modifier = Modifier.padding(top = Spacing.md),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                } else {
                    item(key = "selected") {
                        Column(modifier = Modifier.padding(top = Spacing.lg)) {
                            Text(
                                text = stringResource(R.string.signup_tags_selected),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                state.selected.forEach { tag ->
                                    SelectableChip(
                                        label = tag.label,
                                        selected = true,
                                        onClick = { onRemove(tag.id) },
                                        removeContentDescription =
                                            stringResource(R.string.signup_tags_remove, tag.label),
                                    )
                                }
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = stringResource(R.string.signup_tags_primary_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extras.onSurfaceMuted,
                            )
                        }
                    }
                }
                catalogItems(state, onSelect, onRetry)
            }
            AsyncSaveButton(
                title = stringResource(R.string.signup_save),
                savedTitle = stringResource(R.string.signup_saved),
                state = state.saveState,
                onClick = onSave,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.lg),
            )
        }
    }

    state.dialogError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.signup_error_save_title),
        )
    }
}

private fun LazyListScope.catalogItems(
    state: SignupTagsUiState,
    onSelect: (ExpertiseTagItem) -> Unit,
    onRetry: () -> Unit,
) {
    when (state.catalog) {
        TagCatalog.Loading ->
            item(key = "loading") {
                PickerLoading(text = stringResource(R.string.signup_tags_loading))
            }
        TagCatalog.Failed ->
            item(key = "error") {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    heading = stringResource(R.string.signup_tags_error),
                    body = stringResource(R.string.picker_load_error_body),
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
                    action = EmptyStateAction(stringResource(R.string.picker_retry), onRetry),
                )
            }
        is TagCatalog.Loaded ->
            if (state.noMatch) {
                item(key = "no_match") {
                    NoMatch(query = state.query.trim())
                }
            } else {
                item(key = "suggested_header") {
                    Text(
                        text = stringResource(R.string.signup_tags_suggested),
                        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                items(state.suggested, key = { it.id }) { tag ->
                    TagRow(label = tag.label, onClick = { onSelect(tag) })
                }
            }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ValidatedField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.signup_tags_search),
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

@Composable
private fun TagRow(
    label: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = RowMinHeight)
                    .clickable(role = Role.Button, onClick = onClick),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NoMatch(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
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

@Preview(name = "SignupTags")
@Composable
private fun SignupTagsPreview() {
    val tax = ExpertiseTagItem("t1", "Tax")
    val law = ExpertiseTagItem("t2", "Law")
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SignupTagsContent(
                state =
                    SignupTagsUiState(
                        selected = listOf(tax),
                        catalog = TagCatalog.Loaded(listOf(tax, law)),
                    ),
                onQueryChange = {},
                onSelect = {},
                onRemove = {},
                onRetry = {},
                onSave = {},
                onBack = {},
                onDismissDialog = {},
            )
        }
    }
}
