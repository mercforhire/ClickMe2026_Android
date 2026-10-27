package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.EndingSoonBanner
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ExternalLinkActions
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ExternalSessionHero
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ExternalStatusText
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallAvatar
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallBackground
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallControls
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallEndButton
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallInfo
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallTimer
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.callStatusText
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.time.ZoneId

const val MEETING_CALL_SCREEN_TAG = "meeting_call_screen"

/** Cross-fade when the variant changes after the join response overrides the client-side hint (D-05). */
private const val VARIANT_FADE_MS = 200

/**
 * The expanded call layer for both variants (UI-SPEC surfaces 1 and 2). Stateless: the overlay host maps the live
 * session with [toMeetingCallUiState] and wires the callbacks.
 *
 * The layer is full screen and applies its own edge-to-edge insets (D-08): `safeDrawing` at the top and sides,
 * `navigationBars` at the bottom. The column is a min-height-fill scroll: a normal phone gets the weighted spacer, and
 * at 200% font scale the whole column scrolls so End Call stays reachable.
 */
@Composable
fun MeetingCallScreen(
    state: MeetingCallUiState,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier,
    chatBusy: Boolean = false,
    onChat: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag(MEETING_CALL_SCREEN_TAG)) {
        MeetingCallBackground()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .semantics { isTraversalGroup = true },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
            Spacer(Modifier.height(Spacing.md))
            EndingSoonBanner(state.timer)
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = state.connectionType,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    fadeIn(tween(VARIANT_FADE_MS)) togetherWith fadeOut(tween(VARIANT_FADE_MS))
                },
                label = "callVariant",
            ) { variant ->
                when (variant) {
                    ConnectionType.InAppVoice ->
                        InAppContent(
                            state = state,
                            onMinimize = onMinimize,
                            onToggleMute = onToggleMute,
                            onToggleSpeaker = onToggleSpeaker,
                            onEndCall = onEndCall,
                            onChat = onChat,
                            chatBusy = chatBusy,
                            zone = zone,
                            timeFormat = timeFormat,
                        )
                    ConnectionType.SkypeZoom ->
                        ExternalContent(
                            state = state,
                            onMinimize = onMinimize,
                            onEndCall = onEndCall,
                            onOpenExternal = onOpenExternal,
                            onChat = onChat,
                            chatBusy = chatBusy,
                            zone = zone,
                            timeFormat = timeFormat,
                        )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun InAppContent(
    state: MeetingCallUiState,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    onChat: () -> Unit,
    chatBusy: Boolean,
    zone: ZoneId,
    timeFormat: DateFormat,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MeetingCallAvatar(
            status = state.status,
            peerAvatarUrl = state.peerAvatarUrl,
            myAvatarUrl = state.myAvatarUrl,
        )
        Spacer(Modifier.height(Spacing.xl))
        MeetingCallInfo(
            peerName = state.peerName,
            topic = state.topic,
            scheduledStart = state.scheduledStart,
            scheduledEnd = state.scheduledEnd,
            timer = state.timer,
            statusText = callStatusText(state.status),
            zone = zone,
            timeFormat = timeFormat,
        )
        Spacer(Modifier.height(Spacing.lg))
        MeetingCallControls(
            showAudioControls = true,
            isMuted = state.isMuted,
            isSpeakerOn = state.isSpeakerOn,
            onMinimize = onMinimize,
            onToggleMute = onToggleMute,
            onToggleSpeaker = onToggleSpeaker,
            onChat = onChat,
            chatEnabled = state.peerId != null,
            chatBusy = chatBusy,
            audioControlsEnabled = !state.isEndingCall,
        )
        Spacer(Modifier.height(Spacing.xl))
        MeetingCallEndButton(
            label = stringResource(R.string.call_end_call),
            isEnding = state.isEndingCall,
            onClick = onEndCall,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
    }
}

@Composable
private fun ExternalContent(
    state: MeetingCallUiState,
    onMinimize: () -> Unit,
    onEndCall: () -> Unit,
    onOpenExternal: () -> Unit,
    onChat: () -> Unit,
    chatBusy: Boolean,
    zone: ZoneId,
    timeFormat: DateFormat,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExternalSessionHero(
            peerName = state.peerName,
            peerAvatarUrl = state.peerAvatarUrl,
            topic = state.topic,
            scheduledStart = state.scheduledStart,
            scheduledEnd = state.scheduledEnd,
            modifier = Modifier.padding(horizontal = Spacing.lg),
            zone = zone,
            timeFormat = timeFormat,
        )
        Spacer(Modifier.height(Spacing.sm))
        MeetingCallTimer(state.timer)
        Spacer(Modifier.height(Spacing.lg))
        ExternalLinkActions(
            provider = state.provider,
            joinUrl = state.externalJoinUrl,
            onOpen = onOpenExternal,
        )
        Spacer(Modifier.height(Spacing.lg))
        ExternalStatusText(
            hasOpened = state.hasOpenedExternalLink,
            scheduledEnd = state.scheduledEnd,
            zone = zone,
            timeFormat = timeFormat,
        )
        Spacer(Modifier.height(Spacing.lg))
        MeetingCallControls(
            showAudioControls = false,
            isMuted = state.isMuted,
            isSpeakerOn = state.isSpeakerOn,
            onMinimize = onMinimize,
            onToggleMute = {},
            onToggleSpeaker = {},
            onChat = onChat,
            chatEnabled = state.peerId != null,
            chatBusy = chatBusy,
        )
        Spacer(Modifier.height(Spacing.xl))
        MeetingCallEndButton(
            label = stringResource(R.string.call_end_session),
            isEnding = state.isEndingCall,
            onClick = onEndCall,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
    }
}
