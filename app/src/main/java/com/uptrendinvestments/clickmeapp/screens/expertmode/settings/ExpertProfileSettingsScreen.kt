package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.ExpertTagChip
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.subviews.ExpertiseEditorSheet
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.LOCATION_MAX
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.NAME_MAX
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.PROFILE_FIELD_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileTextFieldId
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AutoSaveStatusIcon
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfessionalDetailsSheet
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileAvatarSection
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileBioField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileCountryField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileLanguagesField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileProfessionalCard
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileReadOnlyField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileTextField
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.languageselection.LanguageSelectionSheet
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.subviews.CountryPickerSheet
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import java.util.UUID

/** Test tag on the Expert Settings scroll column. */
const val EXPERT_SETTINGS_LIST_TAG = "expert_settings_list"

/** Test tag on the meeting link field's wrapper. */
const val EXPERT_SETTINGS_MEETING_TAG = "expert_settings_meeting"

/** Callbacks of [ExpertProfileSettingsContent]; every one defaults to a no-op so tests name only what they use. */
class ExpertSettingsActions(
    val onBack: () -> Unit = {},
    val onFieldChange: (ProfileTextFieldId, String) -> Unit = { _, _ -> },
    val onMeetingUrlChange: (String) -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onOpenProfessional: () -> Unit = {},
    val onApplyProfessional: (jobTitle: String, company: String) -> Unit = { _, _ -> },
    val onDiscardProfessional: () -> Unit = {},
    val onChangePhoto: () -> Unit = {},
    val onDismissAvatarError: () -> Unit = {},
    val onOpenLanguages: () -> Unit = {},
    val onLanguageQueryChange: (String) -> Unit = {},
    val onToggleLanguage: (LanguageItem) -> Unit = {},
    val onRetryLanguages: () -> Unit = {},
    val onDismissLanguages: () -> Unit = {},
    val onApplyLanguages: () -> Unit = {},
    val onOpenCountry: () -> Unit = {},
    val onCountryQueryChange: (String) -> Unit = {},
    val onSelectCountry: (String) -> Unit = {},
    val onDismissCountry: () -> Unit = {},
    val onExpertiseSaved: (List<ExpertiseTagItem>) -> Unit = {},
)

/**
 * Port of iOS `ExpertProfileSettingsView`. Like the client screen there is no Save button: edits save themselves, and
 * Back (the arrow or the system gesture) hands a pending edit to the application scope before popping. A confirmed
 * photo crop is pushed with this screen's [origin] so the Dashboard and Profile stacks never hold equal keys.
 */
@Composable
fun ExpertProfileSettingsScreen(
    viewModel: ExpertProfileSettingsViewModel,
    origin: SettingsOrigin,
    onBack: () -> Unit,
    onOpenCrop: (SettingsOrigin) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnBack by rememberUpdatedState(onBack)
    val latestOnOpenCrop by rememberUpdatedState(onOpenCrop)
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(viewModel::onPhotoPicked)
        }
    val openCrop = state.avatar.openCrop
    LaunchedEffect(openCrop) {
        if (openCrop) {
            viewModel.avatarCropOpened()
            latestOnOpenCrop(origin)
        }
    }
    // Runs on every resume, so a crop confirmed (or cancelled) on the crop screen is handled on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    val leave = {
        viewModel.flushPendingSave()
        latestOnBack()
    }
    BackHandler(onBack = leave)
    val actions =
        remember(viewModel, picker, leave) {
            ExpertSettingsActions(
                onBack = leave,
                onFieldChange = viewModel::onFieldChange,
                onMeetingUrlChange = viewModel::onMeetingUrlChange,
                onRetrySave = viewModel::retrySave,
                onRetryLoad = viewModel::retryLoad,
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
                onExpertiseSaved = viewModel::onExpertiseSaved,
            )
        }
    ExpertProfileSettingsContent(state = state, actions = actions, modifier = modifier)
}

