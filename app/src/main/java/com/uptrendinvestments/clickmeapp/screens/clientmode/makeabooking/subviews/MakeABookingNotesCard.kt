package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.NOTES_MAX_CHARS
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

const val NOTES_FIELD_TAG = "make_a_booking_notes"

private val RestBorder = 1.dp
private val FocusedBorder = 2.dp

/**
 * Optional message to the expert: a multi-line field with a "{n}/500" counter.
 *
 * Stateless: [notes] is the whole value and every edit goes to [onNotesChange]; the ViewModel applies `capNotes`, so
 * pasted text over the limit is cut there. Enter inserts a newline, and the field scrolls into view when focused.
 * [readOnly] is set while a booking is in flight: the text can still be read and keeps focus, but not edited.
 */
@Composable
fun MakeABookingNotesCard(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val borderWidth by animateDpAsState(
        targetValue = if (focused) FocusedBorder else RestBorder,
        animationSpec = tween(durationMillis = Motion.CONTROL_STATE_DURATION_MS),
        label = "notesBorderWidth",
    )
    LaunchedEffect(focused) {
        if (focused) bringIntoViewRequester.bringIntoView()
    }
    val counterDescription = stringResource(R.string.make_a_booking_notes_counter_a11y, notes.length, NOTES_MAX_CHARS)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        BasicTextField(
            value = notes,
            onValueChange = onNotesChange,
            readOnly = readOnly,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .background(colorScheme.surfaceContainer, shape)
                    .border(borderWidth, if (focused) colorScheme.primary else colorScheme.outline, shape)
                    .testTag(NOTES_FIELD_TAG),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            minLines = 4,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colorScheme.primary),
        ) { innerTextField ->
            Box(modifier = Modifier.padding(Spacing.md)) {
                if (notes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.make_a_booking_notes_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
                innerTextField()
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(R.string.make_a_booking_notes_counter, notes.length, NOTES_MAX_CHARS),
                modifier = Modifier.semantics { contentDescription = counterDescription },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

@Preview(name = "MakeABookingNotesCard")
@Composable
private fun MakeABookingNotesCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                MakeABookingNotesCard(notes = "", onNotesChange = {})
                MakeABookingNotesCard(notes = "I would like to discuss my portfolio.", onNotesChange = {})
            }
        }
    }
}
