package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the Starting now badge of a Today's Sessions card. */
const val STARTING_NOW_BADGE_TAG = "starting_now_badge"

/**
 * "Starting now" pill shown on a Today's Sessions card whose start is within the join window (iOS `isStartingNow`).
 * Sentence case, no shadow (UI-SPEC surface 6); the iOS all-caps "STARTING NOW" is deliberately not ported.
 */
@Composable
fun StartingNowBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag(STARTING_NOW_BADGE_TAG),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = stringResource(R.string.call_starting_now),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
    }
}

@Preview(name = "StartingNowBadge")
@Composable
private fun StartingNowBadgePreview() {
    ClickMeTheme { StartingNowBadge() }
}
