package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AutoSaveStatusIcon
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfessionalDetailsSheet
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileAvatarSection
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileBioField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileLanguagesField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileProfessionalCard
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileReadOnlyField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileTextField
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.languageselection.LanguageSelectionSheet
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the Profile Settings scroll column. */
const val PROFILE_SETTINGS_LIST_TAG = "profile_settings_list"

/** Test tag prefix on each text field's wrapper, followed by the [ProfileTextFieldId] name. */
const val PROFILE_FIELD_TAG_PREFIX = "profile_field_"

/**
 * Port of iOS `ClientProfileSettingsView`. There is no Save button and no discard dialog: edits save themselves. Back
 * (the arrow or the system gesture) hands any pending edit to the application scope first, then pops; the pop is never
 * blocked.
 */
@Composable
fun ClientProfileSettingsScreen(
    viewModel: ClientProfileSettingsViewModel,
    onBack: () -> Unit,
    onOpenCrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnOpenCrop by rememberUpdatedState(onOpenCrop)
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(viewModel::onPhotoPicked)
        }
    val openCrop = state.avatar.openCrop
    LaunchedEffect(openCrop) {
        if (openCrop) {
            viewModel.avatarCropOpened()
            latestOnOpenCrop()
        }
    }
    // Runs on every resume, so a crop confirmed (or cancelled) on the crop screen is handled on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    val latestOnBack by rememberUpdatedState(onBack)
    val leave = {
        viewModel.flushPendingSave()
        latestOnBack()
    }
    BackHandler(onBack = leave)
    ClientProfileSettingsContent(
        state = state,
        onBack = leave,
        onFieldChange = viewModel::onFieldChange,
        onRetrySave = viewModel::retrySave,
        onDismissDialog = viewModel::dismissDialog,
        onOpenProfessional = viewModel::openProfessionalSheet,
        onApplyProfessional = viewModel::applyProfessional,
        onDiscardProfessional = viewModel::discardProfessional,
        onChangePhoto = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onDismissAvatarError = viewModel::dismissAvatarError,
        onOpenLanguages = viewModel::openLanguages,
        onLanguageQueryChange = viewModel::onLanguageQuery,
        onToggleLanguage = viewModel::toggleLanguage,
        onRetryLanguages = viewModel::retryLanguages,
        onDismissLanguages = viewModel::dismissLanguages,
        onApplyLanguages = viewModel::applyLanguages,
        onOpenCountry = viewModel::openCountry,
        onCountryQueryChange = viewModel::onCountryQuery,
        onSelectCountry = viewModel::selectCountry,
        onDismissCountry = viewModel::dismissCountry,
        modifier = modifier,
    )
}

