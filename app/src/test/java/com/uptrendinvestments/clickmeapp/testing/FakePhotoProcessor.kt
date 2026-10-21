package com.uptrendinvestments.clickmeapp.testing

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.CropRect
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor

/** Scriptable [PhotoProcessor] for ViewModel and repository tests. Creates no Android objects by default. */
class FakePhotoProcessor : PhotoProcessor {
    var importError: Throwable? = null
    var cropError: Throwable? = null
    var source: ImageBitmap? = null
    var preview: ImageBitmap? = null
    var cropped: ByteArray? = null
    var size: IntSize? = null
    var hasSourceValue = false

    var importCalls = 0
        private set
    var cropCalls = 0
        private set
    var clearCalls = 0
        private set
    var lastCropRect: CropRect? = null
        private set

    override suspend fun importPicked(uri: Uri) {
        importCalls++
        importError?.let { throw it }
        hasSourceValue = true
        cropped = null
    }

    override suspend fun hasSource(): Boolean = hasSourceValue

    override suspend fun sourceSize(): IntSize? = size

    override suspend fun loadSource(): ImageBitmap? = source

    override suspend fun cropToJpeg(rect: CropRect) {
        cropCalls++
        cropError?.let { throw it }
        lastCropRect = rect
        cropped = byteArrayOf(1, 2, 3)
    }

    override suspend fun croppedPreview(): ImageBitmap? = preview

    override suspend fun croppedJpeg(): ByteArray? = cropped

    override suspend fun clearWorkingFiles() {
        clearCalls++
        importError = null
        cropError = null
        source = null
        preview = null
        cropped = null
        size = null
        hasSourceValue = false
        lastCropRect = null
    }
}
