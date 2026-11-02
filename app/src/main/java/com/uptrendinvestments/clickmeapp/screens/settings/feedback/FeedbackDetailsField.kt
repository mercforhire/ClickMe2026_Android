package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the Details text field. */
const val FEEDBACK_DETAILS_TAG = "feedback_details"

/** Test tag on the Details minimum-length message. */
const val FEEDBACK_DETAILS_ERROR_TAG = "feedback_details_error"

private val DetailsMinHeight = 180.dp
private val RestBorder = 1.dp
private val ActiveBorder = 2.dp

/**
 * The free-text Details field of Send Feedback. A plain multi-line [TextField] styled like the review comment: the
 * border is drawn around it (1 dp resting, 2 dp focused, 2 dp destructive with an error). There is deliberately no
 * line cap, character cap or counter (D-05): the field grows inside the screen's scroll column. [showError] shows
 * the minimum-length message under it, sliding in as `ValidatedField`'s error row does.
 */
@Composable
fun FeedbackDetailsField(
    value: String,
    onValueChange: (String) -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val extras = MaterialTheme.extras
    val shape = MaterialTheme.shapes.medium
    val borderColor =
        when {
            showError -> extras.destructive
            focused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val borderWidth = if (showError || focused) ActiveBorder else RestBorder
    val fieldName = stringResource(R.string.feedback_label_details)
    val offsetPx = with(LocalDensity.current) { Motion.FIELD_ERROR_OFFSET_DP.dp.roundToPx() }

    Column(modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DetailsMinHeight)
                    .onFocusChanged { focused = it.isFocused }
                    .border(width = borderWidth, color = borderColor, shape = shape)
                    .semantics { contentDescription = fieldName }
                    .testTag(FEEDBACK_DETAILS_TAG),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.feedback_details_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = extras.onSurfaceMuted,
                )
            },
            isError = showError,
            shape = shape,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors =
                TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    errorTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    errorCursorColor = extras.destructive,
                ),
        )
        AnimatedVisibility(
            visible = showError,
            enter =
                fadeIn(tween(Motion.FIELD_ERROR_DURATION_MS)) +
                    slideInVertically(tween(Motion.FIELD_ERROR_DURATION_MS)) { -offsetPx },
            exit =
                fadeOut(tween(Motion.FIELD_ERROR_DURATION_MS)) +
                    slideOutVertically(tween(Motion.FIELD_ERROR_DURATION_MS)) { -offsetPx },
        ) {
            Text(
                text = stringResource(R.string.feedback_details_too_short),
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs).testTag(FEEDBACK_DETAILS_ERROR_TAG),
                style = MaterialTheme.typography.bodySmall,
                color = extras.destructive,
            )
        }
    }
}
