package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BUG-31: the crop screen cleared the guide circle with `BlendMode.Clear` in the same offscreen layer as the photo, so
 * Clear erased the photo as well as the scrim and the circle showed the screen background (near black) while the photo
 * showed dimmed outside it, the reverse of the intent. Inside the circle the photo must show at full brightness; outside
 * it the scrim must dim it.
 *
 * `w400dp-h900dp` is mdpi, so 1 px = 1 dp. The guide diameter there is min(400 - 32, 900 - 224) = 368, radius 184.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h900dp")
class PhotoCropCircleRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private val redPhoto =
        Bitmap
            .createBitmap(200, 100, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(0xFFFF0000.toInt()) }
            .asImageBitmap()

    private val readyState =
        PhotoCropUiState(
            isLoading = false,
            source = redPhoto,
            sourceSize = IntSize(200, 100),
            transform = CropTransform(2f, Offset.Zero),
            guideRadius = 184f,
        )

    private fun show() {
        rule.setContent {
            ClickMeTheme {
                PhotoCropContent(
                    state = readyState,
                    onGuideMeasured = {},
                    onGesture = { _, _, _ -> },
                    onGestureEnd = {},
                    onDoubleTap = {},
                    onZoomIn = {},
                    onZoomOut = {},
                    onMove = { _, _ -> },
                    onUse = {},
                    onCancel = {},
                    onDismissDialog = {},
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun thePhotoShowsAtFullBrightnessInsideTheCircleAndDimmedOutsideIt() {
        show()
        val image = rule.onNodeWithTag(CROP_CANVAS_TAG).captureToImage().toPixelMap()
        val cx = image.width / 2
        val cy = image.height / 2

        val inside = image[cx + 100, cy]
        assertEquals("inside the circle the photo is not scrimmed: $inside", 1f, inside.red, 0.02f)
        assertEquals("inside the circle the photo is not erased: $inside", 0f, inside.green, 0.02f)

        // 190 px right of the center: outside the 184 px circle, still on the 400 px wide photo.
        val outside = image[cx + 190, cy]
        assertTrue("outside the circle the photo is dimmed by the scrim: $outside", outside.red in 0.2f..0.7f)
        assertTrue("outside the circle the photo still shows through: $outside", outside.red > outside.green + 0.2f)
    }
}
