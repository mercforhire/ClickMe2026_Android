package com.uptrendinvestments.clickmeapp.components.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the spinner a busy [MessageButton] shows in place of its glyph. */
const val MESSAGE_BUTTON_SPINNER_TAG = "message_button_spinner"

private val TextButtonMinHeight = 56.dp
private val TextGlyphSize = 20.dp
private val IconGlyphSize = 24.dp
private val SpinnerStroke = 2.dp
private val OutlineWidth = 1.dp

/** The two shapes of the Message control. */
enum class MessageButtonVariant {
    /** Outlined pill with the chat glyph and a label. */
    Text,

    /** Outlined round icon button, for cards and rows where a label does not fit. */
    Icon,
}

/**
 * The Message control for every chat entry point (D-14). The [MessageButtonVariant.Text] form is an outlined, neutral
 * 56 dp pill with a chat glyph and [label]; the [MessageButtonVariant.Icon] form is an [iconTarget] (56 dp in cards,
 * 48 dp elsewhere) outlined icon button described by [contentDescription] (the label when null).
 *
 * While [busy] both forms show a spinner in place of the glyph, expose "Opening chat" to accessibility and ignore
 * taps. [enabled] false disables the control. [contentPadding] is the Text form's side padding (the Icon form ignores
 * it); a Message beside a Reschedule passes `ActionButtonPairDefaults.ContentPadding` so its label fits a half-width slot.
 */
@Composable
fun MessageButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    enabled: Boolean = true,
    variant: MessageButtonVariant = MessageButtonVariant.Text,
    contentDescription: String? = null,
    iconTarget: Dp = TextButtonMinHeight,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.lg),
) {
    val openingState = stringResource(R.string.chat_entry_opening)
    val guardedClick = { if (!busy) onClick() }
    when (variant) {
        MessageButtonVariant.Text ->
            PressScaleButton(
                onClick = guardedClick,
                enabled = enabled,
                contentPadding = contentPadding,
                modifier =
                    modifier
                        .heightIn(min = TextButtonMinHeight)
                        .border(OutlineWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .then(if (busy) Modifier.semantics { stateDescription = openingState } else Modifier),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                if (busy) Spinner(TextGlyphSize) else Glyph(TextGlyphSize)
                Spacer(Modifier.width(Spacing.sm))
                Text(label)
            }
        MessageButtonVariant.Icon -> {
            val description = contentDescription ?: label
            OutlinedIconButton(
                onClick = guardedClick,
                modifier =
                    modifier
                        .size(iconTarget)
                        .semantics {
                            this.contentDescription = description
                            if (busy) stateDescription = openingState
                        },
                enabled = enabled,
                border = BorderStroke(OutlineWidth, MaterialTheme.colorScheme.outlineVariant),
            ) {
                if (busy) Spinner(IconGlyphSize) else Glyph(IconGlyphSize)
            }
        }
    }
}

@Composable
private fun Glyph(size: Dp) {
    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(size))
}

@Composable
private fun Spinner(size: Dp) {
    CircularProgressIndicator(
        modifier = Modifier.size(size).testTag(MESSAGE_BUTTON_SPINNER_TAG),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = SpinnerStroke,
    )
}

/** The "Couldn't open chat" dialog every Message control shows for a failed initiate (D-15). */
@Composable
fun ChatEntryErrorDialog(
    error: FieldError?,
    onDismiss: () -> Unit,
) {
    if (error != null) {
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismiss,
            title = stringResource(R.string.chat_entry_error_title),
        )
    }
}
