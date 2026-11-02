package com.uptrendinvestments.clickmeapp.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/**
 * The Compose splash stage shown after the OS splash (D-07), mirroring iOS `SplashScreenView`: a full-bleed radial
 * gradient and a centered mark with a pulsing glow, wordmark and tagline, with the iOS entrance (see
 * [rememberSplashMotion]). There is no `Scaffold` and no `BackHandler`: system back is not intercepted during the
 * splash.
 *
 * [onComposed] is reported once, after the first composition, and starts the minimum display time in
 * `AppViewModel.onSplashComposed`.
 */
@Composable
fun SplashScreen(
    onComposed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnComposed by rememberUpdatedState(onComposed)
    LaunchedEffect(Unit) { latestOnComposed() }

    SplashContent(motion = rememberSplashMotion(rememberAnimationsEnabled()), modifier = modifier)
}

@Composable
internal fun SplashContent(
    motion: SplashMotionValues,
    modifier: Modifier = Modifier,
) {
    val radiusPx = with(LocalDensity.current) { 520.dp.toPx() }
    // Read outside the semantics lambda, which is not composable.
    val a11y = stringResource(R.string.splash_a11y)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(SPLASH_SCREEN_TAG)
                // One TalkBack node: the mark and glow are decorative and nothing is a live region.
                .clearAndSetSemantics { contentDescription = a11y }
                .background(
                    Brush.radialGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                        radius = radiusPx,
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.contentMaxWidth().offset(y = (-20).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SplashLogo(motion)
            Spacer(Modifier.height(Spacing.xl))
            Column(
                modifier = Modifier.graphicsLayer { alpha = motion.textAlpha },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.splash_wordmark),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The mark over its soft pulsing halo, fading and scaling in together. */
@Composable
private fun SplashLogo(motion: SplashMotionValues) {
    val glowColor = MaterialTheme.colorScheme.primary
    val glowRadiusPx = with(LocalDensity.current) { GLOW_RADIUS.toPx() }
    Box(
        modifier =
            Modifier.graphicsLayer {
                alpha = motion.logoAlpha
                scaleX = motion.logoScale
                scaleY = motion.logoScale
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(GLOW_BOX)
                    .graphicsLayer {
                        scaleX = motion.glowScale
                        scaleY = motion.glowScale
                    }.drawBehind {
                        val alpha = motion.glowAlpha
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    0f to glowColor.copy(alpha = alpha),
                                    0.55f to glowColor.copy(alpha = alpha / 2f),
                                    1f to Color.Transparent,
                                    center = center,
                                    radius = glowRadiusPx,
                                ),
                            radius = glowRadiusPx,
                            center = center,
                        )
                    },
        )
        Image(
            painter = painterResource(R.drawable.clickme_mark),
            contentDescription = null,
            modifier = Modifier.size(148.dp),
        )
    }
}

/** Test tag of the splash root. */
internal const val SPLASH_SCREEN_TAG = "splash_screen"

private val GLOW_BOX = 190.dp
private val GLOW_RADIUS = 160.dp
