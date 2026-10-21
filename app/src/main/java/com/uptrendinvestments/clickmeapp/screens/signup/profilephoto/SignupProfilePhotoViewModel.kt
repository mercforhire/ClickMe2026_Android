package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile Photo step state. [localPreview] is the freshly cropped, not yet uploaded image; [avatarUrl] is the saved
 * server avatar. Nothing here is written to saved state: the picker URI and image data never leave the processor.
 */
data class SignupProfilePhotoUiState(
    val avatarUrl: String? = null,
    val localPreview: ImageBitmap? = null,
    val hasSource: Boolean = false,
    val isImporting: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
    val showRemoveConfirm: Boolean = false,
    val dialogError: FieldError? = null,
    @StringRes val dialogTitleRes: Int? = null,
) {
    val hasAnyPhoto: Boolean get() = !avatarUrl.isNullOrBlank() || localPreview != null
    val hasUnsavedPhoto: Boolean get() = localPreview != null
}

/**
 * Port of iOS `SignupProfilePhotoViewModel` with the D-12 pick, crop, preview, Save flow. A pick is imported into the
 * app-private working copy and then handed to the crop screen ([openCrop]); [onShown] re-reads the processor on every
 * entry, so the cropped preview appears when the crop screen returns.
 */
@HiltViewModel
class SignupProfilePhotoViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
        private val photoProcessor: PhotoProcessor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SignupProfilePhotoUiState(avatarUrl = repository.draft.value.avatarUrl))
        val uiState: StateFlow<SignupProfilePhotoUiState> = _uiState.asStateFlow()

        private val _openCrop = MutableStateFlow(false)
        val openCrop: StateFlow<Boolean> = _openCrop.asStateFlow()

        private val _saved = MutableStateFlow(false)
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        /** True while a DELETE is in flight, so Save and a second confirm are ignored. */
        private var removing = false

        init {
            viewModelScope.launch {
                repository.draft.collect { draft -> _uiState.update { it.copy(avatarUrl = draft.avatarUrl) } }
            }
        }

        /** Called on every entry (including the return from the crop screen) to pick up the stored crop. */
        fun onShown() {
            viewModelScope.launch { refreshLocal() }
        }

        private suspend fun refreshLocal() {
            val source = photoProcessor.hasSource()
            val preview = photoProcessor.croppedPreview()
            _uiState.update { it.copy(hasSource = source, localPreview = preview) }
        }

        @Suppress("TooGenericExceptionCaught")
        fun onPicked(uri: Uri) {
            if (_uiState.value.isImporting) return
            _uiState.update { it.copy(isImporting = true) }
            viewModelScope.launch {
                try {
                    photoProcessor.importPicked(uri)
                    refreshLocal()
                    _uiState.update { it.copy(isImporting = false) }
                    _openCrop.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            dialogError = FieldError.Res(R.string.signup_photo_open_error_body),
                            dialogTitleRes = R.string.signup_photo_open_error_title,
                        )
                    }
                }
            }
        }

        fun onOpenCropConsumed() {
            _openCrop.value = false
        }

        fun onSavedConsumed() {
            _saved.value = false
        }

        fun save() {
            if (_uiState.value.saveState != SaveState.Idle || removing) return
            _uiState.update { it.copy(saveState = SaveState.Saving, dialogError = null, dialogTitleRes = null) }
            viewModelScope.launch {
                try {
                    // D-04: with nothing new to upload, Save just returns.
                    val bytes = photoProcessor.croppedJpeg()
                    if (bytes != null) {
                        repository.uploadAvatar(bytes)
                        photoProcessor.clearWorkingFiles()
                    }
                    _uiState.update {
                        it.copy(
                            saveState = SaveState.Saved,
                            localPreview = if (bytes != null) null else it.localPreview,
                            hasSource = if (bytes != null) false else it.hasSource,
                        )
                    }
                    delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                    _saved.value = true
                } catch (e: ApiException) {
                    // The local crop stays so the user can retry.
                    _uiState.update {
                        it.copy(
                            saveState = SaveState.Idle,
                            dialogError = e.toDialogError(),
                            dialogTitleRes = R.string.signup_photo_upload_error_title,
                        )
                    }
                }
            }
        }

        /** Remove Photo tapped: a never-saved pick is cleared locally; a saved avatar asks for confirmation. */
        fun requestRemove() {
            val state = _uiState.value
            if (state.saveState != SaveState.Idle || removing) return
            when {
                state.hasUnsavedPhoto ->
                    viewModelScope.launch {
                        photoProcessor.clearWorkingFiles()
                        _uiState.update { it.copy(localPreview = null, hasSource = false) }
                    }
                !state.avatarUrl.isNullOrBlank() -> _uiState.update { it.copy(showRemoveConfirm = true) }
            }
        }

        fun dismissRemove() {
            _uiState.update { it.copy(showRemoveConfirm = false) }
        }

        fun confirmRemove() {
            if (!_uiState.value.showRemoveConfirm || removing) return
            removing = true
            _uiState.update { it.copy(showRemoveConfirm = false) }
            viewModelScope.launch {
                try {
                    repository.removeAvatar()
                } catch (e: ApiException) {
                    _uiState.update { it.copy(dialogError = e.toDialogError(), dialogTitleRes = null) }
                } finally {
                    removing = false
                }
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialogError = null, dialogTitleRes = null) }
        }
    }
