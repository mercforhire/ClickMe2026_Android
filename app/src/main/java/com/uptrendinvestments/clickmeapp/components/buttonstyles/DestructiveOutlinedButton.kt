package com.uptrendinvestments.clickmeapp.components.buttonstyles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

private val MinTouchTarget = 48.dp

/**
 * Error-outlined pill button for destructive actions: Cancel Booking, Confirm Cancellation and Decline (UI-SPEC Color
 * rule). Destructive actions use `colorScheme.error` for the border and label and are never a filled error button
 * (Phase 7 A5). Shrinks slightly while pressed like [PressScaleButton]. [contentPadding] defaults to Material's 24 dp
 * sides; the Cancel Booking button beside a Reschedule passes `ActionButtonPairDefaults.ContentPadding` so its label
 * fits a half-width slot.
 */
@Composable
fun DestructiveOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val borderColor =
        if (enabled) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .pressScale(source)
                .then(modifier)
                .heightIn(min = MinTouchTarget),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        contentPadding = contentPadding,
        interactionSource = source,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            content()
        }
    }
}

@Preview(name = "DestructiveOutlinedButton", showBackground = false)
@Composable
private fun DestructiveOutlinedButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            DestructiveOutlinedButton(onClick = {}) { Text("Cancel Booking") }
        }
    }
}
