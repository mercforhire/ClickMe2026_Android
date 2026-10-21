package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoProcessorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var processor: AndroidPhotoProcessor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        processor = AndroidPhotoProcessor(context, Dispatchers.IO)
    }

    private fun jpegUri(
        width: Int,
        height: Int,
        name: String = "src.jpg",
    ): Uri {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(200, 80, 40))
        val file = folder.newFile(name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun inSampleSize_isPowerOfTwoBoundedByMaxSide() {
        assertEquals(2, PhotoMath.inSampleSize(4000, 3000, 2048))
        assertEquals(4, PhotoMath.inSampleSize(8000, 6000, 2048))
        assertEquals(1, PhotoMath.inSampleSize(1000, 800, 2048))
    }

    @Test
    fun rotationDegrees_mapsExifOrientation() {
        assertEquals(90, PhotoMath.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, PhotoMath.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, PhotoMath.rotationDegrees(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0, PhotoMath.rotationDegrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(0, PhotoMath.rotationDegrees(ExifInterface.ORIENTATION_UNDEFINED))
    }

    @Test
    fun outputSide_capsAtOutputSideAndNeverUpscales() {
        assertEquals(1024, PhotoMath.outputSide(3000f))
        assertEquals(600, PhotoMath.outputSide(600.4f))
    }

    @Test
    fun importPicked_writesBoundedWorkingCopy() =
        runBlocking {
            processor.importPicked(jpegUri(3000, 2000))
            assertTrue(processor.hasSource())
            val size = processor.sourceSize()
            assertNotNull(size)
            assertTrue(maxOf(size!!.width, size.height) <= PhotoMath.MAX_SOURCE_SIDE)
            assertNotNull(processor.loadSource())
        }

    @Test
    fun importPicked_appliesExifRotation() =
        runBlocking {
            val uri = jpegUri(300, 200)
            val exif = ExifInterface(uri.path!!)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            exif.saveAttributes()
            processor.importPicked(uri)
            assertEquals(IntSize(200, 300), processor.sourceSize())
        }

    @Test
    fun cropToJpeg_writesSquare1024Jpeg() =
        runBlocking {
            // 4000 x 3000 is sampled by 2 to a 2000 x 1500 working copy; the 1365 px crop fits inside it.
            processor.importPicked(jpegUri(4000, 3000))
            processor.cropToJpeg(CropRect(500f, 0f, 1365f))
            val bytes = processor.croppedJpeg()
            assertNotNull(bytes)
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size)
            assertEquals(1024, decoded.width)
            assertEquals(1024, decoded.height)
            assertNotNull(processor.croppedPreview())
        }

    @Test
    fun cropToJpeg_doesNotUpscaleSmallCrops() =
        runBlocking {
            processor.importPicked(jpegUri(1000, 800))
            processor.cropToJpeg(CropRect(0f, 0f, 600f))
            val bytes = processor.croppedJpeg()!!
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(600, decoded.width)
            assertEquals(600, decoded.height)
        }

    @Test
    fun importPicked_nonImageThrowsPhotoImportException() =
        runBlocking {
            val file = folder.newFile("note.txt")
            file.writeText("this is not an image")
            try {
                processor.importPicked(Uri.fromFile(file))
                fail("expected PhotoImportException")
            } catch (_: PhotoImportException) {
                // expected
            }
            assertFalse(processor.hasSource())
        }

    @Test
    fun importPicked_missingFileThrowsPhotoImportException() =
        runBlocking {
            try {
                processor.importPicked(Uri.fromFile(File(folder.root, "missing.jpg")))
                fail("expected PhotoImportException")
            } catch (_: PhotoImportException) {
                // expected
            }
        }

    @Test
    fun clearWorkingFiles_removesBothFiles() =
        runBlocking {
            processor.importPicked(jpegUri(1000, 800))
            processor.cropToJpeg(CropRect(0f, 0f, 600f))
            processor.clearWorkingFiles()
            assertFalse(processor.hasSource())
            assertNull(processor.croppedJpeg())
        }

    @Test
    fun importPicked_deletesPreviousCrop() =
        runBlocking {
            processor.importPicked(jpegUri(1000, 800, "a.jpg"))
            processor.cropToJpeg(CropRect(0f, 0f, 600f))
            assertNotNull(processor.croppedJpeg())
            processor.importPicked(jpegUri(1000, 800, "b.jpg"))
            assertNull(processor.croppedJpeg())
            assertTrue(processor.hasSource())
        }
}
