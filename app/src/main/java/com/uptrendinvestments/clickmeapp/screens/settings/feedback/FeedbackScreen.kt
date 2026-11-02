package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerField
import com.uptrendinvestments.clickmeapp.components.PickerRow
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/** Test tag on the Submit Feedback button. */
const val FEEDBACK_SUBMIT_TAG = "feedback_submit"

/** Test tag on the Feedback Type picker field. */
const val FEEDBACK_TYPE_FIELD_TAG = "feedback_type_field"

/**
 * Port of iOS `FeedbackView`: pick a server-driven category, describe the experience, optionally leave an email and
 * send. A successful send opens the thanks dialog, and dismissing it (OK, scrim or back) calls [onBack] (D-06).
 */
@Composable
fun FeedbackScreen(
    viewModel: FeedbackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    FeedbackContent(
        state = state,
        onSelectType = viewModel::onSelectType,
        onDetailsChange = viewModel::onDetailsChange,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::submit,
        onRetry = viewModel::retry,
        onDismissDialog = viewModel::dismissDialog,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Stateless Send Feedback screen under a "Send Feedback" bar. */
@Composable
fun FeedbackContent(
    state: FeedbackUiState,
    onSelectType: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onDismissDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.feedback_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .contentMaxWidth()
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FeedbackForm(
                state = state,
                onSelectType = onSelectType,
                onDetailsChange = onDetailsChange,
                onEmailChange = onEmailChange,
                onSubmit = onSubmit,
            )
        }
    }
    if (state.showThanks) FeedbackThanksDialog(onDismiss = onBack)
}

@Composable
private fun FeedbackForm(
    state: FeedbackUiState,
    onSelectType: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = state.types.firstOrNull { it.id == state.selectedTypeId }?.label

    LabeledField(stringResource(R.string.feedback_label_type)) {
        PickerField(
            value = selectedLabel,
            placeholder = stringResource(R.string.feedback_type_hint),
            leadingIcon = Icons.Outlined.Category,
            onClick = { pickerOpen = true },
            modifier = Modifier.testTag(FEEDBACK_TYPE_FIELD_TAG),
        )
    }
    if (pickerOpen) {
        PickerSheet(
            title = stringResource(R.string.feedback_picker_title),
            onDismiss = { pickerOpen = false },
        ) {
            state.types.forEach { type ->
                item(key = type.id) {
                    PickerRow(
                        label = type.label,
                        selected = type.id == state.selectedTypeId,
                        onClick = {
                            onSelectType(type.id)
                            pickerOpen = false
                        },
                    )
                }
            }
        }
    }

    LabeledField(stringResource(R.string.feedback_label_details)) {
        FeedbackDetailsField(
            value = state.details,
            onValueChange = onDetailsChange,
            showError = state.showDetailsError,
        )
    }

    LabeledField(stringResource(R.string.feedback_label_email)) {
        ValidatedField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.feedback_email_hint),
            leadingIcon = Icons.Outlined.Email,
            error = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        )
    }

    AsyncSaveButton(
        title = stringResource(R.string.feedback_submit),
        state = state.saveState,
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth().testTag(FEEDBACK_SUBMIT_TAG),
        savedTitle = stringResource(R.string.feedback_sent),
    )
}

/** A label `sm` above its control: the label is its own node before the control, so reading order is label first. */
@Composable
private fun LabeledField(
    label: String,
    control: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        control()
    }
}

/**
 * The thanks dialog (D-06): fixed copy only, never the user's text. OK, a scrim tap and system back all call
 * [onDismiss]. No text field lives in it.
 */
@Composable
fun FeedbackThanksDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.feedback_thanks_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.feedback_thanks_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.feedback_thanks_ok),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}
