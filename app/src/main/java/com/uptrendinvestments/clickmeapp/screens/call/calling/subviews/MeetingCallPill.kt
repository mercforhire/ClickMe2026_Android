package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.screens.call.calling.callTimerText
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

const val MEETING_CALL_PILL_TAG = "meeting_call_pill"

private const val PILL_BORDER_ALPHA = 0.55f

/**
 * The minimized call: peer avatar in a primary ring plus the live timer. Tapping it expands the call; it never ends
 * it (CALL-04, iOS `MeetingCallPill`). The whole pill is one merged button reading "Return to call with {name}" so
 * the ticking timer, which is not a live region, never floods TalkBack. Appear and disappear animation belongs to
 * the overlay host.
 */
@Composable
fun MeetingCallPill(
    peerName: String?,
    peerAvatarUrl: String?,
    timer: CallTimerState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clickLabel = stringResource(R.string.call_pill_click_label)
    val description =
        if (peerName.isNullOrBlank()) clickLabel else stringResource(R.string.call_pill_a11y, peerName)
    Surface(
        onClick = onTap,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = PILL_BORDER_ALPHA)),
        shadowElevation = 8.dp,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .testTag(MEETING_CALL_PILL_TAG)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    role = Role.Button
                    onClick(label = clickLabel) {
                        onTap()
                        true
                    }
                },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallRingAvatar(peerAvatarUrl, 32.dp, 2.dp, 2.dp, pulsing = false)
            Text(
                text = callTimerText(timer),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color =
                    if (timer.isWarning) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}
