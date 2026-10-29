package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * What the avatar block shows. [preview] is the freshly cropped photo while it uploads; [error] is the dialog message
 * with [errorTitleRes] as its title (null means "Couldn't upload avatar"); [openCrop] asks the screen to push the crop.
 */
data class AvatarUiState(
    val isImporting: Boolean = false,
    val isUploading: Boolean = false,
    val error: FieldError? = null,
    @StringRes val errorTitleRes: Int? = null,
    val openCrop: Boolean = false,
    val preview: ImageBitmap? = null,
)

/** Pick, crop and upload of the profile avatar, shared by the client and expert settings screens. */
class ProfileAvatarController
    @Inject
    constructor(
        private val photoProcessor: PhotoProcessor,
        private val repository: ProfileSettingsRepository,
        private val userManager: UserManager,
        @ApplicationScope private val appScope: CoroutineScope,
    ) {
        private val _state = MutableStateFlow(AvatarUiState())
        val state: StateFlow<AvatarUiState> = _state

        fun onPicked(
            uri: Uri,
            scope: CoroutineScope,
        ) {
            TODO()
        }

        fun onCropOpened() {
            TODO()
        }

        fun onReturnedFromCrop() {
            TODO()
        }

        fun dismissError() {
            TODO()
        }
    }
