package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.net.Uri
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.components.chat.ChatEntryErrorDialog
import com.uptrendinvestments.clickmeapp.components.web.rememberExternalMeetingLauncher
import com.uptrendinvestments.clickmeapp.managers.CallDialog
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import com.uptrendinvestments.clickmeapp.managers.PendingMicPermission
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MeetingCallPill
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatPeer
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlin.coroutines.cancellation.CancellationException

const val CALL_OVERLAY_LAYER_TAG = "call_overlay_layer"
const val CALL_OVERLAY_CONTENT_TAG = "call_overlay_content"

/** Distance of the pill above the navigation-bar inset: clears the bottom navigation (iOS `.padding(.bottom, 96)`). */
val PILL_BOTTOM_OFFSET = 96.dp

private const val PILL_HIDDEN_SCALE = 0.6f

/** How far the expanded layer follows a predictive-back gesture, as a fraction of its height. */
private const val BACK_TRANSLATION_FRACTION = 0.2f

/**
 * Hilt-aware root call overlay (D-08). Mounted once around the route switch so the expanded call and the pill
 * survive every tab change and the Client/Expert mode switch; the call itself lives in `CallCenter`.
 *
 * The Chat control opens the thread with the counterpart through [chatEntry] and minimizes the call once the thread
 * was handed to the Chats tab (Phase 9 D-11); the call keeps running in the pill. `isExpert` is null because the call
 * layer does not know the mode; the thread fills it from the scanned row.
 */
@Composable
fun CallOverlayHost(
    modifier: Modifier = Modifier,
    viewModel: MeetingCallViewModel = hiltViewModel(),
    chatEntry: ChatEntryViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isMinimized by viewModel.isMinimized.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val pendingPermission by viewModel.pendingPermission.collectAsStateWithLifecycle()
    val entry by chatEntry.state.collectAsStateWithLifecycle()
    // App first, Custom Tab fallback (D-06). A link nothing can open shows a dialog (WR-07).
    val launchMeeting = rememberExternalMeetingLauncher(onFailure = viewModel::onExternalLinkFailed)
    CallOverlayContent(
        state = state,
        isMinimized = isMinimized,
        dialog = dialog,
        pendingPermission = pendingPermission,
        onMinimize = viewModel::minimize,
        onExpand = viewModel::expand,
        onToggleMute = viewModel::toggleMute,
        onToggleSpeaker = viewModel::toggleSpeaker,
        onEndCall = viewModel::endCall,
        onOpenExternal = {
            val opened = state?.externalJoinUrl?.let { launchMeeting(Uri.parse(it)) } == true
            // Confirm once and minimize only when something opened the link; a refused or unhandled link is not
            // "opened" (WR-07, unlike iOS where UIApplication.open always hands off).
            if (opened) viewModel.onExternalLinkOpened()
        },
        onDismissDialog = viewModel::dismissDialog,
        onPermissionResult = viewModel::onPermissionResult,
        modifier = modifier,
        chatBusy = entry.busyPeerId != null,
        onChat = {
            state?.peerId?.let { peerId ->
                chatEntry.open(
                    ChatPeer(peerId, state?.peerName, state?.peerAvatarUrl, isExpert = null),
                    onOpened = viewModel::minimize,
                )
            }
        },
        chatError = entry.error,
        onDismissChatError = chatEntry::dismissError,
        content = content,
    )
}

/**
 * Stateless overlay: [content] below, the expanded call layer or the minimized pill above it, then the mic
 * requester and the call dialogs.
 *
 * Expanded: the layer swallows touches and the content below is hidden from accessibility. Back minimizes and
 * never ends the call (D-09). The back handler is composed only while expanded so it registers after every
 * handler the content composed earlier and therefore runs first (RESEARCH Pitfall 2). While only the pill shows,
 * no handler is composed here, so Back belongs to the screen below.
 *
 * The content and the pill each pad the horizontal display cutout once (UI-SPEC R5, POL-03); the expanded layer pads
 * its own sides through `MeetingCallScreen`'s `safeDrawing` padding, so it gets no second consumer here.
 *
 * Dialog windows draw above the activity root, so the pill cannot sit above an open sheet or dialog (RESEARCH
 * Pattern 7; no Join site is inside a sheet).
 */
@Composable
fun CallOverlayContent(
    state: MeetingCallUiState?,
    isMinimized: Boolean,
    dialog: CallDialog?,
    pendingPermission: PendingMicPermission?,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismissDialog: () -> Unit,
    onPermissionResult: (Long, MicPermissionResult) -> Unit,
    modifier: Modifier = Modifier,
    chatBusy: Boolean = false,
    onChat: () -> Unit = {},
    chatError: FieldError? = null,
    onDismissChatError: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val expanded = state != null && !isMinimized
    // The exit animations need the last state after it became null.
    val lastState = remember { arrayOfNulls<MeetingCallUiState>(1) }
    if (state != null) lastState[0] = state
    val shown = state ?: lastState[0]
    var backProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(expanded) { if (expanded) backProgress = 0f }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .testTag(CALL_OVERLAY_CONTENT_TAG)
                .then(if (expanded) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            content()
        }

        AnimatedVisibility(
            visible = state != null && isMinimized,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .padding(end = Spacing.md)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = PILL_BOTTOM_OFFSET),
            enter =
                scaleIn(
                    initialScale = PILL_HIDDEN_SCALE,
                    animationSpec = tween(Motion.STRENGTH_METER_VISIBILITY_MS),
                ) + fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
            exit =
                scaleOut(
                    targetScale = PILL_HIDDEN_SCALE,
                    animationSpec = tween(Motion.STRENGTH_METER_VISIBILITY_MS),
                ) + fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
        ) {
            shown?.let { MeetingCallPill(it.peerName, it.peerAvatarUrl, it.timer, onExpand) }
        }

        AnimatedVisibility(
            visible = expanded,
            enter =
                slideInVertically(tween(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)) { it } +
                    fadeIn(tween(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)),
            exit =
                slideOutVertically(tween(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)) { it } +
                    fadeOut(tween(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag(CALL_OVERLAY_LAYER_TAG)
                    .graphicsLayer { translationY = size.height * backProgress * BACK_TRANSLATION_FRACTION }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
            ) {
                shown?.let {
                    MeetingCallScreen(
                        state = it,
                        onMinimize = onMinimize,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onEndCall = onEndCall,
                        onOpenExternal = onOpenExternal,
                        chatBusy = chatBusy,
                        onChat = onChat,
                    )
                }
            }
        }

        // Composed only while expanded so it is the most recently registered callback (Pitfall 2).
        if (expanded) {
            PredictiveBackHandler { progress ->
                try {
                    progress.collect { event -> backProgress = event.progress }
                    onMinimize()
                } catch (e: CancellationException) {
                    backProgress = 0f
                    throw e
                }
            }
        }

        MicPermissionRequester(pendingPermission, onPermissionResult)
        CallDialogHost(dialog, onDismissDialog)
        ChatEntryErrorDialog(chatError, onDismissChatError)
    }
}