/** Stateless Expert Settings: status icon in the bar, the skeleton / error / field stack, sheets and dialogs. */
@Composable
fun ExpertProfileSettingsContent(
    state: ExpertProfileSettingsUiState,
    actions: ExpertSettingsActions,
    modifier: Modifier = Modifier,
    expertiseSheet: @Composable (onDismiss: () -> Unit, onSaved: (List<ExpertiseTagItem>) -> Unit) -> Unit =
        { onDismiss, onSaved ->
            // The slot is only composed while the sheet is open, so this id is new for every opening and survives
            // rotation (rememberSaveable), giving each opening its own ViewModel that re-seeds from the server.
            val session = rememberSaveable { UUID.randomUUID().toString() }
            ExpertiseEditorSheet(
                onDismiss = onDismiss,
                onSaved = onSaved,
                viewModel = hiltViewModel(key = "expertise_editor_$session"),
            )
        },
) {
    var showExpertiseSheet by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.expert_settings_title),
                onBack = actions.onBack,
                actions = { AutoSaveStatusIcon(status = state.status, onRetry = actions.onRetrySave) },
            )
        },
    ) { inner ->
        val snapshot = state.snapshot
        val load = state.load
        when {
            snapshot != null ->
                ExpertSettingsFields(
                    state = state,
                    snapshot = snapshot,
                    actions = actions,
                    onEditExpertise = { showExpertiseSheet = true },
                    modifier = Modifier.padding(inner),
                )
            load is ExpertSettingsLoad.Failed ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(inner),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.expert_settings_load_error),
                        body = load.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.expert_settings_retry), actions.onRetryLoad),
                    )
                }
            else -> ExpertSettingsSkeleton(Modifier.padding(inner))
        }
    }

    if (showExpertiseSheet) {
        expertiseSheet(
            { showExpertiseSheet = false },
            { tags ->
                actions.onExpertiseSaved(tags)
                showExpertiseSheet = false
            },
        )
    }

    if (state.countrySheetOpen) {
        CountryPickerSheet(
            countries = state.visibleCountries,
            selectedCode = state.selectedCountryCode,
            query = state.countryQuery,
            onQueryChange = actions.onCountryQueryChange,
            onSelect = actions.onSelectCountry,
            onDismiss = actions.onDismissCountry,
        )
    }

    state.languageSheet?.let { sheet ->
        LanguageSelectionSheet(
            state = sheet,
            onQueryChange = actions.onLanguageQueryChange,
            onToggle = actions.onToggleLanguage,
            onRetry = actions.onRetryLanguages,
            onCancel = actions.onDismissLanguages,
            onDone = actions.onApplyLanguages,
        )
    }

    val sheetProfile = state.snapshot?.profile
    if (state.professionalSheetOpen && sheetProfile != null) {
        ProfessionalDetailsSheet(
            jobTitle = sheetProfile.jobTitle,
            company = sheetProfile.company,
            onUpdate = actions.onApplyProfessional,
            onDiscard = actions.onDiscardProfessional,
        )
    }

    state.avatar.error?.let { message ->
        AuthErrorDialog(
            message = message.asString(),
            onDismiss = actions.onDismissAvatarError,
            title = stringResource(state.avatar.errorTitleRes ?: R.string.profile_settings_avatar_error_title),
            confirmLabel = stringResource(R.string.profile_settings_got_it),
        )
    }

    state.dialog?.let { message ->
        AuthErrorDialog(
            message = message.asString(),
            onDismiss = actions.onDismissDialog,
            title = stringResource(R.string.profile_settings_save_error_title),
            confirmLabel = stringResource(R.string.profile_settings_got_it),
        )
    }
}

