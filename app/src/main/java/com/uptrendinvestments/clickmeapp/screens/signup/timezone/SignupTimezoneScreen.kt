package com.uptrendinvestments.clickmeapp.screens.signup.timezone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerField
import com.uptrendinvestments.clickmeapp.components.PickerRow
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val AutoDetectMinHeight = 48.dp
private val AutoDetectIconSize = 20.dp
private val TimezoneRowMinHeight = 56.dp

/** Hilt-aware entry for the Timezone step. [onSaved] fires after the 600 ms "Saved!" hold; [onBack] pops. */
@Composable
fun SignupTimezoneScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupTimezoneViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) {
            onSaved()
            viewModel.onSavedConsumed()
        }
    }

    SignupTimezoneContent(
        state = state,
        onOpenSheet = viewModel::openSheet,
        onDismissSheet = viewModel::dismissSheet,
        onSelect = viewModel::select,
        onAutoDetect = viewModel::autoDetect,
        onSave = viewModel::save,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
    )
}

/** Stateless Timezone step (04-UI-SPEC screen 4). */
@Composable
fun SignupTimezoneContent(
    state: SignupTimezoneUiState,
    onOpenSheet: () -> Unit,
    onDismissSheet: () -> Unit,
    onSelect: (String) -> Unit,
    onAutoDetect: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AuthTopAppBar(title = stringResource(R.string.signup_timezone_title), onBack = onBack) },
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                AsyncSaveButton(
                    title = stringResource(R.string.signup_save),
                    state = state.saveState,
                    onClick = onSave,
                    savedTitle = stringResource(R.string.signup_saved),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.signup_timezone_heading),
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.signup_timezone_body),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            PickerField(
                value = state.selectedLabel,
                placeholder = stringResource(R.string.signup_timezone_field),
                leadingIcon = Icons.Outlined.Schedule,
                onClick = onOpenSheet,
                modifier = Modifier.padding(top = Spacing.lg),
                error = state.timezoneError?.asString(),
            )
            TextButton(
                onClick = onAutoDetect,
                modifier = Modifier.heightIn(min = AutoDetectMinHeight),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(AutoDetectIconSize),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.signup_timezone_auto_detect),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
            Text(
                text = stringResource(R.string.signup_timezone_explainer),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }

    if (state.showSheet) {
        PickerSheet(
            title = stringResource(R.string.signup_timezone_sheet_title),
            onDismiss = onDismissSheet,
            searchQuery = null,
        ) {
            timezoneSheetRows(state.entries, state.selectedId, onSelect)
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

/** The single-select timezone rows (56 dp, accent check on the selected one). No search field for 10 to 11 rows. */
fun LazyListScope.timezoneSheetRows(
    entries: List<TimezoneEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    items(entries, key = { it.id }) { entry ->
        PickerRow(
            label = entry.label,
            selected = entry.id == selectedId,
            onClick = { onSelect(entry.id) },
            minHeight = 56.dp,
        )
    }
}

/** Sheet body without the [PickerSheet] window, so it can be composed and tested directly. */
@Composable
fun TimezoneSheetContent(
    entries: List<TimezoneEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PickerSheetContent(
        title = stringResource(R.string.signup_timezone_sheet_title),
        modifier = modifier,
        searchQuery = null,
    ) {
        timezoneSheetRows(entries, selectedId, onSelect)
    }
}

@Preview(name = "SignupTimezoneContent")
@Composable
private fun SignupTimezoneContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SignupTimezoneContent(
                state =
                    SignupTimezoneUiState(
                        selectedId = "America/New_York",
                        entries = Timezones.all,
                    ),
                onOpenSheet = {},
                onDismissSheet = {},
                onSelect = {},
                onAutoDetect = {},
                onSave = {},
                onBack = {},
                onDismissDialog = {},
            )
        }
    }
}
