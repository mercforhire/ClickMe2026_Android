package com.uptrendinvestments.clickmeapp.screens.login.passwordchanged

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Entrance timings, from the 03-UI-SPEC Motion table "Password Changed entrance". All one-shot.
private const val CHECK_DELAY_MS = 150L
private const val CHECK_DAMPING = 0.6f
private const val GLOW_DELAY_MS = 300L
private const val GLOW_DURATION_MS = 550
private const val TEXT_DELAY_MS = 520L
private const val TEXT_DURATION_MS = 450
private const val BUTTON_DELAY_MS = 700L
private const val BUTTON_DURATION_MS = 400
private const val CHECK_START_SCALE = 0.4f
private const val SLIDE_DISTANCE_DP = 16f

private val CheckSize = 72.dp
private val GlowRadius = 120.dp
private const val GLOW_CENTER_ALPHA = 0.25f

/**
 * Terminal screen of the password-recovery flow (iOS LoginPasswordSetView). It has no app bar and no
 * back arrow: [onGoToLogin] is the single exit, and system Back is routed to the same callback by the
 * navigation layer (D-10, D-11).
 *
 * The entrance runs once; a saveable flag makes a recreated screen (rotation) show its final state.
 * [onEntranceStarted] fires only when the animated entrance actually runs (test seam).
 */
@Composable
fun PasswordChangedScreen(
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onEntranceStarted: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val entrance = rememberPasswordChangedEntrance(onEntranceStarted)
    val headlineFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { headlineFocus.requestFocus() } }

    Box(modifier.fillMaxSize().background(colorScheme.surface)) {
        GreenGlowBlobLayer(Modifier.matchParentSize())
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CheckGlyph(entrance)
                Spacer(Modifier.height(Spacing.xl))
                Column(
                    modifier = Modifier.slideFade(entrance.textAlpha, entrance.textOffset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.password_changed_headline),
                        modifier =
                            Modifier
                                .focusRequester(headlineFocus)
                                .focusable()
                                .semantics { heading() },
                        style = MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = stringResource(R.string.password_changed_body),
                        modifier = Modifier.padding(horizontal = Spacing.lg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            AsyncSaveButton(
                title = stringResource(R.string.password_changed_go_to_login),
                state = SaveState.Idle,
                onClick = onGoToLogin,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.xxl)
                        .slideFade(entrance.buttonAlpha, entrance.buttonOffset),
            )
        }
    }
}

/** Check glyph drawn over a radial glow; the glow fades in separately from the glyph spring. */
@Composable
private fun CheckGlyph(entrance: PasswordChangedEntrance) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .size(CheckSize)
                .drawBehind {
                    val radius = GlowRadius.toPx()
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = GLOW_CENTER_ALPHA), Color.Transparent),
                                radius = radius,
                            ),
                        radius = radius,
                        alpha = entrance.glowAlpha.value.coerceIn(0f, 1f),
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            modifier =
                Modifier
                    .size(CheckSize)
                    .graphicsLayer {
                        val scale = entrance.checkScale.value
                        scaleX = scale
                        scaleY = scale
                        alpha = entrance.checkAlpha.value.coerceIn(0f, 1f)
                    },
            tint = primary,
        )
    }
}

private fun Modifier.slideFade(
    alpha: Animatable<Float, *>,
    offsetDp: Animatable<Float, *>,
): Modifier =
    graphicsLayer {
        this.alpha = alpha.value.coerceIn(0f, 1f)
        translationY = offsetDp.value.dp.toPx()
    }

/** Animated values of the entrance; each starts hidden and is driven by [rememberPasswordChangedEntrance]. */
private class PasswordChangedEntrance {
    val checkScale = Animatable(CHECK_START_SCALE)
    val checkAlpha = Animatable(0f)
    val glowAlpha = Animatable(0f)
    val textAlpha = Animatable(0f)
    val textOffset = Animatable(SLIDE_DISTANCE_DP)
    val buttonAlpha = Animatable(0f)
    val buttonOffset = Animatable(SLIDE_DISTANCE_DP)

    suspend fun snapToEnd() {
        checkScale.snapTo(1f)
        checkAlpha.snapTo(1f)
        glowAlpha.snapTo(1f)
        textAlpha.snapTo(1f)
        textOffset.snapTo(0f)
        buttonAlpha.snapTo(1f)
        buttonOffset.snapTo(0f)
    }
}

@Composable
private fun rememberPasswordChangedEntrance(onStarted: () -> Unit): PasswordChangedEntrance {
    val entrance = remember { PasswordChangedEntrance() }
    var played by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (played) {
            entrance.snapToEnd()
            return@LaunchedEffect
        }
        played = true
        onStarted()
        val spring = spring<Float>(dampingRatio = CHECK_DAMPING, stiffness = Spring.StiffnessMediumLow)
        launch {
            delay(CHECK_DELAY_MS)
            launch { entrance.checkScale.animateTo(1f, spring) }
            launch { entrance.checkAlpha.animateTo(1f, spring) }
        }
        launch {
            delay(GLOW_DELAY_MS)
            entrance.glowAlpha.animateTo(1f, tween(GLOW_DURATION_MS, easing = EaseOut))
        }
        launch {
            delay(TEXT_DELAY_MS)
            launch { entrance.textAlpha.animateTo(1f, tween(TEXT_DURATION_MS, easing = EaseOut)) }
            launch { entrance.textOffset.animateTo(0f, tween(TEXT_DURATION_MS, easing = EaseOut)) }
        }
        launch {
            delay(BUTTON_DELAY_MS)
            launch { entrance.buttonAlpha.animateTo(1f, tween(BUTTON_DURATION_MS, easing = EaseOut)) }
            launch { entrance.buttonOffset.animateTo(0f, tween(BUTTON_DURATION_MS, easing = EaseOut)) }
        }
    }
    return entrance
}

@Preview(name = "PasswordChanged")
@Composable
private fun PasswordChangedScreenPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            PasswordChangedScreen(onGoToLogin = {})
        }
    }
}
