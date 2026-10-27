package com.uptrendinvestments.clickmeapp.components.chat

import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
private val SpinnerStroke = 2.dp

/**
 * The Message control for every chat entry point (D-14): an outlined, neutral 56 dp button with a chat glyph and
 * [label]. While [busy] it shows a spinner, exposes "Opening chat" to accessibility and ignores taps.
 */
@Composable
fun MessageButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    enabled: Boolean = true,
) {
    val openingState = stringResource(R.string.chat_entry_opening)
    PressScaleButton(
        onClick = { if (!busy) onClick() },
        enabled = enabled,
        modifier =
            modifier
                .heightIn(min = TextButtonMinHeight)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .then(if (busy) Modifier.semantics { stateDescription = openingState } else Modifier),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(TextGlyphSize).testTag(MESSAGE_BUTTON_SPINNER_TAG),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = SpinnerStroke,
            )
        } else {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(TextGlyphSize))
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(label)
    }
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
