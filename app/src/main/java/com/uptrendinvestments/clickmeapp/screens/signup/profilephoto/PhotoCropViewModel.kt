package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoCropUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val source: ImageBitmap? = null,
    val sourceSize: IntSize? = null,
    val transform: CropTransform = CropTransform.Unset,
    val guideRadius: Float = 0f,
    val isCropping: Boolean = false,
    val dialogError: FieldError? = null,
)

private const val KEY_ZOOM_FACTOR = "crop_zoom_factor"
private const val KEY_OFFSET_X = "crop_offset_x_r"
private const val KEY_OFFSET_Y = "crop_offset_y_r"

/**
 * Crop step state (D-12). Loads the app-private working copy, keeps a transform that is clamped on every committed
 * change and saves it normalised (zoom relative to the minimum scale, offset relative to the guide radius) so a
 * rotation that resizes the guide restores the same framing. The picker source is never touched here.
 */
@HiltViewModel
class PhotoCropViewModel
    @Inject
    constructor(
        private val photoProcessor: PhotoProcessor,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PhotoCropUiState())
        val uiState: StateFlow<PhotoCropUiState> = _uiState.asStateFlow()

        private val _done = MutableStateFlow(false)
        val done: StateFlow<Boolean> = _done.asStateFlow()

        init {
            viewModelScope.launch { load() }
        }

        @Suppress("TooGenericExceptionCaught")
        private suspend fun load() {
            try {
                val bitmap = photoProcessor.loadSource()
                if (bitmap == null) {
                    failLoad()
                    return
                }
                val size = photoProcessor.sourceSize() ?: IntSize(bitmap.width, bitmap.height)
                _uiState.update { it.copy(isLoading = false, source = bitmap, sourceSize = size) }
                initTransform()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failLoad()
            }
        }

        private fun failLoad() {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loadFailed = true,
                    dialogError = FieldError.Res(R.string.signup_photo_open_error_body),
                )
            }
        }

        fun onGuideMeasured(radiusPx: Float) {
            if (radiusPx <= 0f) return
            _uiState.update { it.copy(guideRadius = radiusPx) }
            initTransform()
        }

        /** Builds the transform once both the image size and the guide radius are known (and again on resize). */
        private fun initTransform() {
            val state = _uiState.value
            val size = state.sourceSize ?: return
            val radius = state.guideRadius
            if (radius <= 0f) return
            val min = CropMath.minScale(size.width, size.height, radius)
            val zoomFactor = savedStateHandle.get<Float>(KEY_ZOOM_FACTOR)
            val raw =
                if (zoomFactor != null) {
                    val xr = savedStateHandle.get<Float>(KEY_OFFSET_X) ?: 0f
                    val yr = savedStateHandle.get<Float>(KEY_OFFSET_Y) ?: 0f
                    CropTransform(min * zoomFactor, Offset(xr * radius, yr * radius))
                } else {
                    CropTransform(min, Offset.Zero)
                }
            _uiState.update { it.copy(transform = CropMath.clamp(raw, size.width, size.height, radius)) }
        }

        /** Applies a live gesture. The offset stays raw so the drag follows the finger; [onGestureEnd] clamps it. */
        fun onGesture(
            centroidFromCenter: Offset,
            zoom: Float,
            pan: Offset,
        ) {
            val state = _uiState.value
            val size = state.sourceSize ?: return
            if (state.guideRadius <= 0f || state.transform.scale <= 0f) return
            val min = CropMath.minScale(size.width, size.height, state.guideRadius)
            val next =
                CropMath.applyGesture(
                    current = state.transform,
                    centroidFromCenter = centroidFromCenter,
                    zoom = zoom,
                    pan = pan,
                    minScale = min,
                    maxScale = CropMath.maxScale(min),
                )
            _uiState.update { it.copy(transform = next) }
        }

        fun onGestureEnd() = commit { it }

        fun onDoubleTap() {
            val min = currentMinScale() ?: return
            commit { CropMath.toggleDoubleTap(it, min) }
        }

        fun zoomIn() = zoomStep(true)

        fun zoomOut() = zoomStep(false)

        private fun zoomStep(zoomIn: Boolean) {
            val min = currentMinScale() ?: return
            commit { CropMath.zoomStep(it, zoomIn, min, CropMath.maxScale(min)) }
        }

        fun move(
            dx: Int,
            dy: Int,
        ) {
            val radius = _uiState.value.guideRadius
            if (radius <= 0f) return
            commit { CropMath.moveStep(it, dx, dy, radius) }
        }

        private fun currentMinScale(): Float? {
            val state = _uiState.value
            val size = state.sourceSize ?: return null
            if (state.guideRadius <= 0f) return null
            return CropMath.minScale(size.width, size.height, state.guideRadius)
        }

        /** Transforms the current state, clamps it, publishes it and saves the normalised form. */
        private fun commit(change: (CropTransform) -> CropTransform) {
            val state = _uiState.value
            val size = state.sourceSize ?: return
            val radius = state.guideRadius
            if (radius <= 0f || state.transform.scale <= 0f) return
            val next = CropMath.clamp(change(state.transform), size.width, size.height, radius)
            _uiState.update { it.copy(transform = next) }
            val min = CropMath.minScale(size.width, size.height, radius)
            savedStateHandle[KEY_ZOOM_FACTOR] = next.scale / min
            savedStateHandle[KEY_OFFSET_X] = next.offset.x / radius
            savedStateHandle[KEY_OFFSET_Y] = next.offset.y / radius
        }

        @Suppress("TooGenericExceptionCaught")
        fun useCrop() {
            val state = _uiState.value
            val size = state.sourceSize ?: return
            if (state.isCropping || _done.value || state.guideRadius <= 0f || state.transform.scale <= 0f) return
            _uiState.update { it.copy(isCropping = true, dialogError = null) }
            val rect = CropMath.sourceRect(state.transform, size.width, size.height, state.guideRadius)
            viewModelScope.launch {
                try {
                    photoProcessor.cropToJpeg(rect)
                    _uiState.update { it.copy(isCropping = false) }
                    _done.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isCropping = false, dialogError = FieldError.Res(R.string.login_error_unknown))
                    }
                }
            }
        }

        fun dismissDialog() = _uiState.update { it.copy(dialogError = null) }

        fun onDoneConsumed() {
            _done.value = false
        }
    }
