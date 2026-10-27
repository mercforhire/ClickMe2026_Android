package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

const val CALL_CONTROL_MINIMIZE_TAG = "call_control_minimize"
const val CALL_CONTROL_CHAT_TAG = "call_control_chat"
const val CALL_CONTROL_MUTE_TAG = "call_control_mute"
const val CALL_CONTROL_SPEAKER_TAG = "call_control_speaker"

private val CircleSize = 64.dp
private val IconSize = 24.dp
private val MinControlWidth = 48.dp
private const val DISABLED_ALPHA = 0.38f
private const val ON_BORDER_ALPHA = 0.5f

/**
 * One round control with a label underneath (iOS `MeetingCallControlButton`). A [toggle] control carries
 * [stateDescription] and `Role.Switch` semantics; a plain control is a button. [isOn] only changes the look (primary
 * icon and border); state is also exposed through [stateDescription] and an icon swap, never color alone.
 */
@Composable
fun MeetingCallControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOn: Boolean = false,
    stateDescription: String? = null,
    contentDescription: String? = null,
    toggle: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        targetValue = if (isOn) colors.primary.copy(alpha = ON_BORDER_ALPHA) else colors.outlineVariant,
        animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
        label = "controlBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isOn) colors.primary else colors.onSurface,
        animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
        label = "controlIcon",
    )
    val semanticsModifier =
        Modifier.semantics(mergeDescendants = true) {
            if (stateDescription != null) this.stateDescription = stateDescription
            if (contentDescription != null) this.contentDescription = contentDescription
        }
    val interaction =
        if (toggle) {
            Modifier.toggleable(value = isOn, enabled = enabled, role = Role.Switch) { onClick() }
        } else {
            Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        }

    Column(
        modifier =
            modifier
                .widthIn(min = MinControlWidth)
                .then(semanticsModifier)
                .then(interaction),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(CircleSize),
            shape = CircleShape,
            color = colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(
                    targetState = icon,
                    animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                    label = "controlIconSwap",
                ) { vector ->
                    Icon(
                        imageVector = vector,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize).alpha(if (enabled) 1f else DISABLED_ALPHA),
                        tint = iconTint,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        )
    }
}

/**
 * The control row. In-app: Minimize, Chat, Mute, Speaker spread evenly (iOS layout). External
 * ([showAudioControls] false): Minimize and Chat only, centered, since there is no in-app audio (D-05). Chat opens the
 * thread with the counterpart (D-11): it is enabled when [chatEnabled] (the peer id is known) and ignores taps while
 * [chatBusy]. Mute and Speaker are disabled when [audioControlsEnabled] is false (while the call is ending, WR-04).
 */
@Composable
fun MeetingCallControls(
    showAudioControls: Boolean,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onChat: () -> Unit,
    chatEnabled: Boolean,
    chatBusy: Boolean,
    modifier: Modifier = Modifier,
    audioControlsEnabled: Boolean = true,
) {
    val arrangement =
        if (showAudioControls) {
            Arrangement.spacedBy(Spacing.md)
        } else {
            Arrangement.spacedBy(Spacing.xl, Alignment.CenterHorizontally)
        }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top,
    ) {
        val cell = if (showAudioControls) Modifier.weight(1f) else Modifier
        MeetingCallControlButton(
            icon = Icons.Outlined.KeyboardArrowDown,
            label = stringResource(R.string.call_control_minimize),
            onClick = onMinimize,
            modifier = cell.testTag(CALL_CONTROL_MINIMIZE_TAG),
            contentDescription = stringResource(R.string.call_control_minimize_a11y),
        )
        MeetingCallControlButton(
            icon = Icons.Outlined.Forum,
            label = stringResource(R.string.call_control_chat),
            onClick = { if (!chatBusy) onChat() },
            modifier = cell.testTag(CALL_CONTROL_CHAT_TAG),
            enabled = chatEnabled,
        )
        if (showAudioControls) {
            MeetingCallControlButton(
                icon = if (isMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                label = stringResource(R.string.call_control_mute),
                onClick = onToggleMute,
                modifier = cell.testTag(CALL_CONTROL_MUTE_TAG),
                enabled = audioControlsEnabled,
                isOn = isMuted,
                stateDescription =
                    stringResource(if (isMuted) R.string.call_state_muted else R.string.call_state_unmuted),
                toggle = true,
            )
            MeetingCallControlButton(
                icon =
                    if (isSpeakerOn) {
                        Icons.AutoMirrored.Outlined.VolumeUp
                    } else {
                        Icons.AutoMirrored.Outlined.VolumeDown
                    },
                label = stringResource(R.string.call_control_speaker),
                onClick = onToggleSpeaker,
                modifier = cell.testTag(CALL_CONTROL_SPEAKER_TAG),
                enabled = audioControlsEnabled,
                isOn = isSpeakerOn,
                stateDescription = stringResource(if (isSpeakerOn) R.string.call_state_on else R.string.call_state_off),
                toggle = true,
            )
        }
    }
}

@Preview(name = "MeetingCallControls")
@Composable
private fun MeetingCallControlsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                MeetingCallControls(true, isMuted = false, isSpeakerOn = true, {}, {}, {}, {}, true, false)
                MeetingCallControls(false, isMuted = false, isSpeakerOn = false, {}, {}, {}, {}, false, false)
            }
        }
    }
}
