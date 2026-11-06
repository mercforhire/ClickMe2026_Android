package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_END_TRAVERSAL_INDEX
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
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.ShortWindowHeight
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.callStatusText
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.time.ZoneId

const val MEETING_CALL_SCREEN_TAG = "meeting_call_screen"

/** The scrolling column above the pinned End Call; the reachability tests swipe it. */
const val CALL_SCROLL_CONTENT_TAG = "call_scroll_content"

/** Cross-fade when the variant changes after the join response overrides the client-side hint (D-05). */
private const val VARIANT_FADE_MS = 200

/** A short window at least this wide puts the hero beside the controls instead of above them. */
private val SideBySideMinWidth = 560.dp

/**
 * The expanded call layer for both variants (UI-SPEC surfaces 1 and 2). Stateless: the overlay host maps the live
 * session with [toMeetingCallUiState] and wires the callbacks.
 *
 * The layer is full screen and applies its own edge-to-edge insets (D-08): `safeDrawing` at the top and sides,
 * `navigationBars` at the bottom. End Call is pinned at the bottom, above the navigation bar (BUG-10). Everything above
 * it is a min-height-fill scroll: a normal phone gets the weighted spacer, and when the content is taller than the
 * space (the ending-soon banner with the time-left chip, 200% font scale) it scrolls while End Call stays on screen.
 *
 * TalkBack reads the layer in the order of UI-SPEC Flow 3 through traversal indexes inside a traversal group. The
 * variant content is its own group because `AnimatedContent` puts a semantics node between it and the root column,
 * and indexes only sort the direct children of a group: status line (-1), name, timer, Mute and Speaker (0), Chat (1), Minimize (2), End call (3, always last). On a
 * window shorter than 480 dp the hero shrinks, the gaps tighten and, when the window is also wide, the hero sits beside
 * the controls so Mute, Speaker and End call stay on screen.
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
    val endLabel =
        if (state.connectionType == ConnectionType.SkypeZoom) R.string.call_end_session else R.string.call_end_call
    Box(modifier = modifier.fillMaxSize().testTag(MEETING_CALL_SCREEN_TAG)) {
        MeetingCallBackground()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .semantics { isTraversalGroup = true },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .testTag(CALL_SCROLL_CONTENT_TAG)
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
                                    onChat = onChat,
                                    chatBusy = chatBusy,
                                    zone = zone,
                                    timeFormat = timeFormat,
                                )
                            ConnectionType.SkypeZoom ->
                                ExternalContent(
                                    state = state,
                                    onMinimize = onMinimize,
                                    onOpenExternal = onOpenExternal,
                                    onChat = onChat,
                                    chatBusy = chatBusy,
                                    zone = zone,
                                    timeFormat = timeFormat,
                                )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xl))
                }
            }
            // BUG-10: End Call sits below the scrolling content, so a tall content column (the ending-soon banner and
            // time-left chip, a large font) scrolls above it instead of pushing it off screen or under the navigation bar.
            MeetingCallEndButton(
                label = stringResource(endLabel),
                isEnding = state.isEndingCall,
                onClick = onEndCall,
                modifier =
                    Modifier
                        .padding(horizontal = Spacing.lg)
                        .semantics { traversalIndex = CALL_END_TRAVERSAL_INDEX },
            )
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
    onChat: () -> Unit,
    chatBusy: Boolean,
    zone: ZoneId,
    timeFormat: DateFormat,
) {
    val configuration = LocalConfiguration.current
    val shortWindow = configuration.screenHeightDp.dp < ShortWindowHeight
    val sideBySide = shortWindow && configuration.screenWidthDp.dp >= SideBySideMinWidth
    val hero: @Composable () -> Unit = {
        MeetingCallAvatar(
            status = state.status,
            peerAvatarUrl = state.peerAvatarUrl,
            myAvatarUrl = state.myAvatarUrl,
        )
    }
    val panel: @Composable (Modifier) -> Unit = { panelModifier ->
        InAppPanel(
            state = state,
            compact = shortWindow,
            onMinimize = onMinimize,
            onToggleMute = onToggleMute,
            onToggleSpeaker = onToggleSpeaker,
            onChat = onChat,
            chatBusy = chatBusy,
            zone = zone,
            timeFormat = timeFormat,
            modifier = panelModifier,
        )
    }
    if (sideBySide) {
        // A short, wide window cannot stack the hero over the controls: the hero moves beside them so Mute, Speaker and
        // End call stay on screen. Semantics are unchanged: the nodes are still siblings in the layer's traversal group.
        Row(
            modifier = Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { hero() }
            panel(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            hero()
            Spacer(Modifier.height(if (shortWindow) Spacing.md else Spacing.xl))
            panel(Modifier.fillMaxWidth())
        }
    }
}

/** Name, status and controls. [compact] tightens the vertical gaps on a short window (below 480 dp). */
@Composable
private fun InAppPanel(
    state: MeetingCallUiState,
    compact: Boolean,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onChat: () -> Unit,
    chatBusy: Boolean,
    zone: ZoneId,
    timeFormat: DateFormat,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
        Spacer(Modifier.height(if (compact) Spacing.md else Spacing.lg))
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
    }
}

@Composable
private fun ExternalContent(
    state: MeetingCallUiState,
    onMinimize: () -> Unit,
    onOpenExternal: () -> Unit,
    onChat: () -> Unit,
    chatBusy: Boolean,
    zone: ZoneId,
    timeFormat: DateFormat,
) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
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
    }
}
