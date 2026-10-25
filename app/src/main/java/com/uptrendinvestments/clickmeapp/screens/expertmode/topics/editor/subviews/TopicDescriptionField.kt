package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private const val DESCRIPTION_MIN_LINES = 3
private const val DESCRIPTION_MAX_LINES = 6

/** Test tag of the description text field. */
const val TOPIC_DESCRIPTION_TAG = "topic_editor_description"

/**
 * Optional multi-line topic description with a live `length/2000` counter. The ViewModel caps the length, so this
 * field only displays [value] and forwards edits. Stateless.
 */
@Composable
fun TopicDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.topic_editor_description_label),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().testTag(TOPIC_DESCRIPTION_TAG),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.topic_editor_description_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            minLines = DESCRIPTION_MIN_LINES,
            maxLines = DESCRIPTION_MAX_LINES,
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    disabledTextColor = colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
                    focusedContainerColor = colorScheme.surfaceContainer,
                    unfocusedContainerColor = colorScheme.surfaceContainer,
                    disabledContainerColor = colorScheme.surfaceContainer,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    disabledBorderColor = colorScheme.outline.copy(alpha = DISABLED_ALPHA),
                    cursorColor = colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.extras.onSurfaceMuted,
                    unfocusedPlaceholderColor = MaterialTheme.extras.onSurfaceMuted,
                    disabledPlaceholderColor = MaterialTheme.extras.onSurfaceMuted.copy(alpha = DISABLED_ALPHA),
                ),
        )
        Spacer(Modifier.height(Spacing.xs))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(R.string.topic_editor_description_counter, value.length),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f

@Preview(name = "TopicDescriptionField")
@Composable
private fun TopicDescriptionFieldPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicDescriptionField(value = "A deep dive into pricing.", onValueChange = {}, enabled = true)
        }
    }
}