/** The loaded field stack, in the iOS order: the link sits after the professional card, before the location. */
@Composable
private fun ExpertSettingsFields(
    state: ExpertProfileSettingsUiState,
    snapshot: ExpertSettingsSnapshot,
    actions: ExpertSettingsActions,
    onEditExpertise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = snapshot.profile
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .contentMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md)
                .testTag(EXPERT_SETTINGS_LIST_TAG),
    ) {
        ProfileAvatarSection(
            avatarUrl = state.avatarUrl,
            avatar = state.avatar,
            onChangePhoto = actions.onChangePhoto,
        )
        if (state.email.isNotEmpty()) {
            ProfileReadOnlyField(
                label = stringResource(R.string.profile_settings_email_label),
                value = state.email,
            )
        }
        ProfileTextField(
            label = stringResource(R.string.profile_settings_first_name_label),
            value = profile.firstName,
            onValueChange = { actions.onFieldChange(ProfileTextFieldId.FirstName, it) },
            icon = Icons.Outlined.Person,
            modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.FirstName.name),
            error = state.fieldErrors[ProfileTextFieldId.FirstName]?.asString(),
            maxLength = NAME_MAX,
        )
        ProfileTextField(
            label = stringResource(R.string.profile_settings_last_name_label),
            value = profile.lastName,
            onValueChange = { actions.onFieldChange(ProfileTextFieldId.LastName, it) },
            icon = Icons.Outlined.Person,
            modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.LastName.name),
            error = state.fieldErrors[ProfileTextFieldId.LastName]?.asString(),
            maxLength = NAME_MAX,
        )
        ProfileBioField(
            label = stringResource(R.string.profile_settings_bio_label),
            value = profile.bio,
            onValueChange = { actions.onFieldChange(ProfileTextFieldId.Bio, it) },
            error = state.fieldErrors[ProfileTextFieldId.Bio]?.asString(),
        )
        ProfileProfessionalCard(
            jobTitle = profile.jobTitle,
            company = profile.company,
            onClick = actions.onOpenProfessional,
        )
        ProfileTextField(
            label = stringResource(R.string.expert_settings_meeting_label),
            value = state.meetingUrlText,
            onValueChange = actions.onMeetingUrlChange,
            icon = Icons.Outlined.Link,
            modifier = Modifier.testTag(EXPERT_SETTINGS_MEETING_TAG),
            error = state.meetingUrlError?.asString(),
            keyboardOptions = MeetingUrlKeyboard,
            placeholder = stringResource(R.string.expert_settings_meeting_placeholder),
        )
        ExpertiseSection(tags = state.expertiseTags, onEdit = onEditExpertise)
        ProfileTextField(
            label = stringResource(R.string.profile_settings_city_label),
            value = profile.city,
            onValueChange = { actions.onFieldChange(ProfileTextFieldId.City, it) },
            icon = Icons.Outlined.LocationOn,
            modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.City.name),
            error = state.fieldErrors[ProfileTextFieldId.City]?.asString(),
            maxLength = LOCATION_MAX,
        )
        ProfileTextField(
            label = stringResource(R.string.profile_settings_state_label),
            value = profile.stateProvince,
            onValueChange = { actions.onFieldChange(ProfileTextFieldId.StateProvince, it) },
            icon = Icons.Outlined.Map,
            modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.StateProvince.name),
            error = state.fieldErrors[ProfileTextFieldId.StateProvince]?.asString(),
            maxLength = LOCATION_MAX,
        )
        ProfileCountryField(countryCode = profile.countryCode, onClick = actions.onOpenCountry)
        ProfileLanguagesField(languages = profile.languages, onEdit = actions.onOpenLanguages)
    }
}

/** Expertise: the profile tags as read-only chips that wrap, and the button that opens the Edit Expertise sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpertiseSection(
    tags: List<String>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
        Text(
            text = stringResource(R.string.expert_settings_expertise),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                tags.forEach { label -> ExpertTagChip(label = label) }
            }
        }
        PressScaleButton(
            onClick = onEdit,
            modifier =
                Modifier
                    .padding(top = Spacing.md)
                    .fillMaxWidth()
                    .heightIn(min = EditExpertiseMinHeight)
                    .border(BorderStroke(OutlineWidth, MaterialTheme.colorScheme.outline), CircleShape),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier.size(EditExpertiseIconSize),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(text = stringResource(R.string.expert_settings_edit_expertise))
        }
    }
}

private val EditExpertiseMinHeight = 56.dp
private val EditExpertiseIconSize = 24.dp
private val OutlineWidth = 1.dp

private val SkeletonAvatarSize = 96.dp
private val SkeletonFieldHeight = 56.dp
private const val SKELETON_FIELD_COUNT = 6

/** Loading: a 96 dp circle then six 56 dp field blocks, announced once as "Loading". */
@Composable
private fun ExpertSettingsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SkeletonBlock(modifier = Modifier.size(SkeletonAvatarSize), shape = CircleShape)
            Spacer(Modifier.height(Spacing.xs))
            repeat(SKELETON_FIELD_COUNT) {
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().height(SkeletonFieldHeight),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

private val MeetingUrlKeyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false)
