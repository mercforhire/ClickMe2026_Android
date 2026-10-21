package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * [PhotoProcessor] over [BitmapFactory] and the platform [ExifInterface], with working files in the app-private
 * `cacheDir/signup`. Decoding is bounds-first with a power-of-two sample size so memory stays bounded for any input.
 */
@Singleton
class AndroidPhotoProcessor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : PhotoProcessor {
        private val mutex = Mutex()

        private val dir: File get() = File(context.cacheDir, DIR_NAME)
        private val sourceFile: File get() = File(dir, SOURCE_NAME)
        private val cropFile: File get() = File(dir, CROP_NAME)

        override suspend fun importPicked(uri: Uri): Unit =
            locked {
                try {
                    val resolver = context.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver
                        .openInputStream(
                            uri,
                        ).requireStream(uri)
                        .use { BitmapFactory.decodeStream(it, null, bounds) }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        throw PhotoImportException("Not a readable image: $uri")
                    }
                    val orientation =
                        try {
                            resolver.openInputStream(uri).requireStream(uri).use {
                                ExifInterface(it).getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL,
                                )
                            }
                        } catch (_: IOException) {
                            ExifInterface.ORIENTATION_NORMAL
                        } catch (_: RuntimeException) {
                            ExifInterface.ORIENTATION_NORMAL
                        }
                    val options =
                        BitmapFactory.Options().apply {
                            inSampleSize =
                                PhotoMath.inSampleSize(bounds.outWidth, bounds.outHeight, PhotoMath.MAX_SOURCE_SIDE)
                        }
                    val decoded =
                        resolver
                            .openInputStream(
                                uri,
                            ).requireStream(uri)
                            .use { BitmapFactory.decodeStream(it, null, options) }
                            ?: throw PhotoImportException("Could not decode image: $uri")
                    val prepared = rotateAndBound(decoded, PhotoMath.rotationDegrees(orientation))
                    cropFile.delete()
                    dir.mkdirs()
                    FileOutputStream(
                        sourceFile,
                    ).use { prepared.compress(Bitmap.CompressFormat.JPEG, SOURCE_QUALITY, it) }
                    prepared.recycle()
                } catch (e: PhotoImportException) {
                    throw e
                } catch (e: IOException) {
                    throw PhotoImportException("Could not import image", e)
                } catch (e: SecurityException) {
                    throw PhotoImportException("No permission to read image", e)
                } catch (e: OutOfMemoryError) {
                    throw PhotoImportException("Image too large", e)
                }
            }

        override suspend fun hasSource(): Boolean = locked { sourceFile.isFile }

        override suspend fun sourceSize(): IntSize? =
            locked {
                if (!sourceFile.isFile) return@locked null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(sourceFile.path, bounds)
                if (bounds.outWidth > 0 && bounds.outHeight > 0) IntSize(bounds.outWidth, bounds.outHeight) else null
            }

        override suspend fun loadSource(): ImageBitmap? = locked { decodeFile(sourceFile) }

        override suspend fun cropToJpeg(rect: CropRect): Unit =
            locked {
                val src = decodeBitmap(sourceFile) ?: throw PhotoImportException("No source image")
                try {
                    val srcRect = clampedRect(rect, src.width, src.height)
                    val side = PhotoMath.outputSide(rect.size)
                    val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                    try {
                        Canvas(out).drawBitmap(src, srcRect, Rect(0, 0, side, side), Paint(Paint.FILTER_BITMAP_FLAG))
                        dir.mkdirs()
                        FileOutputStream(
                            cropFile,
                        ).use { out.compress(Bitmap.CompressFormat.JPEG, PhotoMath.JPEG_QUALITY, it) }
                    } finally {
                        out.recycle()
                    }
                } catch (e: IOException) {
                    throw PhotoImportException("Could not write cropped image", e)
                } finally {
                    src.recycle()
                }
            }

        override suspend fun croppedPreview(): ImageBitmap? = locked { decodeFile(cropFile) }

        override suspend fun croppedJpeg(): ByteArray? = locked { if (cropFile.isFile) cropFile.readBytes() else null }

        override suspend fun clearWorkingFiles(): Unit =
            locked {
                dir.listFiles()?.forEach { it.delete() }
            }

        private suspend fun <T> locked(block: () -> T): T = withContext(io) { mutex.withLock { block() } }

        private fun InputStream?.requireStream(uri: Uri): InputStream =
            this ?: throw PhotoImportException("Could not open $uri")

        private fun decodeBitmap(file: File): Bitmap? = if (file.isFile) BitmapFactory.decodeFile(file.path) else null

        private fun decodeFile(file: File): ImageBitmap? = decodeBitmap(file)?.asImageBitmap()

        private fun rotateAndBound(
            decoded: Bitmap,
            degrees: Int,
        ): Bitmap {
            val longest = maxOf(decoded.width, decoded.height)
            val matrix = Matrix()
            if (degrees != 0) matrix.postRotate(degrees.toFloat())
            if (longest > PhotoMath.MAX_SOURCE_SIDE) {
                val factor = PhotoMath.MAX_SOURCE_SIDE.toFloat() / longest
                matrix.postScale(factor, factor)
            }
            if (matrix.isIdentity) return decoded
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (result !== decoded) decoded.recycle()
            return result
        }

        /** A square integer rect inside the bitmap: the side is capped to the bitmap and the rect is shifted to fit. */
        private fun clampedRect(
            rect: CropRect,
            width: Int,
            height: Int,
        ): Rect {
            val side = rect.size.roundToInt().coerceIn(1, minOf(width, height))
            val left = rect.left.roundToInt().coerceIn(0, width - side)
            val top = rect.top.roundToInt().coerceIn(0, height - side)
            return Rect(left, top, left + side, top + side)
        }

        private companion object {
            const val DIR_NAME = "signup"
            const val SOURCE_NAME = "photo_src.jpg"
            const val CROP_NAME = "photo_crop.jpg"
            const val SOURCE_QUALITY = 92
        }
    }
