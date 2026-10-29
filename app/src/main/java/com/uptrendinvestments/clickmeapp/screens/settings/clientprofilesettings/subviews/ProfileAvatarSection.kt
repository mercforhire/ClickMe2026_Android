package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AvatarUiState

/** Test tag on the 96 dp upload spinner. */
const val PROFILE_AVATAR_PROGRESS_TAG = "profile_avatar_progress"

/** Test tag on the tappable avatar. */
const val PROFILE_AVATAR_TAG = "profile_avatar"

@Composable
fun ProfileAvatarSection(
    avatarUrl: String?,
    avatar: AvatarUiState,
    onChangePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // RED stub.
}
