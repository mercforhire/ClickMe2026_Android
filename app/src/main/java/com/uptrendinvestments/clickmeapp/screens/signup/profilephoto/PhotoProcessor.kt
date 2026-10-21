package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import java.io.IOException
import kotlin.math.roundToInt

/** Thrown when a picked image cannot be decoded or copied into the app-private working file. */
class PhotoImportException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * The on-device profile photo pipeline: import (decode, rotate, downsample) a picked image into an app-private working
 * file, crop it to a square JPEG, and clean up.
 *
 * The picker URI is read once by [importPicked] and never stored; every later step reads the app-private working file
 * in `cacheDir/signup`, which survives process death while the picker URI may not.
 */
interface PhotoProcessor {
    /** Reads [uri] once, writes the working copy (longest side at most 2048 px, EXIF-rotated) and drops any old crop. */
    suspend fun importPicked(uri: Uri)

    /** True when a working copy exists. */
    suspend fun hasSource(): Boolean

    /** Pixel size of the working copy (bounds-only read), or null when there is none. */
    suspend fun sourceSize(): IntSize?

    /** The working copy as an [ImageBitmap], or null when there is none. */
    suspend fun loadSource(): ImageBitmap?

    /** Crops [rect] (working-copy pixels) to a square JPEG of at most 1024 px at quality 85 and stores it. */
    suspend fun cropToJpeg(rect: CropRect)

    /** The stored crop as an [ImageBitmap], or null. */
    suspend fun croppedPreview(): ImageBitmap?

    /** The stored crop's JPEG bytes (no EXIF), or null. */
    suspend fun croppedJpeg(): ByteArray?

    /** Deletes the working copy and the crop. */
    suspend fun clearWorkingFiles()
}

/** Pure constants and helpers for [PhotoProcessor]. */
object PhotoMath {
    const val MAX_SOURCE_SIDE = 2048
    const val OUTPUT_SIDE = 1024
    const val JPEG_QUALITY = 85

    /** Largest power-of-two sample size that brings the longest side to at most [maxSide] (at least 1). */
    fun inSampleSize(
        width: Int,
        height: Int,
        maxSide: Int,
    ): Int {
        val longest = maxOf(width, height)
        var sample = 1
        while (longest / sample > maxSide) sample *= 2
        return sample
    }

    /** Clockwise rotation in degrees implied by an EXIF orientation value (flips are not handled). */
    fun rotationDegrees(exifOrientation: Int): Int =
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /** Output side in pixels: the crop size capped at [OUTPUT_SIDE] (never upscales), at least 1. */
    fun outputSide(cropSize: Float): Int = minOf(OUTPUT_SIDE, cropSize.roundToInt()).coerceAtLeast(1)
}
