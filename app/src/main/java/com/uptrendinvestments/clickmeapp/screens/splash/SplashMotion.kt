package com.uptrendinvestments.clickmeapp.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/** Duration of the mark and glow entrance (fade from 0 and scale up), iOS `easeOut(duration: 0.7)`. */
const val SPLASH_LOGO_ENTRANCE_MS = 700

/** Scale the mark and glow start from, iOS `logoScale = 0.82`. */
const val SPLASH_LOGO_START_SCALE = 0.82f

/** Duration of the wordmark and tagline fade, iOS `easeOut(duration: 0.6)`. */
const val SPLASH_TEXT_ENTRANCE_MS = 600

/** Delay before the wordmark and tagline start to fade, iOS `.delay(0.35)`. */
const val SPLASH_TEXT_DELAY_MS = 350

/** Half-cycle of the glow pulse, iOS `easeInOut(duration: 1.6).repeatForever(autoreverses: true)`. */
const val SPLASH_GLOW_PULSE_MS = 1600

const val SPLASH_GLOW_ALPHA_MIN = 0.28f
const val SPLASH_GLOW_ALPHA_MAX = 0.45f
const val SPLASH_GLOW_SCALE_MIN = 0.95f
const val SPLASH_GLOW_SCALE_MAX = 1.05f

/** Midpoint of the pulse range, shown (with scale 1) as the static glow when animations are off. */
const val SPLASH_GLOW_ALPHA_STATIC = 0.365f

/**
 * The animated values of the splash. Each is exposed as a getter over a [State] so a caller that reads it inside
 * `graphicsLayer { }` or `drawBehind { }` invalidates only that phase, never recomposition.
 */
@Stable
class SplashMotionValues(
    private val logoAlphaState: State<Float>,
    private val logoScaleState: State<Float>,
    private val textAlphaState: State<Float>,
    private val glowAlphaState: State<Float>,
    private val glowScaleState: State<Float>,
) {
    val logoAlpha: Float get() = logoAlphaState.value
    val logoScale: Float get() = logoScaleState.value
    val textAlpha: Float get() = textAlphaState.value
    val glowAlpha: Float get() = glowAlphaState.value
    val glowScale: Float get() = glowScaleState.value
}

/**
 * The splash motion from iOS `SplashScreenView.playAnimations` (D-07): the mark and glow fade and scale in over 700 ms,
 * the text fades in over 600 ms after 350 ms, and the glow pulses forever.
 *
 * With [animationsEnabled] false, or when the entrance already played before a configuration change, everything rests
 * on its final value and the glow is static at [SPLASH_GLOW_ALPHA_STATIC] and scale 1. The entrance never replays on
 * rotation or any other configuration change.
 */
@Composable
fun rememberSplashMotion(animationsEnabled: Boolean): SplashMotionValues {
    var entrancePlayed by rememberSaveable { mutableStateOf(false) }
    // Decided once per composition instance: flipping entrancePlayed below must not swap the values mid-flight.
    val playEntrance = remember(animationsEnabled) { animationsEnabled && !entrancePlayed }
    val logoAlpha = remember(playEntrance) { Animatable(if (playEntrance) 0f else 1f) }
    val logoScale = remember(playEntrance) { Animatable(if (playEntrance) SPLASH_LOGO_START_SCALE else 1f) }
    val textAlpha = remember(playEntrance) { Animatable(if (playEntrance) 0f else 1f) }

    LaunchedEffect(playEntrance) {
        if (!playEntrance) return@LaunchedEffect
        // Recorded as soon as the entrance starts: a rotation mid-way lands on the final frame, not on a replay.
        entrancePlayed = true
        launch { logoAlpha.animateTo(1f, tween(SPLASH_LOGO_ENTRANCE_MS, easing = FastOutSlowInEasing)) }
        launch { logoScale.animateTo(1f, tween(SPLASH_LOGO_ENTRANCE_MS, easing = FastOutSlowInEasing)) }
        launch {
            textAlpha.animateTo(
                1f,
                tween(SPLASH_TEXT_ENTRANCE_MS, delayMillis = SPLASH_TEXT_DELAY_MS, easing = FastOutSlowInEasing),
            )
        }
    }

    val glowAlpha: State<Float>
    val glowScale: State<Float>
    if (animationsEnabled) {
        val pulse = rememberInfiniteTransition(label = "splashGlow")
        glowAlpha =
            pulse.animateFloat(
                initialValue = SPLASH_GLOW_ALPHA_MIN,
                targetValue = SPLASH_GLOW_ALPHA_MAX,
                animationSpec = infiniteRepeatable(tween(SPLASH_GLOW_PULSE_MS, easing = EaseInOut), RepeatMode.Reverse),
                label = "splashGlowAlpha",
            )
        glowScale =
            pulse.animateFloat(
                initialValue = SPLASH_GLOW_SCALE_MIN,
                targetValue = SPLASH_GLOW_SCALE_MAX,
                animationSpec = infiniteRepeatable(tween(SPLASH_GLOW_PULSE_MS, easing = EaseInOut), RepeatMode.Reverse),
                label = "splashGlowScale",
            )
    } else {
        glowAlpha = remember { mutableFloatStateOf(SPLASH_GLOW_ALPHA_STATIC) }
        glowScale = remember { mutableFloatStateOf(1f) }
    }

    return remember(playEntrance, animationsEnabled) {
        SplashMotionValues(
            logoAlphaState = logoAlpha.asState(),
            logoScaleState = logoScale.asState(),
            textAlphaState = textAlpha.asState(),
            glowAlphaState = glowAlpha,
            glowScaleState = glowScale,
        )
    }
}