/** Stateless Profile Settings: status icon in the bar, the field stack, and the save-failure dialog. */
@Composable
fun ClientProfileSettingsContent(
    state: ClientProfileSettingsUiState,
    onBack: () -> Unit,
    onFieldChange: (ProfileTextFieldId, String) -> Unit,
    onRetrySave: () -> Unit,
    onDismissDialog: () -> Unit,
    onOpenProfessional: () -> Unit,
    onApplyProfessional: (jobTitle: String, company: String) -> Unit,
    onDiscardProfessional: () -> Unit,
    onChangePhoto: () -> Unit,
    onDismissAvatarError: () -> Unit,
    onOpenLanguages: () -> Unit,
    onLanguageQueryChange: (String) -> Unit,
    onToggleLanguage: (LanguageItem) -> Unit,
    onRetryLanguages: () -> Unit,
    onDismissLanguages: () -> Unit,
    onApplyLanguages: () -> Unit,
    onOpenCountry: () -> Unit,
    onCountryQueryChange: (String) -> Unit,
    onSelectCountry: (String) -> Unit,
    onDismissCountry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.profile_settings_title),
                onBack = onBack,
                actions = { AutoSaveStatusIcon(status = state.status, onRetry = onRetrySave) },
            )
        },
    ) { inner ->
        val snapshot = state.snapshot
        if (snapshot == null) {
            val loading = stringResource(R.string.profile_settings_loading)
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.md, vertical = Spacing.md)
                        .testTag(PROFILE_SETTINGS_LIST_TAG),
            ) {
                ProfileAvatarSection(
                    avatarUrl = state.avatarUrl,
                    avatar = state.avatar,
                    onChangePhoto = onChangePhoto,
                )
                if (state.email.isNotEmpty()) {
                    ProfileReadOnlyField(
                        label = stringResource(R.string.profile_settings_email_label),
                        value = state.email,
                    )
                }
                ProfileTextField(
                    label = stringResource(R.string.profile_settings_first_name_label),
                    value = snapshot.firstName,
                    onValueChange = { onFieldChange(ProfileTextFieldId.FirstName, it) },
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.FirstName.name),
                    error = state.fieldErrors[ProfileTextFieldId.FirstName]?.asString(),
                    maxLength = NAME_MAX,
                )
                ProfileTextField(
                    label = stringResource(R.string.profile_settings_last_name_label),
                    value = snapshot.lastName,
                    onValueChange = { onFieldChange(ProfileTextFieldId.LastName, it) },
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.LastName.name),
                    error = state.fieldErrors[ProfileTextFieldId.LastName]?.asString(),
                    maxLength = NAME_MAX,
                )
                ProfileBioField(
                    label = stringResource(R.string.profile_settings_bio_label),
                    value = snapshot.bio,
                    onValueChange = { onFieldChange(ProfileTextFieldId.Bio, it) },
                    error = state.fieldErrors[ProfileTextFieldId.Bio]?.asString(),
                )
                ProfileProfessionalCard(
                    jobTitle = snapshot.jobTitle,
                    company = snapshot.company,
                    onClick = onOpenProfessional,
                )
                ProfileTextField(
                    label = stringResource(R.string.profile_settings_city_label),
                    value = snapshot.city,
                    onValueChange = { onFieldChange(ProfileTextFieldId.City, it) },
                    icon = Icons.Outlined.LocationOn,
                    modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.City.name),
                    error = state.fieldErrors[ProfileTextFieldId.City]?.asString(),
                    maxLength = LOCATION_MAX,
                )
                ProfileTextField(
                    label = stringResource(R.string.profile_settings_state_label),
                    value = snapshot.stateProvince,
                    onValueChange = { onFieldChange(ProfileTextFieldId.StateProvince, it) },
                    icon = Icons.Outlined.Map,
                    modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.StateProvince.name),
                    error = state.fieldErrors[ProfileTextFieldId.StateProvince]?.asString(),
                    maxLength = LOCATION_MAX,
                )
                ProfileLanguagesField(languages = snapshot.languages, onEdit = onOpenLanguages)
            }
        }
    }

    state.languageSheet?.let { sheet ->
        LanguageSelectionSheet(
            state = sheet,
            onQueryChange = onLanguageQueryChange,
            onToggle = onToggleLanguage,
            onRetry = onRetryLanguages,
            onCancel = onDismissLanguages,
            onDone = onApplyLanguages,
        )
    }

    val sheetSnapshot = state.snapshot
    if (state.professionalSheetOpen && sheetSnapshot != null) {
        ProfessionalDetailsSheet(
            jobTitle = sheetSnapshot.jobTitle,
            company = sheetSnapshot.company,
            onUpdate = onApplyProfessional,
            onDiscard = onDiscardProfessional,
        )
    }

    state.avatar.error?.let { message ->
        AuthErrorDialog(
            message = message.asString(),
            onDismiss = onDismissAvatarError,
            title = stringResource(state.avatar.errorTitleRes ?: R.string.profile_settings_avatar_error_title),
            confirmLabel = stringResource(R.string.profile_settings_got_it),
        )
    }

    state.dialog?.let { message ->
        AuthErrorDialog(
            message = message.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.profile_settings_save_error_title),
            confirmLabel = stringResource(R.string.profile_settings_got_it),
        )
    }
}
