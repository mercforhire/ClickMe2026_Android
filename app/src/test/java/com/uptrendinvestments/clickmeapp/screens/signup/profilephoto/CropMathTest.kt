package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {
    private val w = 4000
    private val h = 3000
    private val r = 500f
    private val min = CropMath.minScale(w, h, r)
    private val max = CropMath.maxScale(min)

    @Test
    fun minScale_coversGuideAlongShortSide() {
        assertEquals(1000f / 3000f, min, 1e-6f)
    }

    @Test
    fun maxScale_isFiveTimesMin() {
        assertEquals(min * 5f, max, 1e-6f)
    }

    @Test
    fun clamp_keepsGuideInsideImage() {
        val clamped = CropMath.clamp(CropTransform(min, Offset(5000f, 5000f)), w, h, r)
        assertEquals(4000f / 6f - 500f, clamped.offset.x, 0.01f)
        assertEquals(0f, clamped.offset.y, 0.01f)
        val negative = CropMath.clamp(CropTransform(min, Offset(-5000f, -5000f)), w, h, r)
        assertEquals(-(4000f / 6f - 500f), negative.offset.x, 0.01f)
        assertEquals(0f, negative.offset.y, 0.01f)
    }

    @Test
    fun clamp_leavesOffsetInsideLimitsUnchanged() {
        val t = CropTransform(min, Offset(100f, 0f))
        assertEquals(t, CropMath.clamp(t, w, h, r))
    }

    @Test
    fun clamp_floorsScaleAtMin() {
        val clamped = CropMath.clamp(CropTransform(min / 2f, Offset.Zero), w, h, r)
        assertEquals(min, clamped.scale, 1e-6f)
    }

    @Test
    fun sourceRect_atMinScaleIsCenteredSquare() {
        val rect = CropMath.sourceRect(CropTransform(min, Offset.Zero), w, h, r)
        assertEquals(500f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(3000f, rect.size, 0.01f)
    }

    @Test
    fun sourceRect_panRightShiftsCropLeft() {
        val rect = CropMath.sourceRect(CropTransform(min, Offset(100f, 0f)), w, h, r)
        assertEquals(500f - 300f, rect.left, 0.01f)
        assertEquals(3000f, rect.size, 0.01f)
    }

    @Test
    fun applyGesture_keepsPointUnderCentroidFixed() {
        val start = CropTransform(min * 1.5f, Offset(40f, -20f))
        listOf(Offset(0f, 0f), Offset(200f, -100f), Offset(-300f, 250f)).forEach { centroid ->
            val result = CropMath.applyGesture(start, centroid, 2f, Offset.Zero, min, max)
            // The image point under the centroid: (centroid - offset) / scale must be unchanged.
            val before = (centroid - start.offset) / start.scale
            val after = (centroid - result.offset) / result.scale
            assertEquals(before.x, after.x, 0.01f)
            assertEquals(before.y, after.y, 0.01f)
            assertEquals(start.scale * 2f, result.scale, 1e-5f)
        }
    }

    @Test
    fun applyGesture_capsAtMaxAndFloorsAtMin() {
        val start = CropTransform(min * 2f, Offset.Zero)
        assertEquals(max, CropMath.applyGesture(start, Offset.Zero, 100f, Offset.Zero, min, max).scale, 1e-6f)
        assertEquals(min, CropMath.applyGesture(start, Offset.Zero, 0.001f, Offset.Zero, min, max).scale, 1e-6f)
    }

    @Test
    fun applyGesture_addsPan() {
        val start = CropTransform(min, Offset.Zero)
        val result = CropMath.applyGesture(start, Offset.Zero, 1f, Offset(10f, -5f), min, max)
        assertEquals(Offset(10f, -5f), result.offset)
    }

    @Test
    fun toggleDoubleTap_switchesBetweenMinAndTwoPointFiveTimes() {
        val zoomed = CropMath.toggleDoubleTap(CropTransform(min, Offset.Zero), min)
        assertEquals(min * 2.5f, zoomed.scale, 1e-5f)
        val back = CropMath.toggleDoubleTap(zoomed, min)
        assertEquals(min, back.scale, 1e-5f)
    }

    @Test
    fun zoomStep_inMultipliesAndCapsAtMax() {
        val up = CropMath.zoomStep(CropTransform(min, Offset(10f, 20f)), true, min, max)
        assertEquals(min * 1.1f, up.scale, 1e-5f)
        assertEquals(11f, up.offset.x, 1e-3f)
        assertEquals(22f, up.offset.y, 1e-3f)
        val capped = CropMath.zoomStep(CropTransform(max, Offset.Zero), true, min, max)
        assertEquals(max, capped.scale, 1e-6f)
    }

    @Test
    fun zoomStep_outDividesAndFloorsAtMin() {
        val down = CropMath.zoomStep(CropTransform(min * 2f, Offset.Zero), false, min, max)
        assertEquals(min * 2f / 1.1f, down.scale, 1e-5f)
        val floored = CropMath.zoomStep(CropTransform(min, Offset.Zero), false, min, max)
        assertEquals(min, floored.scale, 1e-6f)
    }

    @Test
    fun moveStep_movesOffsetByTenPercentOfGuideDiameter() {
        val right = CropMath.moveStep(CropTransform(min, Offset.Zero), 1, 0, r)
        assertEquals(Offset(-100f, 0f), right.offset)
        val down = CropMath.moveStep(CropTransform(min, Offset.Zero), 0, 1, r)
        assertEquals(Offset(0f, -100f), down.offset)
        val left = CropMath.moveStep(CropTransform(min, Offset.Zero), -1, 0, r)
        assertEquals(Offset(100f, 0f), left.offset)
    }
}
