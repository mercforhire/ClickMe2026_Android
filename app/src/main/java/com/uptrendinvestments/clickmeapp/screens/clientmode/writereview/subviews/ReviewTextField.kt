package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the review text field. */
const val WRITE_REVIEW_INPUT_TAG = "write_review_input"

/** Test tag on the character counter under the field. */
const val WRITE_REVIEW_COUNTER_TAG = "write_review_counter"

/** Test tag on the non-editable comment of an already submitted review. */
const val WRITE_REVIEW_READ_ONLY_COMMENT_TAG = "write_review_read_only_comment"

private const val FIELD_MAX_LINES = 8

/**
 * The optional review comment (iOS `ReviewTextField`). A plain multi-line [TextField] on `surfaceContainer`; the
 * 1 dp resting and 2 dp focused border is drawn around it. The cap is enforced by the ViewModel. The counter shows
 * when [showCounter] and turns tertiary when [atCap].
 */
@Composable
fun ReviewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    showCounter: Boolean,
    atCap: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fieldLabel = stringResource(R.string.write_review_field_label)
    Column(modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .onFocusChanged { focused = it.isFocused }
                    .border(width = if (focused) 2.dp else 1.dp, color = borderColor, shape = shape)
                    .semantics { contentDescription = fieldLabel }
                    .testTag(WRITE_REVIEW_INPUT_TAG),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.write_review_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            },
            maxLines = FIELD_MAX_LINES,
            shape = shape,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors =
                TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
        )
        if (showCounter) {
            Text(
                text = stringResource(R.string.write_review_counter, value.length, ReviewsRepository.COMMENT_MAX),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs)
                        .testTag(WRITE_REVIEW_COUNTER_TAG),
                style = MaterialTheme.typography.bodySmall,
                color = if (atCap) MaterialTheme.colorScheme.tertiary else MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.End,
            )
        }
    }
}

/**
 * The comment of an already submitted review: a non-editable container with selectable text, or "No written
 * comment." when the review has none.
 */
@Composable
fun ReadOnlyReviewComment(
    comment: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(WRITE_REVIEW_READ_ONLY_COMMENT_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.heightIn(min = 56.dp).padding(Spacing.md)) {
            if (comment.isBlank()) {
                Text(
                    text = stringResource(R.string.write_review_no_comment),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            } else {
                SelectionContainer {
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
