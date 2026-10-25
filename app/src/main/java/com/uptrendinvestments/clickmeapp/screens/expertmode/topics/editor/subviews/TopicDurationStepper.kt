package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.TopicEditorRules
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.topicDurationText
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val ButtonSize = 48.dp
private val GlyphSize = 24.dp

/**
 * Session duration control: minus, the formatted value, plus (iOS stepper). Each button disables at its bound
 * (30 and 480 minutes); the value is a polite live region so TalkBack announces each step. No press-and-hold repeat.
 * Stateless: the ViewModel applies the step.
 */
@Composable
fun TopicDurationStepper(
    minutes: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.topic_editor_duration_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(
                    onClick = onDecrease,
                    enabled = enabled && TopicEditorRules.canDecrease(minutes),
                    glyph = Icons.Outlined.Remove,
                    description = stringResource(R.string.topic_editor_duration_decrease),
                )
                Text(
                    text = topicDurationText(minutes),
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                StepButton(
                    onClick = onIncrease,
                    enabled = enabled && TopicEditorRules.canIncrease(minutes),
                    glyph = Icons.Outlined.Add,
                    description = stringResource(R.string.topic_editor_duration_increase),
                )
            }
        }
    }
}

@Composable
private fun StepButton(
    onClick: () -> Unit,
    enabled: Boolean,
    glyph: ImageVector,
    description: String,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(ButtonSize)) {
        Icon(imageVector = glyph, contentDescription = description, modifier = Modifier.size(GlyphSize))
    }
}

@Preview(name = "TopicDurationStepper")
@Composable
private fun TopicDurationStepperPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicDurationStepper(minutes = 90, onDecrease = {}, onIncrease = {}, enabled = true)
        }
    }
}
