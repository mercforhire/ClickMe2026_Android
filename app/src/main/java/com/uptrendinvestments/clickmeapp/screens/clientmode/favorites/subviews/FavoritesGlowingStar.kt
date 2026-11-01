package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/** Test tag on the empty-state star. */
const val FAVORITES_GLOWING_STAR_TAG = "favorites_glowing_star"

private const val GLOW_MIN_ALPHA = 0.10f
private const val GLOW_MAX_ALPHA = 0.30f
private const val GLOW_DURATION_MS = 2300
private const val OUTER_COPY_ALPHA = 0.18f
private const val INNER_COPY_ALPHA = 0.30f
private const val DOT_ALPHA = 0.55f

private val StarBoxSize = 200.dp
private val GlowRadius = 100.dp
private val StarSize = 96.dp
private val InnerCopySize = 112.dp
private val OuterCopySize = 128.dp

/** Scatter dots: x and y offsets from the center in dp, and the radius in dp (iOS `FavoritesGlowingStar`). */
private val Dots =
    listOf(
        Triple(-70f, -40f, 4f),
        Triple(80f, -30f, 5f),
        Triple(-80f, 40f, 3f),
        Triple(65f, 55f, 4f),
        Triple(0f, -85f, 3f),
    )

/**
 * Port of iOS `FavoritesGlowingStar`: an outlined star over a pulsing green glow (alpha 0.10 to 0.30 over 2.3 s, ease
 * in and out, reversing), with five scattered dots. The glow is a radial gradient with no render-effect blur (API 31+
 * only, the rule `GreenGlowBlobLayer` follows), and two larger translucent copies of the star stand in for the iOS
 * blur. Decorative, so it exposes no semantics beyond its test tag.
 */
@Composable
fun FavoritesGlowingStar(modifier: Modifier = Modifier) {
    val green = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "favoritesStarGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = GLOW_MIN_ALPHA,
        targetValue = GLOW_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = GLOW_DURATION_MS, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "favoritesStarGlowAlpha",
    )
    Box(
        modifier = modifier.size(StarBoxSize).testTag(FAVORITES_GLOWING_STAR_TAG).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(StarBoxSize).drawBehind {
                val radius = GlowRadius.toPx()
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
        Icon(
            imageVector = Icons.Outlined.StarBorder,
            contentDescription = null,
            modifier = Modifier.size(OuterCopySize).alpha(OUTER_COPY_ALPHA),
            tint = green,
        )
        Icon(
            imageVector = Icons.Outlined.StarBorder,
            contentDescription = null,
            modifier = Modifier.size(InnerCopySize).alpha(INNER_COPY_ALPHA),
            tint = green,
        )
        Icon(
            imageVector = Icons.Outlined.StarBorder,
            contentDescription = null,
            modifier = Modifier.size(StarSize),
            tint = green,
        )
        Canvas(Modifier.size(StarBoxSize)) {
            Dots.forEach { (x, y, radius) ->
                drawCircle(
                    color = green.copy(alpha = DOT_ALPHA),
                    radius = radius.dp.toPx(),
                    center = center + Offset(x.dp.toPx(), y.dp.toPx()),
                )
            }
        }
    }
}

@Preview(name = "FavoritesGlowingStar")
@Composable
private fun FavoritesGlowingStarPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FavoritesGlowingStar()
        }
    }
}
