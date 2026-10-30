package com.uptrendinvestments.clickmeapp.components.call

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the one-time "You were on a call" note. */
const val CALL_RESUME_NOTE_TAG = "call_resume_note"

/**
 * The one-time note both booking detail screens show above their join control after the app was killed during a call
 * (CONTEXT D-11, UI-SPEC A4). It only explains; Join Call stays the user's own tap. A polite live region, so TalkBack
 * announces it once when the screen appears.
 */
@Composable
fun CallResumeNote(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.call_resume_note),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.sm)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(CALL_RESUME_NOTE_TAG),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
