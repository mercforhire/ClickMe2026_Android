package com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Test tag on the empty-state illustration. */
const val SEARCH_EMPTY_ILLUSTRATION_TAG = "search_empty_illustration"

private const val GLOW_MIN_ALPHA = 0.10f
private const val GLOW_MAX_ALPHA = 0.30f
private const val GLOW_DURATION_MS = 2300
private const val GLOW_RADIUS_DP = 140f
private const val HANDLE_OFFSET = 0.707f
private const val LENS_RADIUS = 52f
private const val MAIN_STROKE = 2.5f
private const val UNDERGLOW_EXTRA = 4f
private const val UNDERGLOW_ALPHA = 0.25f
private const val GRAPH_ALPHA = 0.80f
private const val SPOKE_ALPHA = 0.45f
private const val LINK_ALPHA = 0.25f
private const val SCATTER_ALPHA = 0.60f

/** Graph polyline inside the lens, as offsets from the lens center in dp. */
private val GraphPoints = listOf(-28f to -6f, -8f to 10f, 14f to -14f, 30f to 6f)

/** Network nodes around the lens: x and y offsets from the canvas center in dp, and the dot radius. */
private val NetworkNodes =
    listOf(
        Triple(-80f, -60f, 5f),
        Triple(30f, -85f, 4f),
        Triple(95f, -30f, 4f),
        Triple(-95f, 30f, 3f),
        Triple(0f, 90f, 3f),
    )

private val ScatterDots = listOf(-55f to -90f, 60f to -65f, 110f to 20f, 50f to 100f, -30f to 110f)

/**
 * Port of iOS `SearchExpertGlowingIllustration`: a magnifier over a small node network on a pulsing green glow
 * (alpha 0.10 to 0.30 over 2.3 s, ease in and out, reversing). The glow is a radial gradient with no render-effect blur
 * (API 31+ only, the same rule as `GreenGlowBlobLayer`), and the strokes carry a wider translucent under-stroke instead
 * of iOS's blurred copy. Decorative, so it exposes no semantics beyond its test tag.
 */
@Composable
fun SearchExpertGlowingIllustration(modifier: Modifier = Modifier) {
    val green = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "searchGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = GLOW_MIN_ALPHA,
        targetValue = GLOW_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = GLOW_DURATION_MS, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "searchGlowAlpha",
    )
    Box(
        modifier = modifier.size(280.dp).testTag(SEARCH_EMPTY_ILLUSTRATION_TAG).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(280.dp).drawBehind {
                val radius = GLOW_RADIUS_DP.dp.toPx()
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(green.copy(alpha = glowAlpha), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                    radius = radius,
                    center = center,
                )
            },
        )
        Canvas(Modifier.size(width = 280.dp, height = 260.dp)) { drawNetworkAndLens(green) }
    }
}

private fun DrawScope.drawNetworkAndLens(green: Color) {
    val unit = 1.dp.toPx()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val lensCenter = Offset(cx - 10f * unit, cy - 8f * unit)
    val lensRadius = LENS_RADIUS * unit

    drawLens(green, lensCenter, lensRadius, unit)
    drawLensGraph(green, lensCenter, unit)
    drawNodes(green, Offset(cx, cy), lensCenter, lensRadius, unit)
    ScatterDots.forEach { (x, y) ->
        drawCircle(green.copy(alpha = SCATTER_ALPHA), radius = 2f * unit, center = Offset(cx + x * unit, cy + y * unit))
    }
}

/** A stroke with iOS's glow: the same shape first drawn wider and translucent, then the crisp stroke on top. */
private fun DrawScope.glowStroke(
    green: Color,
    unit: Float,
    draw: (Color, Stroke) -> Unit,
) {
    draw(
        green.copy(alpha = UNDERGLOW_ALPHA),
        Stroke(width = (MAIN_STROKE + UNDERGLOW_EXTRA) * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    draw(green, Stroke(width = MAIN_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawLens(
    green: Color,
    lensCenter: Offset,
    lensRadius: Float,
    unit: Float,
) {
    glowStroke(green, unit) { color, stroke ->
        drawCircle(color, radius = lensRadius, center = lensCenter, style = stroke)
    }
    val handleStart = lensCenter + Offset(lensRadius * HANDLE_OFFSET, lensRadius * HANDLE_OFFSET)
    val handleEnd = handleStart + Offset(30f * unit, 30f * unit)
    glowStroke(green, unit) { color, stroke ->
        drawLine(color, handleStart, handleEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawLensGraph(
    green: Color,
    lensCenter: Offset,
    unit: Float,
) {
    val points = GraphPoints.map { (x, y) -> lensCenter + Offset(x * unit, y * unit) }
    val color = green.copy(alpha = GRAPH_ALPHA)
    points.zipWithNext().forEach { (from, to) ->
        drawLine(color, from, to, strokeWidth = 2f * unit, cap = StrokeCap.Round)
    }
    points.forEach { drawCircle(green, radius = 3f * unit, center = it) }
}

private fun DrawScope.drawNodes(
    green: Color,
    center: Offset,
    lensCenter: Offset,
    lensRadius: Float,
    unit: Float,
) {
    val positions = NetworkNodes.map { (x, y, _) -> center + Offset(x * unit, y * unit) }
    NetworkNodes.forEachIndexed { index, (_, _, radius) ->
        val point = positions[index]
        val angle = atan2(point.y - lensCenter.y, point.x - lensCenter.x)
        val edge = Offset(lensCenter.x + cos(angle) * lensRadius, lensCenter.y + sin(angle) * lensRadius)
        drawLine(green.copy(alpha = SPOKE_ALPHA), point, edge, strokeWidth = 1.2f * unit, cap = StrokeCap.Round)
        drawCircle(green, radius = radius * unit, center = point)
        positions.getOrNull(index + 1)?.let { next ->
            drawLine(green.copy(alpha = LINK_ALPHA), point, next, strokeWidth = 1f * unit, cap = StrokeCap.Round)
        }
    }
}

@Preview(name = "SearchExpertGlowingIllustration")
@Composable
private fun SearchExpertGlowingIllustrationPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SearchExpertGlowingIllustration()
        }
    }
}
