@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignupProfilePhotoViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val avatarUrl = "https://cdn.example/avatar.jpg"

    private class Setup(
        val h: SignupTestHarness,
        val vm: SignupProfilePhotoViewModel,
    )

    private fun setup(scope: TestScope): Setup {
        val h = SignupTestHarness(scope, tmp, main.testDispatcher)
        return Setup(h, SignupProfilePhotoViewModel(h.repository, h.photo))
    }

    /** Hydrates a saved avatar into the draft. */
    private suspend fun Setup.withSavedAvatar() {
        h.api.expertProfileHandler = { h.fullProfile(avatarUrl = avatarUrl) }
        h.api.emailStatusHandler = { EmailStatusData(verified = false) }
        h.repository.hydrate()
    }

    private fun Setup.withLocalCrop() {
        h.photo.preview = ImageBitmap(1, 1)
        h.photo.cropped = byteArrayOf(1, 2, 3)
        h.photo.hasSourceValue = true
    }

    @Test
    fun avatarUrlFollowsTheDraft() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            assertNull(s.vm.uiState.value.avatarUrl)

            s.withSavedAvatar()
            advanceUntilIdle()

            assertEquals(avatarUrl, s.vm.uiState.value.avatarUrl)
            assertTrue(s.vm.uiState.value.hasAnyPhoto)
            assertFalse(s.vm.uiState.value.hasUnsavedPhoto)
        }

    @Test
    fun onPickedImportsOnceAndOpensTheCrop() =
        runTest(main.testDispatcher) {
            val s = setup(this)

            s.vm.onPicked(Uri.parse("content://media/1"))
            advanceUntilIdle()

            assertEquals(1, s.h.photo.importCalls)
            assertTrue(s.vm.openCrop.value)
            assertFalse(s.vm.uiState.value.isImporting)
            assertTrue(s.vm.uiState.value.hasSource)
            s.vm.onOpenCropConsumed()
            assertFalse(s.vm.openCrop.value)
        }

    @Test
    fun importFailureShowsTheOpenErrorAndDoesNotOpenTheCrop() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.h.photo.importError = PhotoImportException("bad image")

            s.vm.onPicked(Uri.parse("content://media/1"))
            advanceUntilIdle()

            assertFalse(s.vm.openCrop.value)
            assertFalse(s.vm.uiState.value.isImporting)
            assertEquals(FieldError.Res(R.string.signup_photo_open_error_body), s.vm.uiState.value.dialogError)
            assertEquals(R.string.signup_photo_open_error_title, s.vm.uiState.value.dialogTitleRes)
            s.vm.dismissDialog()
            assertNull(s.vm.uiState.value.dialogError)
            assertNull(s.vm.uiState.value.dialogTitleRes)
        }

    @Test
    fun onShownLoadsTheCroppedPreviewWhenOneExists() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.withLocalCrop()

            s.vm.onShown()
            advanceUntilIdle()

            assertNotNull(s.vm.uiState.value.localPreview)
            assertTrue(s.vm.uiState.value.hasUnsavedPhoto)
            assertTrue(s.vm.uiState.value.hasSource)
        }

    @Test
    fun onShownWithNothingStoredLeavesNoPreview() =
        runTest(main.testDispatcher) {
            val s = setup(this)

            s.vm.onShown()
            advanceUntilIdle()

            assertNull(s.vm.uiState.value.localPreview)
            assertFalse(s.vm.uiState.value.hasUnsavedPhoto)
            assertFalse(s.vm.uiState.value.hasAnyPhoto)
        }

    @Test
    fun saveWithNoLocalCropUploadsNothingAndReturnsAfterTheHold() =
        runTest(main.testDispatcher) {
            val s = setup(this)

            s.vm.save()
            runCurrent()

            assertEquals(SaveState.Saved, s.vm.uiState.value.saveState)
            assertFalse(s.vm.saved.value)
            advanceTimeBy(599)
            assertFalse(s.vm.saved.value)
            advanceTimeBy(2)
            runCurrent()
            assertTrue(s.vm.saved.value)
            assertEquals(0, s.h.api.uploadAvatarCalls)
            s.vm.onSavedConsumed()
            assertFalse(s.vm.saved.value)
        }

    @Test
    fun saveWithALocalCropUploadsTheJpegAsTheImagePart() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            var captured: MultipartBody.Part? = null
            s.h.api.uploadAvatarHandler = {
                captured = it
                AvatarUploadData("https://cdn.example/new.jpg")
            }
            s.withLocalCrop()
            s.vm.onShown()
            advanceUntilIdle()

            s.vm.save()
            runCurrent()

            assertEquals(1, s.h.api.uploadAvatarCalls)
            assertEquals("image/jpeg", captured?.body?.contentType().toString())
            assertTrue(captured?.headers?.get("Content-Disposition")?.contains("name=\"image\"") == true)
            assertEquals("https://cdn.example/new.jpg", s.vm.uiState.value.avatarUrl)
            assertEquals(1, s.h.photo.clearCalls)
            assertNull(s.vm.uiState.value.localPreview)
            assertEquals(SaveState.Saved, s.vm.uiState.value.saveState)
            advanceTimeBy(601)
            runCurrent()
            assertTrue(s.vm.saved.value)
        }

    @Test
    fun uploadFailureKeepsTheLocalCropForARetry() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.h.api.uploadAvatarHandler = { throw IOException("offline") }
            s.withLocalCrop()
            s.vm.onShown()
            advanceUntilIdle()

            s.vm.save()
            runCurrent()

            val state = s.vm.uiState.value
            assertEquals(SaveState.Idle, state.saveState)
            assertEquals(R.string.signup_photo_upload_error_title, state.dialogTitleRes)
            assertNotNull(state.dialogError)
            assertNotNull(state.localPreview)
            assertEquals(0, s.h.photo.clearCalls)
            assertFalse(s.vm.saved.value)

            s.h.api.uploadAvatarHandler = { AvatarUploadData("https://cdn.example/retry.jpg") }
            s.vm.save()
            runCurrent()
            assertEquals(2, s.h.api.uploadAvatarCalls)
            assertNull(s.vm.uiState.value.dialogTitleRes)
        }

    @Test
    fun aSecondSaveWhileSavingMakesNoSecondCall() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            val gate = CompletableDeferred<AvatarUploadData>()
            s.h.api.uploadAvatarHandler = { gate.await() }
            s.withLocalCrop()
            s.vm.onShown()
            advanceUntilIdle()

            s.vm.save()
            runCurrent()
            s.vm.save()
            runCurrent()

            assertEquals(1, s.h.api.uploadAvatarCalls)
            assertEquals(SaveState.Saving, s.vm.uiState.value.saveState)
            gate.complete(AvatarUploadData("u"))
            advanceUntilIdle()
        }

    @Test
    fun removingAnUnsavedLocalPickClearsItWithNoDialogAndNoServerCall() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.withLocalCrop()
            s.vm.onShown()
            advanceUntilIdle()

            s.vm.requestRemove()
            advanceUntilIdle()

            assertFalse(s.vm.uiState.value.showRemoveConfirm)
            assertNull(s.vm.uiState.value.localPreview)
            assertEquals(1, s.h.photo.clearCalls)
            assertEquals(0, s.h.api.deleteAvatarCalls)
        }

    @Test
    fun removingASavedAvatarAsksThenDeletesAndClearsTheUrl() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.h.api.deleteAvatarHandler = { SuccessMessageResponse() }
            s.withSavedAvatar()
            advanceUntilIdle()

            s.vm.requestRemove()
            assertTrue(s.vm.uiState.value.showRemoveConfirm)
            assertEquals(0, s.h.api.deleteAvatarCalls)

            s.vm.confirmRemove()
            advanceUntilIdle()

            assertEquals(1, s.h.api.deleteAvatarCalls)
            assertFalse(s.vm.uiState.value.showRemoveConfirm)
            assertNull(s.vm.uiState.value.avatarUrl)
        }

    @Test
    fun dismissingTheRemoveConfirmationMakesNoCall() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.withSavedAvatar()
            advanceUntilIdle()

            s.vm.requestRemove()
            s.vm.dismissRemove()
            s.vm.confirmRemove()
            advanceUntilIdle()

            assertFalse(s.vm.uiState.value.showRemoveConfirm)
            assertEquals(0, s.h.api.deleteAvatarCalls)
            assertEquals(avatarUrl, s.vm.uiState.value.avatarUrl)
        }

    @Test
    fun aFailedDeleteShowsTheDefaultDialogAndKeepsTheAvatar() =
        runTest(main.testDispatcher) {
            val s = setup(this)
            s.h.api.deleteAvatarHandler = { throw IOException("offline") }
            s.withSavedAvatar()
            advanceUntilIdle()

            s.vm.requestRemove()
            s.vm.confirmRemove()
            advanceUntilIdle()

            assertNotNull(s.vm.uiState.value.dialogError)
            assertNull(s.vm.uiState.value.dialogTitleRes)
            assertEquals(avatarUrl, s.vm.uiState.value.avatarUrl)
        }
}
