package com.uptrendinvestments.clickmeapp.components.buttonstyles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val MinTouchTarget = 48.dp

/** Scales the element to [pressedScale] while [interactionSource] reports a press (iOS PressScaleButtonStyle). */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = Motion.PRESSED_SCALE,
): Modifier =
    composed {
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = tween(durationMillis = Motion.PRESS_DURATION_MS, easing = Motion.pressEasing),
            label = "pressScale",
        )
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

/**
 * Material 3 [Button] that shrinks slightly while pressed. Keeps the M3 ripple, button semantics and a
 * 48 dp minimum touch target.
 */
@Composable
fun PressScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = PressScaleButtonDefaults.filledColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .pressScale(source)
                .then(modifier)
                .heightIn(min = MinTouchTarget),
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = source,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            content()
        }
    }
}

object PressScaleButtonDefaults {
    /** Primary fill (container primary, content onPrimary). */
    @Composable
    fun filledColors(): ButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )

    /** Neutral raised fill for secondary actions. */
    @Composable
    fun tonalColors(): ButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )

    /** Destructive fill for delete and similar actions. */
    @Composable
    fun destructiveColors(): ButtonColors =
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.extras.destructive,
            contentColor = MaterialTheme.extras.onDestructive,
        )
}

@Preview(name = "PressScaleButton", showBackground = false)
@Composable
private fun PressScaleButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PressScaleButton(onClick = {}) { Text("Continue") }
                PressScaleButton(onClick = {}, colors = PressScaleButtonDefaults.tonalColors()) { Text("Cancel") }
                PressScaleButton(onClick = {}, colors = PressScaleButtonDefaults.destructiveColors()) { Text("Delete") }
            }
        }
    }
}
