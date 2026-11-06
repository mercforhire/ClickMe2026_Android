package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

const val MEETING_CALL_HERO_SINGLE_TAG = "meeting_call_hero_single"
const val MEETING_CALL_HERO_PAIR_TAG = "meeting_call_hero_pair"

private const val RING_PULSE_SCALE = 1.06f
private const val GLOW_ALPHA_MIN = 0.10f
private const val GLOW_ALPHA_MAX = 0.28f
private val HeroMaxWidth = 320.dp
private val GlowSize = 360.dp
private val SingleAvatarSize = 160.dp
private val PairAvatarSize = 96.dp

/** Below this window height the hero shrinks and the call layer compacts (UI-SPEC "Large screens", D-02). */
internal val ShortWindowHeight = 480.dp

/** The hero avatar never shrinks below this on a short window. */
private val MinAvatarSize = 96.dp

/**
 * An avatar inside a primary ring. [pulsing] scales the ring between 1.0 and 1.06 (UI-SPEC Motion); otherwise it is
 * static. Decorative: the avatar carries no content description because the name is read from the info block.
 */
@Composable
fun CallRingAvatar(
    url: String?,
    size: Dp,
    ringWidth: Dp,
    ringGap: Dp,
    pulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale =
        if (pulsing) {
            val transition = rememberInfiniteTransition(label = "callRingPulse")
            val animated by transition.animateFloat(
                initialValue = 1f,
                targetValue = RING_PULSE_SCALE,
                animationSpec = infiniteRepeatable(tween(Motion.CALL_RING_PULSE_MS), RepeatMode.Reverse),
                label = "callRingScale",
            )
            animated
        } else {
            1f
        }
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.border(ringWidth, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(ringWidth + ringGap),
    ) {
        Avatar(url = url, contentDescription = null, size = size)
    }
}

/**
 * The call hero. One 160dp peer avatar with a pulsing ring while connecting or waiting; a 300 ms cross-fade to the
 * two-avatar pair (mine left, peer right, 96dp) once connected. The ring is static when connected or ended.
 *
 * In a window shorter than [ShortWindowHeight] the single avatar and the glow scale down with the window height; the
 * avatar never goes below 96dp (the pair is already 96dp) so Mute, Speaker and End call stay on screen.
 */
@Composable
fun MeetingCallAvatar(
    status: CallStatus,
    peerAvatarUrl: String?,
    myAvatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val windowHeight = LocalConfiguration.current.screenHeightDp.dp
    val heightFactor = if (windowHeight < ShortWindowHeight) windowHeight / ShortWindowHeight else 1f
    val singleSize = (SingleAvatarSize * heightFactor).coerceAtLeast(MinAvatarSize)
    val glowSize = (GlowSize * heightFactor).coerceAtLeast(MinAvatarSize)
    val glowTransition = rememberInfiniteTransition(label = "callGlow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = GLOW_ALPHA_MIN,
        targetValue = GLOW_ALPHA_MAX,
        animationSpec = infiniteRepeatable(tween(Motion.CALL_GLOW_PULSE_MS), RepeatMode.Reverse),
        label = "callGlowAlpha",
    )
    // The glow is drawn behind the hero and outside its bounds: it must not reserve layout height, or the 360 dp box
    // pushes the call controls behind the pinned End Call (BUG-22).
    Box(
        modifier =
            modifier
                .widthIn(max = HeroMaxWidth)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = glowAlpha), primary.copy(alpha = 0f)),
                            center = center,
                            radius = glowSize.toPx() / 2f,
                        ),
                        radius = glowSize.toPx() / 2f,
                        center = center,
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = status == CallStatus.Active,
            animationSpec = tween(Motion.PUSH_POP_DURATION_MS),
            label = "callHero",
        ) { connected ->
            if (connected) {
                Row(
                    modifier = Modifier.testTag(MEETING_CALL_HERO_PAIR_TAG),
                    horizontalArrangement = Arrangement.spacedBy(-Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CallRingAvatar(myAvatarUrl, PairAvatarSize, 3.dp, Spacing.xs, pulsing = false)
                    CallRingAvatar(peerAvatarUrl, PairAvatarSize, 3.dp, Spacing.xs, pulsing = false)
                }
            } else {
                CallRingAvatar(
                    url = peerAvatarUrl,
                    size = singleSize,
                    ringWidth = 4.dp,
                    ringGap = Spacing.sm,
                    pulsing = status == CallStatus.Connecting || status == CallStatus.WaitingForPeer,
                    modifier = Modifier.testTag(MEETING_CALL_HERO_SINGLE_TAG),
                )
            }
        }
    }
}
