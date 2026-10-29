package com.uptrendinvestments.clickmeapp.testing

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.CropRect
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoImportException
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor

/**
 * Scriptable [PhotoProcessor] for ViewModel and repository tests that models the real processor's two working files
 * (WR-08), so a test cannot reach a state production cannot:
 * - [importPicked] writes the working copy and deletes any stored crop;
 * - [cropToJpeg] needs a working copy and stores a crop;
 * - [croppedPreview] and [croppedJpeg] are non-null exactly when a crop is stored;
 * - [clearWorkingFiles] deletes both files.
 *
 * Test configuration (the scripted errors and the bitmaps the files decode to) is kept apart from the simulated file
 * state and is never reset by [clearWorkingFiles]. No Android object is created unless a test asks for a preview of a
 * stored crop without configuring [previewBitmap].
 */
class FakePhotoProcessor : PhotoProcessor {
    // ---- test configuration ----

    /** Thrown by every [importPicked] until set back to null. */
    var importError: Throwable? = null

    /** Thrown by every [cropToJpeg] until set back to null. */
    var cropError: Throwable? = null

    /** Thrown by every [croppedPreview] until set back to null. */
    var previewError: Throwable? = null

    /** What the working copy decodes to in [loadSource]. */
    var sourceBitmap: ImageBitmap? = null

    /** The working copy's pixel size reported by [sourceSize]. */
    var sourcePixelSize: IntSize? = null

    /** What the stored crop decodes to in [croppedPreview]; a 1x1 bitmap is used when unset. */
    var previewBitmap: ImageBitmap? = null

    /** The JPEG bytes [cropToJpeg] stores. */
    var cropBytes: ByteArray = byteArrayOf(1, 2, 3)

    // ---- simulated file state ----

    var hasSourceFile = false
        private set

    /** The stored crop's bytes, or null when no crop file exists. */
    var storedCrop: ByteArray? = null
        private set

    // ---- call recording ----

    var importCalls = 0
        private set
    var cropCalls = 0
        private set
    var clearCalls = 0
        private set
    var lastCropRect: CropRect? = null
        private set

    private val defaultPreview by lazy { ImageBitmap(1, 1) }

    /** Puts a working copy on "disk", as a successful [importPicked] does (any stored crop is deleted). */
    fun givenSource(
        bitmap: ImageBitmap? = sourceBitmap,
        size: IntSize? = sourcePixelSize,
    ) {
        sourceBitmap = bitmap
        sourcePixelSize = size
        hasSourceFile = true
        storedCrop = null
    }

    /** Puts a working copy and a stored crop on "disk", as an import followed by a successful crop does. */
    fun givenCrop(bytes: ByteArray = cropBytes) {
        hasSourceFile = true
        storedCrop = bytes
    }

    override suspend fun importPicked(uri: Uri) {
        importCalls++
        importError?.let { throw it }
        hasSourceFile = true
        storedCrop = null
    }

    override suspend fun hasSource(): Boolean = hasSourceFile

    override suspend fun sourceSize(): IntSize? = if (hasSourceFile) sourcePixelSize else null

    override suspend fun loadSource(): ImageBitmap? = if (hasSourceFile) sourceBitmap else null

    override suspend fun cropToJpeg(rect: CropRect) {
        cropCalls++
        cropError?.let { throw it }
        if (!hasSourceFile) throw PhotoImportException("No source image")
        lastCropRect = rect
        storedCrop = cropBytes
    }

    override suspend fun croppedPreview(): ImageBitmap? {
        previewError?.let { throw it }
        return if (storedCrop != null) previewBitmap ?: defaultPreview else null
    }

    override suspend fun croppedJpeg(): ByteArray? = storedCrop

    override suspend fun clearWorkingFiles() {
        clearCalls++
        hasSourceFile = false
        storedCrop = null
    }
}
