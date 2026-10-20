package com.uptrendinvestments.clickmeapp.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val SegmentHeight = 4.dp
private val SegmentShape = RoundedCornerShape(2.dp)
private const val SEGMENT_COUNT = 3

/**
 * Three-segment strength bar with a "Strength: <level>" caption (iOS PasswordStrengthMeter). Weak fills one
 * segment, Fair two, Strong three. TalkBack reads a single "Password strength: <level>" phrase. The caller
 * wraps it in AnimatedVisibility.
 */
@Composable
fun PasswordStrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val extras = MaterialTheme.extras
    val fillColor =
        when (strength) {
            PasswordStrength.Weak -> extras.destructive
            PasswordStrength.Fair -> MaterialTheme.colorScheme.tertiary
            PasswordStrength.Strong -> MaterialTheme.colorScheme.primary
            PasswordStrength.Empty -> extras.overlayWhite15
        }
    val filled =
        when (strength) {
            PasswordStrength.Empty -> 0
            PasswordStrength.Weak -> 1
            PasswordStrength.Fair -> 2
            PasswordStrength.Strong -> SEGMENT_COUNT
        }
    val levelWord =
        when (strength) {
            PasswordStrength.Empty -> ""
            PasswordStrength.Weak -> stringResource(R.string.password_strength_weak)
            PasswordStrength.Fair -> stringResource(R.string.password_strength_fair)
            PasswordStrength.Strong -> stringResource(R.string.password_strength_strong)
        }
    val caption = stringResource(R.string.password_strength_caption)
    val description =
        if (strength == PasswordStrength.Empty) caption else stringResource(R.string.a11y_password_strength, levelWord)

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(SEGMENT_COUNT) { index ->
                val target = if (index < filled) fillColor else extras.overlayWhite15
                val color by animateColorAsState(
                    targetValue = target,
                    animationSpec = tween(Motion.STRENGTH_SEGMENT_COLOR_MS),
                    label = "strengthSegment",
                )
                Segment(color)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = extras.onSurfaceMuted,
            )
            if (strength != PasswordStrength.Empty) {
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = levelWord,
                    style = MaterialTheme.typography.labelMedium,
                    color = fillColor,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Segment(color: Color) {
    Spacer(
        Modifier
            .weight(1f)
            .height(SegmentHeight)
            .background(color, SegmentShape),
    )
}

@Preview(name = "PasswordStrengthMeter")
@Composable
private fun PasswordStrengthMeterPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                PasswordStrength.entries.forEach { PasswordStrengthMeter(it) }
            }
        }
    }
}
