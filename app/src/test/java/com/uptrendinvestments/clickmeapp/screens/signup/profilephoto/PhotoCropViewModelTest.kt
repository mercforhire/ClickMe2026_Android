package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakePhotoProcessor
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoCropViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val fake =
        FakePhotoProcessor().apply { givenSource(ImageBitmap(400, 300), IntSize(400, 300)) }

    private val delta = 0.01f

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = PhotoCropViewModel(fake, handle)

    private val minAt50 = 100f / 300f

    @Test
    fun loadsSourceOnCreation() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            runCurrent()
            assertFalse(vm.uiState.value.isLoading)
            assertNotNull(vm.uiState.value.source)
            assertFalse(vm.uiState.value.loadFailed)
        }

    @Test
    fun missingSourceFailsLoadWithOpenErrorBody() =
        runTest(dispatcherRule.testDispatcher) {
            // No working copy on disk (for example cleared by a logout).
            fake.clearWorkingFiles()
            val vm = viewModel()
            runCurrent()
            assertTrue(vm.uiState.value.loadFailed)
            assertEquals(FieldError.Res(R.string.signup_photo_open_error_body), vm.uiState.value.dialogError)
        }

    @Test
    fun guideMeasurementSetsMinimumTransform() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            runCurrent()
            vm.onGuideMeasured(50f)
            assertEquals(50f, vm.uiState.value.guideRadius, 0f)
            assertEquals(CropTransform(minAt50, Offset.Zero), vm.uiState.value.transform)
        }

    @Test
    fun guideMeasuredBeforeLoadStillInitialisesTransform() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            vm.onGuideMeasured(50f)
            runCurrent()
            assertEquals(CropTransform(minAt50, Offset.Zero), vm.uiState.value.transform)
        }

    @Test
    fun gestureZoomIsCappedAndPanIsRawUntilGestureEnd() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            runCurrent()
            vm.onGuideMeasured(50f)

            vm.onGesture(Offset.Zero, 10f, Offset.Zero)
            assertEquals(minAt50 * CropMath.MAX_ZOOM_FACTOR, vm.uiState.value.transform.scale, 0.0001f)

            vm.onGesture(Offset.Zero, 1f, Offset(500f, 0f))
            assertEquals(500f, vm.uiState.value.transform.offset.x, delta)

            vm.onGestureEnd()
            val limitX = 400 * vm.uiState.value.transform.scale / 2f - 50f
            assertEquals(limitX, vm.uiState.value.transform.offset.x, delta)
        }

    @Test
    fun doubleTapZoomInOutAndMoveAreClamped() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            runCurrent()
            vm.onGuideMeasured(50f)

            vm.onDoubleTap()
            assertEquals(minAt50 * CropMath.DOUBLE_TAP_FACTOR, vm.uiState.value.transform.scale, 0.0001f)
            vm.onDoubleTap()
            assertEquals(minAt50, vm.uiState.value.transform.scale, 0.0001f)

            vm.zoomIn()
            assertEquals(minAt50 * 1.1f, vm.uiState.value.transform.scale, 0.0001f)
            vm.zoomOut()
            assertEquals(minAt50, vm.uiState.value.transform.scale, 0.0001f)

            vm.zoomIn()
            vm.move(1, 0)
            val scale = vm.uiState.value.transform.scale
            val limitX = 400 * scale / 2f - 50f
            assertTrue(vm.uiState.value.transform.offset.x <= 0f)
            assertTrue(vm.uiState.value.transform.offset.x >= -limitX - delta)
            repeat(20) { vm.move(1, 0) }
            assertEquals(-limitX, vm.uiState.value.transform.offset.x, delta)
        }

    @Test
    fun useCropAtMinimumCallsProcessorOnceAndFinishes() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            runCurrent()
            vm.onGuideMeasured(50f)

            vm.useCrop()
            vm.useCrop()
            assertTrue(vm.uiState.value.isCropping)
            runCurrent()

            assertEquals(1, fake.cropCalls)
            val rect = fake.lastCropRect!!
            assertEquals(50f, rect.left, delta)
            assertEquals(0f, rect.top, delta)
            assertEquals(300f, rect.size, delta)
            assertTrue(vm.done.value)
            assertFalse(vm.uiState.value.isCropping)

            vm.onDoneConsumed()
            assertFalse(vm.done.value)
        }

    @Test
    fun useCropBeforeGuideMeasuredDoesNothing() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            runCurrent()
            vm.useCrop()
            runCurrent()
            assertEquals(0, fake.cropCalls)
            assertFalse(vm.done.value)
        }

    @Test
    fun cropFailureShowsUnknownErrorAndStopsCropping() =
        runTest(dispatcherRule.testDispatcher) {
            fake.cropError = java.io.IOException("disk full")
            val vm = viewModel()
            runCurrent()
            vm.onGuideMeasured(50f)

            vm.useCrop()
            runCurrent()

            assertEquals(FieldError.Res(R.string.login_error_unknown), vm.uiState.value.dialogError)
            assertFalse(vm.uiState.value.isCropping)
            assertFalse(vm.done.value)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun savedStateRestoresFramingRelativeToNewGuideSize() =
        runTest(dispatcherRule.testDispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle)
            runCurrent()
            first.onGuideMeasured(50f)
            first.zoomIn()
            first.move(1, 0)
            val before = first.uiState.value.transform

            val second = viewModel(handle)
            runCurrent()
            second.onGuideMeasured(100f)
            val after = second.uiState.value.transform

            val minAt100 = 200f / 300f
            assertEquals(1.1f, after.scale / minAt100, 0.0001f)
            assertEquals(before.offset.x / 50f, after.offset.x / 100f, 0.0001f)
            assertEquals(before.offset.y / 50f, after.offset.y / 100f, 0.0001f)
        }
}
