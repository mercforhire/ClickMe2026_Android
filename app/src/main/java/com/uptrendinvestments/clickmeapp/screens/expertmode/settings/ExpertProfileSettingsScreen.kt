package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.NAME_MAX
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.PROFILE_FIELD_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileTextFieldId
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AutoSaveStatusIcon
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileReadOnlyField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfileTextField
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

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
)

/**
 * Port of iOS `ExpertProfileSettingsView`. Like the client screen there is no Save button: edits save themselves, and
 * Back (the arrow or the system gesture) hands a pending edit to the application scope before popping.
 */
@Composable
fun ExpertProfileSettingsScreen(
    viewModel: ExpertProfileSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnBack by rememberUpdatedState(onBack)
    val leave = {
        viewModel.flushPendingSave()
        latestOnBack()
    }
    BackHandler(onBack = leave)
    val actions =
        remember(viewModel, leave) {
            ExpertSettingsActions(
                onBack = leave,
                onFieldChange = viewModel::onFieldChange,
                onMeetingUrlChange = viewModel::onMeetingUrlChange,
                onRetrySave = viewModel::retrySave,
                onRetryLoad = viewModel::retryLoad,
                onDismissDialog = viewModel::dismissDialog,
            )
        }
    ExpertProfileSettingsContent(state = state, actions = actions, modifier = modifier)
}

/** Stateless Expert Settings: status icon in the bar, the field stack, and the save-failure dialog. */
@Composable
fun ExpertProfileSettingsContent(
    state: ExpertProfileSettingsUiState,
    actions: ExpertSettingsActions,
    modifier: Modifier = Modifier,
) {
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
                        .testTag(EXPERT_SETTINGS_LIST_TAG),
            ) {
                if (state.email.isNotEmpty()) {
                    ProfileReadOnlyField(
                        label = stringResource(R.string.profile_settings_email_label),
                        value = state.email,
                    )
                }
                ProfileTextField(
                    label = stringResource(R.string.profile_settings_first_name_label),
                    value = snapshot.profile.firstName,
                    onValueChange = { actions.onFieldChange(ProfileTextFieldId.FirstName, it) },
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.FirstName.name),
                    error = state.fieldErrors[ProfileTextFieldId.FirstName]?.asString(),
                    maxLength = NAME_MAX,
                )
                ProfileTextField(
                    label = stringResource(R.string.profile_settings_last_name_label),
                    value = snapshot.profile.lastName,
                    onValueChange = { actions.onFieldChange(ProfileTextFieldId.LastName, it) },
                    icon = Icons.Outlined.Person,
                    modifier = Modifier.testTag(PROFILE_FIELD_TAG_PREFIX + ProfileTextFieldId.LastName.name),
                    error = state.fieldErrors[ProfileTextFieldId.LastName]?.asString(),
                    maxLength = NAME_MAX,
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
            }
        }
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

private val MeetingUrlKeyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false)
