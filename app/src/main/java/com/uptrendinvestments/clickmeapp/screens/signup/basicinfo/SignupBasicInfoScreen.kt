package com.uptrendinvestments.clickmeapp.screens.signup.basicinfo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerField
import com.uptrendinvestments.clickmeapp.components.SelectableChip
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.languageselection.LanguageSelectionSheet
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.subviews.CountryPickerSheet
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val AddLanguageHeight = 56.dp
private val IconSize = 24.dp

/**
 * Hilt-aware Personal Details step (iOS `SignupBasicInfoView`). [onSaved] is called once after the 600 ms
 * "Saved!" hold; the caller pops back to the hub.
 */
@Composable
fun SignupBasicInfoScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupBasicInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) {
            onSaved()
            viewModel.onSavedConsumed()
        }
    }

    SignupBasicInfoContent(
        state = state,
        onFirstNameChange = viewModel::onFirstNameChange,
        onCityChange = viewModel::onCityChange,
        onProvinceChange = viewModel::onProvinceChange,
        onOpenCountrySheet = viewModel::openCountrySheet,
        onOpenLanguageSheet = viewModel::openLanguageSheet,
        onRemoveLanguage = viewModel::removeLanguage,
        onSave = viewModel::save,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
    )

    if (state.showCountrySheet) {
        CountryPickerSheet(
            countries = state.visibleCountries,
            selectedCode = state.countryCode,
            query = state.countryQuery,
            onQueryChange = viewModel::onCountryQueryChange,
            onSelect = viewModel::selectCountry,
            onDismiss = viewModel::dismissCountrySheet,
        )
    }
    state.languageSheet?.let { sheet ->
        LanguageSelectionSheet(
            state = sheet,
            onQueryChange = viewModel::onLanguageQueryChange,
            onToggle = viewModel::toggleLanguage,
            onRetry = viewModel::retryLanguages,
            onCancel = viewModel::dismissLanguageSheet,
            onDone = viewModel::confirmLanguageSheet,
        )
    }
}

/** Stateless Personal Details form: scrolling fields with the Save button pinned above the keyboard. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignupBasicInfoContent(
    state: SignupBasicInfoUiState,
    onFirstNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onProvinceChange: (String) -> Unit,
    onOpenCountrySheet: () -> Unit,
    onOpenLanguageSheet: () -> Unit,
    onRemoveLanguage: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = state.saveState == SaveState.Idle
    // WR-06: leaving mid-save would cancel the request after the PATCH may have landed, before the draft is updated;
    // swallow system Back and disable the arrow until it finishes (as Tags does).
    BackHandler(enabled = !editable) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.signup_basic_info_title),
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
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .contentMaxWidth()
                        .padding(horizontal = Spacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.signup_basic_info_intro),
                    modifier = Modifier.padding(vertical = Spacing.lg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                ValidatedField(
                    value = state.firstName,
                    onValueChange = onFirstNameChange,
                    placeholder = stringResource(R.string.signup_basic_info_first_name),
                    leadingIcon = Icons.Outlined.Person,
                    error = state.firstNameError?.asString(),
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    enabled = editable,
                    contentType = ContentType.PersonFirstName,
                )
                ValidatedField(
                    value = state.city,
                    onValueChange = onCityChange,
                    placeholder = stringResource(R.string.signup_basic_info_city),
                    leadingIcon = Icons.Outlined.LocationCity,
                    error = state.cityError?.asString(),
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    enabled = editable,
                )
                ValidatedField(
                    value = state.provinceState,
                    onValueChange = onProvinceChange,
                    placeholder = stringResource(R.string.signup_basic_info_province),
                    leadingIcon = Icons.Outlined.Public,
                    error = state.provinceError?.asString(),
                    keyboardOptions =
                        KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    enabled = editable,
                )
                PickerField(
                    value = state.countryName,
                    placeholder = stringResource(R.string.signup_basic_info_country),
                    leadingIcon = Icons.Outlined.Flag,
                    onClick = onOpenCountrySheet,
                    error = state.countryError?.asString(),
                    enabled = editable,
                )
                Text(
                    text = stringResource(R.string.signup_basic_info_languages_label),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    state.languages.forEach { language ->
                        SelectableChip(
                            label = language.label,
                            selected = true,
                            onClick = { if (editable) onRemoveLanguage(language.id) },
                            removeContentDescription =
                                stringResource(R.string.signup_basic_info_remove_language, language.label),
                        )
                    }
                }
                state.languagesError?.let {
                    Text(
                        text = it.asString(),
                        modifier = Modifier.padding(top = Spacing.xs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.extras.destructive,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                AddLanguageRow(onClick = onOpenLanguageSheet, enabled = editable)
                Spacer(Modifier.height(Spacing.xl))
            }
            AsyncSaveButton(
                title = stringResource(R.string.signup_save),
                savedTitle = stringResource(R.string.signup_saved),
                state = state.saveState,
                onClick = onSave,
                modifier = Modifier.contentMaxWidth().padding(Spacing.lg),
            )
        }
    }

    state.dialogError?.let {
        AuthErrorDialog(
            message = it.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.signup_error_save_title),
        )
    }
}

/** Pill-shaped "Add language..." row that opens the Language Selection sheet. */
@Composable
private fun AddLanguageRow(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = AddLanguageHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        Text(
            text = stringResource(R.string.signup_basic_info_add_language),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}
