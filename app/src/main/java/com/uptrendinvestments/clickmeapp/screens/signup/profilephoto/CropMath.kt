package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.ui.geometry.Offset

/** A square region in source-image pixels: [left], [top] and side length [size]. */
data class CropRect(
    val left: Float,
    val top: Float,
    val size: Float,
)

/**
 * The crop canvas transform. [scale] maps image pixels to canvas pixels; [offset] is the image center relative to
 * the canvas center (canvas pixels).
 */
data class CropTransform(
    val scale: Float,
    val offset: Offset,
) {
    companion object {
        /** Sentinel for "not initialised yet" (the canvas size is not known). */
        val Unset = CropTransform(0f, Offset.Zero)
    }
}

/**
 * Pure crop geometry (no Android types). Frame of reference: canvas coordinates with the origin at the canvas
 * center C; the circular guide has radius `guideRadius` canvas pixels and is centered on C; the image is centered at
 * `C + offset` and scaled by `scale`.
 */
object CropMath {
    const val MAX_ZOOM_FACTOR = 5f
    const val DOUBLE_TAP_FACTOR = 2.5f
    const val A11Y_STEP_FRACTION = 0.1f

    private const val ZOOM_STEP = 1.1f
    private const val AT_MIN_THRESHOLD = 1.5f

    /** Smallest scale at which the image fully covers the guide diameter along its shorter side. */
    fun minScale(
        imageWidth: Int,
        imageHeight: Int,
        guideRadius: Float,
    ): Float = 2f * guideRadius / minOf(imageWidth, imageHeight)

    /** Largest allowed scale: [MAX_ZOOM_FACTOR] times [minScale]. */
    fun maxScale(minScale: Float): Float = minScale * MAX_ZOOM_FACTOR

    /**
     * Applies a pinch/pan gesture. [centroidFromCenter] is the gesture centroid relative to the canvas center. Zoom is
     * applied about the centroid (the image point under it stays put) and then [pan] (canvas pixels) is added. The
     * result is not offset-clamped; the caller clamps with [clamp].
     */
    fun applyGesture(
        current: CropTransform,
        centroidFromCenter: Offset,
        zoom: Float,
        pan: Offset,
        minScale: Float,
        maxScale: Float,
    ): CropTransform {
        val newScale = (current.scale * zoom).coerceIn(minScale, maxScale)
        val z = newScale / current.scale
        val offset = centroidFromCenter - (centroidFromCenter - current.offset) * z + pan
        return CropTransform(newScale, offset)
    }

    /** Floors the scale at the minimum and limits [CropTransform.offset] so the guide stays inside the image. */
    fun clamp(
        t: CropTransform,
        imageWidth: Int,
        imageHeight: Int,
        guideRadius: Float,
    ): CropTransform {
        val scale = maxOf(t.scale, minScale(imageWidth, imageHeight, guideRadius))
        val limitX = maxOf(0f, imageWidth * scale / 2f - guideRadius)
        val limitY = maxOf(0f, imageHeight * scale / 2f - guideRadius)
        val offset = Offset(t.offset.x.coerceIn(-limitX, limitX), t.offset.y.coerceIn(-limitY, limitY))
        return CropTransform(scale, offset)
    }

    /** The square inside the guide, in source-image pixels. */
    fun sourceRect(
        t: CropTransform,
        imageWidth: Int,
        imageHeight: Int,
        guideRadius: Float,
    ): CropRect {
        val size = 2f * guideRadius / t.scale
        val left = imageWidth / 2f + (-guideRadius - t.offset.x) / t.scale
        val top = imageHeight / 2f + (-guideRadius - t.offset.y) / t.scale
        return CropRect(left, top, size)
    }

    /** Double-tap: from (near) the minimum scale zoom to [DOUBLE_TAP_FACTOR] x minimum, otherwise back to minimum. */
    fun toggleDoubleTap(
        t: CropTransform,
        minScale: Float,
    ): CropTransform {
        val target = if (t.scale < AT_MIN_THRESHOLD * minScale) minScale * DOUBLE_TAP_FACTOR else minScale
        return zoomAboutCenter(t, target)
    }

    /** Accessibility zoom step (x or / 1.1) about the canvas center; the caller clamps the offset afterwards. */
    fun zoomStep(
        t: CropTransform,
        zoomIn: Boolean,
        minScale: Float,
        maxScale: Float,
    ): CropTransform {
        val factor = if (zoomIn) ZOOM_STEP else 1f / ZOOM_STEP
        return zoomAboutCenter(t, (t.scale * factor).coerceIn(minScale, maxScale))
    }

    /**
     * Accessibility move step. [dx]/[dy] are -1, 0 or 1 in the direction of the revealed content ("Move right" shows
     * more of the right side), so the image offset moves the opposite way by [A11Y_STEP_FRACTION] of the guide
     * diameter. The caller clamps afterwards.
     */
    fun moveStep(
        t: CropTransform,
        dx: Int,
        dy: Int,
        guideRadius: Float,
    ): CropTransform {
        val step = A11Y_STEP_FRACTION * 2f * guideRadius
        return t.copy(offset = Offset(t.offset.x - dx * step, t.offset.y - dy * step))
    }

    private fun zoomAboutCenter(
        t: CropTransform,
        newScale: Float,
    ): CropTransform = CropTransform(newScale, t.offset * (newScale / t.scale))
}
