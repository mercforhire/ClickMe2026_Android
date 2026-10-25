package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion

/** Test tag on the idle Save text button. */
internal const val AVAILABILITY_SAVE_TAG = "availability_save"

private val ActionBoxSize = 48.dp
private val ActionIconSize = 24.dp
private val SpinnerStroke = 2.dp

/**
 * App bar Save action (D-02): "Save" text button when idle (disabled with an explanation while days overlap), a
 * spinner while saving and a check once saved. The three looks cross-fade in 48 dp boxes so the bar does not shift.
 */
@Composable
fun AvailabilitySaveAction(
    state: SaveState,
    enabled: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = state,
        modifier = modifier,
        animationSpec = tween(durationMillis = Motion.CONTROL_STATE_DURATION_MS),
        label = "availabilitySaveAction",
    ) { current ->
        when (current) {
            SaveState.Idle -> {
                val saveLabel = stringResource(R.string.availability_save_a11y)
                val disabledReason = stringResource(R.string.availability_save_disabled)
                TextButton(
                    onClick = onSave,
                    enabled = enabled,
                    modifier =
                        Modifier
                            .testTag(AVAILABILITY_SAVE_TAG)
                            .semantics {
                                contentDescription = saveLabel
                                if (!enabled) stateDescription = disabledReason
                                onClick(label = saveLabel) {
                                    if (enabled) onSave()
                                    enabled
                                }
                            },
                ) {
                    Text(
                        text = stringResource(R.string.availability_save),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            SaveState.Saving -> {
                val savingLabel = stringResource(R.string.availability_saving)
                Box(
                    modifier = Modifier.size(ActionBoxSize).semantics { stateDescription = savingLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ActionIconSize),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = SpinnerStroke,
                    )
                }
            }

            SaveState.Saved -> {
                val savedLabel = stringResource(R.string.availability_saved)
                Box(
                    modifier =
                        Modifier.size(ActionBoxSize).semantics {
                            contentDescription = savedLabel
                            liveRegion = LiveRegionMode.Polite
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(ActionIconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Preview(name = "AvailabilitySaveAction")
@Composable
private fun AvailabilitySaveActionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AvailabilitySaveAction(state = SaveState.Idle, enabled = true, onSave = {})
        }
    }
}
