package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileTextFieldId

/** Callbacks of [ExpertProfileSettingsContent]; every one defaults to a no-op so tests name only what they use. */
class ExpertSettingsActions(
    val onBack: () -> Unit = {},
    val onFieldChange: (ProfileTextFieldId, String) -> Unit = { _, _ -> },
    val onMeetingUrlChange: (String) -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
)

@Composable
fun ExpertProfileSettingsContent(
    state: ExpertProfileSettingsUiState,
    actions: ExpertSettingsActions,
    modifier: Modifier = Modifier,
) = Unit
