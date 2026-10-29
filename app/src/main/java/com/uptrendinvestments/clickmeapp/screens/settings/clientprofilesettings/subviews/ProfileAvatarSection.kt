package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AvatarUiState
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the 96 dp upload spinner. */
const val PROFILE_AVATAR_PROGRESS_TAG = "profile_avatar_progress"

/** Test tag on the tappable avatar. */
const val PROFILE_AVATAR_TAG = "profile_avatar"

private val AvatarSize = 96.dp
private val SpinnerSize = 32.dp
private val ChangePhotoMinHeight = 48.dp
private const val SCRIM_ALPHA = 0.4f

/**
 * Centered 96 dp avatar with a "Change photo" text button underneath. Both open the photo picker through
 * [onChangePhoto]. While [AvatarUiState.isUploading] a 40% scrim and a spinner cover the avatar, which shows the
 * freshly cropped photo; the previous avatar stays until the upload succeeds.
 */
@Composable
fun ProfileAvatarSection(
    avatarUrl: String?,
    avatar: AvatarUiState,
    onChangePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a11y = stringResource(R.string.profile_settings_avatar_a11y)
    val uploadingText = stringResource(R.string.profile_settings_avatar_uploading)
    val busy = avatar.isUploading || avatar.isImporting
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(AvatarSize)) {
            Box(
                modifier =
                    Modifier
                        .size(AvatarSize)
                        .testTag(PROFILE_AVATAR_TAG)
                        .clip(CircleShape)
                        .clickable(enabled = !busy, role = Role.Button, onClick = onChangePhoto)
                        .semantics { contentDescription = a11y },
            ) {
                Avatar(url = avatarUrl, contentDescription = null, size = AvatarSize, image = avatar.preview)
            }
            if (avatar.isUploading) {
                Box(
                    modifier =
                        Modifier
                            .size(AvatarSize)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA), CircleShape)
                            .testTag(PROFILE_AVATAR_PROGRESS_TAG)
                            .semantics { contentDescription = uploadingText },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SpinnerSize),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        TextButton(
            onClick = onChangePhoto,
            enabled = !busy,
            modifier = Modifier.heightIn(min = ChangePhotoMinHeight),
        ) {
            Text(
                text = stringResource(R.string.profile_settings_change_photo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}
