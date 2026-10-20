package com.uptrendinvestments.clickmeapp.components.buttons

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

enum class SaveState { Idle, Saving, Saved }

private val GlowElevation = 12.dp
private const val GLOW_ALPHA = 0.5f

/**
 * Full-width pill button with three states of identical size (iOS AsyncSaveButton): Idle shows [title],
 * Saving shows a spinner, Saved shows a check and [savedTitle]. Taps are ignored unless the state is
 * [SaveState.Idle] and [enabled]; Saving and Saved keep the primary look.
 */
@Composable
fun AsyncSaveButton(
    title: String,
    state: SaveState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    savedTitle: String = stringResource(R.string.save_button_saved_default),
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactive = enabled && state == SaveState.Idle
    val primaryLook = enabled || state != SaveState.Idle
    val savingDescription = stringResource(R.string.a11y_state_saving)
    val savedDescription = stringResource(R.string.a11y_state_saved)

    val colors =
        if (primaryLook) {
            PressScaleButtonDefaults.filledColors().copy(
                disabledContainerColor = colorScheme.primary,
                disabledContentColor = colorScheme.onPrimary,
            )
        } else {
            PressScaleButtonDefaults.filledColors()
        }
    val glow = colorScheme.primary.copy(alpha = GLOW_ALPHA)
    val glowModifier =
        if (primaryLook) {
            Modifier.shadow(GlowElevation, CircleShape, ambientColor = glow, spotColor = glow)
        } else {
            Modifier
        }

    PressScaleButton(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(glowModifier)
                .semantics {
                    when (state) {
                        SaveState.Saving -> stateDescription = savingDescription
                        SaveState.Saved -> {
                            stateDescription = savedDescription
                            liveRegion = LiveRegionMode.Polite
                        }
                        SaveState.Idle -> Unit
                    }
                },
        enabled = interactive,
        shape = CircleShape,
        colors = colors,
    ) {
        Crossfade(
            targetState = state,
            animationSpec = tween(durationMillis = Motion.CONTROL_STATE_DURATION_MS),
            label = "saveState",
        ) { current ->
            when (current) {
                SaveState.Idle -> Text(text = title, style = MaterialTheme.typography.labelLarge)
                SaveState.Saving ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                SaveState.Saved ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(text = savedTitle, style = MaterialTheme.typography.labelLarge)
                    }
            }
        }
    }
}

@Preview(name = "AsyncSaveButton")
@Composable
private fun AsyncSaveButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                AsyncSaveButton(title = "Save changes", state = SaveState.Idle, onClick = {})
                AsyncSaveButton(title = "Save changes", state = SaveState.Saving, onClick = {})
                AsyncSaveButton(title = "Save changes", state = SaveState.Saved, onClick = {})
            }
        }
    }
}
