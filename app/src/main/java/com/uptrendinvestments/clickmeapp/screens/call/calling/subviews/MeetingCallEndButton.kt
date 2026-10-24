package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

const val CALL_END_BUTTON_TAG = "call_end_button"

private val EndButtonMinHeight = 56.dp
private val EndIconSize = 24.dp
private val SpinnerStroke = 2.dp

/**
 * The End Call / End Session button: the one approved filled destructive button in the app (UI-SPEC Color exception
 * A1). There is no confirmation dialog because hanging up never ends the session; the other party can rejoin until the
 * scheduled time is over (CALL-END-SEMANTICS). While [isEnding] it shows a spinner, announces "Ending call" and ignores
 * taps; the container keeps full opacity so it stays readable.
 */
@Composable
fun MeetingCallEndButton(
    label: String,
    isEnding: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val endingState = stringResource(R.string.call_ending_state)
    val container = MaterialTheme.extras.destructive
    val content = MaterialTheme.extras.onDestructive
    PressScaleButton(
        onClick = { if (!isEnding) onClick() },
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = EndButtonMinHeight)
                .testTag(CALL_END_BUTTON_TAG)
                .semantics { if (isEnding) stateDescription = endingState },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container,
                disabledContentColor = content,
            ),
    ) {
        if (isEnding) {
            CircularProgressIndicator(Modifier.size(EndIconSize), color = content, strokeWidth = SpinnerStroke)
        } else {
            Icon(Icons.Filled.CallEnd, contentDescription = null, modifier = Modifier.size(EndIconSize))
            Spacer(Modifier.width(Spacing.sm))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "MeetingCallEndButton")
@Composable
private fun MeetingCallEndButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MeetingCallEndButton(label = "End Call", isEnding = false, onClick = {})
        }
    }
}
