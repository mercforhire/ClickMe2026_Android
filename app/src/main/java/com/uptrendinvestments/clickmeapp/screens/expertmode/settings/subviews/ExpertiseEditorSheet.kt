package com.uptrendinvestments.clickmeapp.screens.expertmode.settings.subviews

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.ExpertiseTagNoMatch
import com.uptrendinvestments.clickmeapp.components.ExpertiseTagSearchField
import com.uptrendinvestments.clickmeapp.components.PickerLoading
import com.uptrendinvestments.clickmeapp.components.PickerRow
import com.uptrendinvestments.clickmeapp.components.SelectedTagChips
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.util.Locale

/** Test tag of the Save Expertise button. */
internal const val EXPERTISE_SHEET_SAVE_TAG = "expertise_sheet_save"

/** Test tag of the Cancel button. */
internal const val EXPERTISE_SHEET_CANCEL_TAG = "expertise_sheet_cancel"

private val TitleMinHeight = 56.dp
private val ActionMinHeight = 56.dp
private val ActionBorderWidth = 1.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp

/**
 * Edit Expertise bottom sheet (D-08, iOS `ExpertiseEditorSheet`). Opens fully expanded and cannot be dismissed
 * (swipe, scrim or Back) while a save is in flight. A successful save hands the saved tags to [onSaved] once; the
 * caller closes the sheet. The caller scopes a fresh [viewModel] per opening so each open re-seeds from the server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertiseEditorSheet(
    onDismiss: () -> Unit,
    onSaved: (List<ExpertiseTagItem>) -> Unit,
    viewModel: ExpertiseEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by rememberUpdatedState(state.isSaving)

    LaunchedEffect(state.saved) {
        state.saved?.let { saved ->
            onSaved(saved)
            viewModel.onSavedConsumed()
        }
    }
    BackHandler(enabled = state.isSaving) {}

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { !isSaving || it != SheetValue.Hidden },
            ),
        shape =
            MaterialTheme.shapes.extraLarge.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ExpertiseEditorContent(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onToggle = viewModel::toggle,
            onRemove = viewModel::remove,
            onRetry = viewModel::retry,
            onCancel = onDismiss,
            onSave = viewModel::save,
            onDismissDialog = viewModel::dismissDialog,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

/**
 * Stateless sheet body: title, scrolling search / chips / catalog list and the pinned Cancel / Save Expertise row.
 */
@Composable
fun ExpertiseEditorContent(
    state: ExpertiseEditorUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (ExpertiseTagItem) -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = TitleMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.expertise_editor_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        TagList(
            state = state,
            onQueryChange = onQueryChange,
            onToggle = onToggle,
            onRemove = onRemove,
            onRetry = onRetry,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        ActionRow(state = state, onCancel = onCancel, onSave = onSave)
    }

    state.dialog?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.expertise_editor_save_error_title),
        )
    }
}

@Composable
private fun TagList(
    state: ExpertiseEditorUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (ExpertiseTagItem) -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIds = state.selectedIds
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.md),
    ) {
        val seed = state.seed
        if (seed is ExpertiseSeedState.Failed) {
            item(key = "seed_error") {
                EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    heading = stringResource(R.string.expertise_editor_seed_error),
                    body = seed.error.asString(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
                    action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
                )
            }
            return@LazyColumn
        }
        if (seed is ExpertiseSeedState.Loading) {
            item(key = "seed_loading") {
                PickerLoading(text = stringResource(R.string.expertise_editor_seed_loading))
            }
            return@LazyColumn
        }
        item(key = "search") {
            ExpertiseTagSearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.signup_tags_search),
            )
        }
        if (state.selected.isEmpty()) {
            item(key = "pick_one") {
                Text(
                    text = stringResource(R.string.expertise_editor_pick_one),
                    modifier = Modifier.padding(top = Spacing.md),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        } else {
            item(key = "selected") {
                SelectedTagChips(
                    header = stringResource(R.string.expertise_editor_your_tags),
                    selected = state.selected,
                    onRemove = onRemove,
                    hint = stringResource(R.string.signup_tags_primary_hint),
                    modifier = Modifier.padding(top = Spacing.md),
                    headerStyle = MaterialTheme.typography.labelMedium,
                    headerColor = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
        item(key = "all_header") {
            Text(
                text = stringResource(R.string.expertise_editor_all),
                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.sm),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
        when (val catalog = state.catalog) {
            ExpertiseCatalogState.Loading ->
                item(key = "loading") {
                    PickerLoading(text = stringResource(R.string.signup_tags_loading))
                }
            is ExpertiseCatalogState.Failed ->
                item(key = "error") {
                    EmptyState(
                        icon = Icons.Outlined.CloudOff,
                        heading = stringResource(R.string.signup_tags_error),
                        body = catalog.error.asString(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
                        action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
                    )
                }
            is ExpertiseCatalogState.Loaded -> {
                val rows = state.rows
                if (rows.isEmpty() && state.query.isNotBlank()) {
                    item(key = "no_match") {
                        ExpertiseTagNoMatch(query = state.query.trim())
                    }
                } else {
                    items(rows, key = { it.id }) { tag ->
                        PickerRow(
                            label = tag.label,
                            selected = tag.id.lowercase(Locale.ROOT) in selectedIds,
                            onClick = { onToggle(tag) },
                            role = Role.Checkbox,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    state: ExpertiseEditorUiState,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.md)
                .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PressScaleButton(
            onClick = onCancel,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = ActionMinHeight)
                    .border(ActionBorderWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .testTag(EXPERTISE_SHEET_CANCEL_TAG),
            enabled = !state.isSaving,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = Color.Transparent,
                ),
        ) {
            Text(text = stringResource(R.string.expertise_editor_cancel))
        }
        PressScaleButton(
            onClick = onSave,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = ActionMinHeight)
                    .testTag(EXPERTISE_SHEET_SAVE_TAG),
            enabled = state.canSave,
            colors =
                if (state.isSaving) {
                    // Disabled while saving, but keep the filled look behind the spinner.
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    PressScaleButtonDefaults.filledColors()
                },
        ) {
            if (state.isSaving) {
                val savingLabel = stringResource(R.string.expertise_editor_saving)
                CircularProgressIndicator(
                    modifier = Modifier.size(SpinnerSize).semantics { stateDescription = savingLabel },
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = SpinnerStroke,
                )
            } else {
                Text(text = stringResource(R.string.expertise_editor_save))
            }
        }
    }
}

@Preview(name = "ExpertiseEditorContent")
@Composable
private fun ExpertiseEditorContentPreview() {
    val tax = ExpertiseTagItem("t1", "Tax")
    val law = ExpertiseTagItem("t2", "Law")
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ExpertiseEditorContent(
                state =
                    ExpertiseEditorUiState(
                        selected = listOf(tax),
                        seed = ExpertiseSeedState.Ready,
                        catalog = ExpertiseCatalogState.Loaded(listOf(law, tax)),
                    ),
                onQueryChange = {},
                onToggle = {},
                onRemove = {},
                onRetry = {},
                onCancel = {},
                onSave = {},
                onDismissDialog = {},
            )
        }
    }
}
