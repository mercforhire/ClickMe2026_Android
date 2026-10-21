package com.uptrendinvestments.clickmeapp.testing

import android.net.Uri
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.CropRect
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoImportException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Pins the fake to the real processor's file contract (WR-08), so photo tests cannot pass on impossible states. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FakePhotoProcessorTest {
    private val uri = Uri.parse("content://media/1")
    private val rect = CropRect(left = 0f, top = 0f, size = 10f)

    @Test
    fun aStoredCropAlwaysHasAPreviewAndBytes() =
        runTest {
            val fake = FakePhotoProcessor()
            fake.importPicked(uri)

            fake.cropToJpeg(rect)

            assertNotNull(fake.croppedPreview())
            assertArrayEquals(byteArrayOf(1, 2, 3), fake.croppedJpeg())
            assertTrue(fake.hasSource())
        }

    @Test
    fun aNewImportDeletesTheOldCropAndItsPreview() =
        runTest {
            val fake = FakePhotoProcessor()
            fake.givenCrop()

            fake.importPicked(uri)

            assertNull(fake.croppedPreview())
            assertNull(fake.croppedJpeg())
            assertTrue(fake.hasSource())
        }

    @Test
    fun cropWithoutAWorkingCopyFails() =
        runTest {
            val fake = FakePhotoProcessor()
            try {
                fake.cropToJpeg(rect)
                fail("expected PhotoImportException")
            } catch (e: PhotoImportException) {
                // expected, as AndroidPhotoProcessor
            }
            assertNull(fake.croppedJpeg())
        }

    @Test
    fun clearDeletesTheFilesButKeepsTheScriptedFailures() =
        runTest {
            val fake = FakePhotoProcessor()
            fake.givenCrop()
            fake.importError = PhotoImportException("bad image")
            fake.cropError = PhotoImportException("disk full")

            fake.clearWorkingFiles()

            assertFalse(fake.hasSource())
            assertNull(fake.croppedPreview())
            assertNull(fake.croppedJpeg())
            assertEquals(1, fake.clearCalls)
            try {
                fake.importPicked(uri)
                fail("expected the scripted import failure")
            } catch (e: PhotoImportException) {
                assertEquals("bad image", e.message)
            }
            fake.givenSource()
            try {
                fake.cropToJpeg(rect)
                fail("expected the scripted crop failure")
            } catch (e: PhotoImportException) {
                assertEquals("disk full", e.message)
            }
        }
}
