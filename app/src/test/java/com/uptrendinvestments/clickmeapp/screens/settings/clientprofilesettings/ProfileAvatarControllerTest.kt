@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import android.net.Uri
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoImportException
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileAvatarControllerTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val seeded = userProfileData()
    private val tooBig =
        ApiException.Http(
            status = 500,
            code = null,
            rawCode = null,
            serverMessage = "Too big",
            fieldErrors = emptyList(),
            detail = null,
        )

    private class Setup(
        val h: SignupTestHarness,
        val controller: ProfileAvatarController,
        val viewScope: CoroutineScope,
        val viewJob: Job,
    ) {
        var profileCalls = 0
    }

    private suspend fun TestScope.setup(): Setup {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        val viewJob = Job()
        val setup =
            Setup(
                h = h,
                controller =
                    ProfileAvatarController(h.photo, ProfileSettingsRepository(h.api), h.userManager, backgroundScope),
                viewScope = CoroutineScope(viewJob + StandardTestDispatcher(testScheduler)),
                viewJob = viewJob,
            )
        h.api.userProfileHandler = { seeded }
        h.signIn()
        runCurrent()
        h.userManager.refreshProfile()
        h.api.userProfileHandler = {
            setup.profileCalls++
            seeded
        }
        return setup
    }

    private val uri: Uri = Uri.parse("content://picker/1")

    /** Drives a pick through to the crop route opening, then marks the crop screen as opened. */
    private fun Setup.pickAndOpenCrop(scope: TestScope) {
        controller.onPicked(uri, viewScope)
        scope.runCurrent()
        controller.onCropOpened()
    }

    @Test
    fun pickCropUploadRefresh() =
        runTest(main.testDispatcher) {
            val s = setup()
            var uploaded: MultipartBody.Part? = null
            s.h.api.uploadAvatarHandler = {
                uploaded = it
                AvatarUploadData("https://cdn.example/new.jpg")
            }

            s.controller.onPicked(uri, s.viewScope)
            runCurrent()
            assertEquals(1, s.h.photo.importCalls)
            assertTrue(s.controller.state.value.openCrop)

            s.controller.onCropOpened()
            assertFalse(s.controller.state.value.openCrop)
            s.h.photo.givenCrop(byteArrayOf(9, 8, 7))
            s.controller.onReturnedFromCrop()
            runCurrent()

            assertEquals(1, s.h.api.uploadAvatarCalls)
            assertNotNull(uploaded)
            assertEquals(1, s.h.photo.clearCalls)
            assertNull(s.h.photo.storedCrop)
            assertEquals(1, s.profileCalls)
            assertFalse(s.controller.state.value.isUploading)
            assertNull(s.controller.state.value.error)
        }

    @Test
    fun returningAgainDoesNotUploadTwice() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.h.api.uploadAvatarHandler = { AvatarUploadData("https://cdn.example/new.jpg") }
            s.pickAndOpenCrop(this)
            s.h.photo.givenCrop()

            s.controller.onReturnedFromCrop()
            s.controller.onReturnedFromCrop()
            runCurrent()
            s.controller.onReturnedFromCrop()
            runCurrent()

            assertEquals(1, s.h.api.uploadAvatarCalls)
        }

    @Test
    fun cancelledCropUploadsNothing() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.pickAndOpenCrop(this)
            assertTrue(s.h.photo.hasSourceFile)

            s.controller.onReturnedFromCrop()
            runCurrent()

            assertEquals(0, s.h.api.uploadAvatarCalls)
            assertEquals(1, s.h.photo.clearCalls)
            assertFalse(s.h.photo.hasSourceFile)
            assertEquals(0, s.profileCalls)
            assertFalse(s.controller.state.value.isUploading)
        }

    @Test
    fun returnWithoutOpeningTheCropDoesNothing() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.h.photo.givenCrop()

            s.controller.onReturnedFromCrop()
            runCurrent()

            assertEquals(0, s.h.api.uploadAvatarCalls)
            assertEquals(0, s.h.photo.clearCalls)
        }

    @Test
    fun uploadFailureShowsDialogKeepsAvatar() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.h.api.uploadAvatarHandler = { throw tooBig }
            s.pickAndOpenCrop(this)
            s.h.photo.givenCrop()
            val before: UserProfileData? = s.h.userManager.profile.value

            s.controller.onReturnedFromCrop()
            runCurrent()

            assertEquals(FieldError.Text("Too big"), s.controller.state.value.error)
            assertNull(s.controller.state.value.errorTitleRes)
            assertFalse(s.controller.state.value.isUploading)
            assertSame(before, s.h.userManager.profile.value)
            assertEquals(0, s.profileCalls)
            assertFalse(s.h.photo.hasSourceFile)

            s.controller.dismissError()
            assertNull(s.controller.state.value.error)
        }

    @Test
    fun workingFileFailureDuringUploadShowsDialogInsteadOfCrashing() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.pickAndOpenCrop(this)
            s.h.photo.givenCrop()
            s.h.photo.previewError = java.io.IOException("disk")

            s.controller.onReturnedFromCrop()
            runCurrent()

            val state = s.controller.state.value
            assertEquals(FieldError.Res(R.string.signup_photo_open_error_body), state.error)
            assertEquals(R.string.signup_photo_open_error_title, state.errorTitleRes)
            assertFalse(state.isUploading)
            assertEquals(0, s.h.api.uploadAvatarCalls)
            assertEquals(1, s.h.photo.clearCalls)
        }

    @Test
    fun uploadFailureWithoutServerMessageUsesConnectionCopy() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.h.api.uploadAvatarHandler = {
                throw ApiException.Http(500, null, null, null, emptyList(), null)
            }
            s.pickAndOpenCrop(this)
            s.h.photo.givenCrop()

            s.controller.onReturnedFromCrop()
            runCurrent()

            assertEquals(FieldError.Res(R.string.login_error_network), s.controller.state.value.error)
        }

    @Test
    fun uploadingShowsThePreviewAndBlocksANewPick() =
        runTest(main.testDispatcher) {
            val s = setup()
            val gate = CompletableDeferred<AvatarUploadData>()
            s.h.api.uploadAvatarHandler = { gate.await() }
            s.pickAndOpenCrop(this)
            s.h.photo.givenCrop()

            s.controller.onReturnedFromCrop()
            runCurrent()
            assertTrue(s.controller.state.value.isUploading)
            assertNotNull(s.controller.state.value.preview)

            s.controller.onPicked(uri, s.viewScope)
            runCurrent()
            assertEquals(1, s.h.photo.importCalls)

            gate.complete(AvatarUploadData("https://cdn.example/new.jpg"))
            runCurrent()
            assertFalse(s.controller.state.value.isUploading)
            assertNull(s.controller.state.value.preview)
        }

    @Test
    fun uploadSurvivesLeaving() =
        runTest(main.testDispatcher) {
            val s = setup()
            val gate = CompletableDeferred<AvatarUploadData>()
            s.h.api.uploadAvatarHandler = { gate.await() }
            s.pickAndOpenCrop(this)
            s.h.photo.givenCrop()
            s.controller.onReturnedFromCrop()
            runCurrent()
            assertTrue(s.controller.state.value.isUploading)

            s.viewJob.cancel()
            runCurrent()
            gate.complete(AvatarUploadData("https://cdn.example/new.jpg"))
            runCurrent()

            assertFalse(s.controller.state.value.isUploading)
            assertEquals(1, s.profileCalls)
            assertEquals(1, s.h.photo.clearCalls)
            assertNull(s.controller.state.value.error)
        }

    @Test
    fun importFailureShowsDialogAndNeverOpensTheCrop() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.h.photo.importError = PhotoImportException("bad image")

            s.controller.onPicked(uri, s.viewScope)
            runCurrent()

            val state = s.controller.state.value
            assertEquals(FieldError.Res(R.string.signup_photo_open_error_body), state.error)
            assertEquals(R.string.signup_photo_open_error_title, state.errorTitleRes)
            assertFalse(state.openCrop)
            assertFalse(state.isImporting)
        }

    @Test
    fun aPickStoresNothingUntilTheCropIsConfirmed() =
        runTest(main.testDispatcher) {
            val s = setup()

            s.controller.onPicked(uri, s.viewScope)
            runCurrent()

            assertEquals(0, s.h.api.uploadAvatarCalls)
            assertNull(s.h.photo.storedCrop)
        }
}
