package com.uptrendinvestments.clickmeapp.components.backgrounds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

private const val CENTER_ALPHA = 0.18f
private const val MID_ALPHA = 0.07f

/**
 * Two soft primary-colored glows (top right, lower left) drawn as radial gradients. No
 * render-effect softening: that is API 31+ only and costs GPU time. Decorative, so it exposes no semantics.
 * Opt-in per screen.
 */
@Composable
fun GreenGlowBlobLayer(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .clearAndSetSemantics { }
            .drawBehind {
                val radius = 190.dp.toPx()

                fun blob(center: Offset) {
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0f to primary.copy(alpha = CENTER_ALPHA),
                                        0.55f to primary.copy(alpha = MID_ALPHA),
                                        1f to Color.Transparent,
                                    ),
                                center = center,
                                radius = radius,
                            ),
                        radius = radius,
                        center = center,
                    )
                }
                blob(Offset(size.width + 30.dp.toPx(), size.height * 0.10f))
                blob(Offset(-20.dp.toPx(), size.height * 0.65f))
            },
    )
}

@Preview
@Composable
private fun GreenGlowBlobLayerPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GreenGlowBlobLayer()
        }
    }
}
