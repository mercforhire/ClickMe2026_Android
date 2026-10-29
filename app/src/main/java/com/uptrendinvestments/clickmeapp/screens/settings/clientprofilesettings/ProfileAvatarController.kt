package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
        val state: StateFlow<AvatarUiState> = _state.asStateFlow()

        /** True from the moment the crop screen is pushed until the screen is shown again. */
        private var awaitingCrop = false

        /**
         * A photo was picked. The picker URI is read once by [PhotoProcessor.importPicked] on [scope] and never stored;
         * on success the screen is asked to open the crop. Ignored while importing or uploading.
         */
        @Suppress("TooGenericExceptionCaught")
        fun onPicked(
            uri: Uri,
            scope: CoroutineScope,
        ) {
            val current = _state.value
            if (current.isImporting || current.isUploading) return
            _state.update { it.copy(isImporting = true, error = null, errorTitleRes = null) }
            scope.launch {
                try {
                    photoProcessor.importPicked(uri)
                    _state.update { it.copy(isImporting = false, openCrop = true) }
                } catch (e: CancellationException) {
                    _state.update { it.copy(isImporting = false) }
                    throw e
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isImporting = false,
                            error = FieldError.Res(R.string.signup_photo_open_error_body),
                            errorTitleRes = R.string.signup_photo_open_error_title,
                        )
                    }
                }
            }
        }

        /** The screen pushed the crop route. */
        fun onCropOpened() {
            _state.update { it.copy(openCrop = false) }
            awaitingCrop = true
        }

        /**
         * The settings screen is shown again (called on every resume; a no-op unless the crop was opened). A stored
         * crop is uploaded on the application scope so leaving the screen cannot cancel it; no stored crop means the
         * user cancelled, so the working files are deleted and nothing is uploaded.
         */
        fun onReturnedFromCrop() {
            if (!awaitingCrop) return
            awaitingCrop = false
            appScope.launch { uploadStoredCrop() }
        }

        fun dismissError() {
            _state.update { it.copy(error = null, errorTitleRes = null) }
        }

        private suspend fun uploadStoredCrop() {
            val bytes = photoProcessor.croppedJpeg()
            if (bytes == null) {
                photoProcessor.clearWorkingFiles()
                return
            }
            _state.update { it.copy(isUploading = true, preview = null, error = null, errorTitleRes = null) }
            try {
                _state.update { it.copy(preview = photoProcessor.croppedPreview()) }
                repository.uploadAvatar(bytes)
                photoProcessor.clearWorkingFiles()
                try {
                    userManager.refreshProfile()
                } catch (_: ApiException) {
                    // The upload succeeded; the hub card and top bar catch up on the next profile refresh.
                }
            } catch (e: ApiException) {
                photoProcessor.clearWorkingFiles()
                _state.update { it.copy(error = e.toSaveDialogError(), errorTitleRes = null) }
            } finally {
                _state.update { it.copy(isUploading = false, preview = null) }
            }
        }
    }
