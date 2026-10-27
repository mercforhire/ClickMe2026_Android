package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.Delivery
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ThreadItem
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val BubbleMaxWidth = 320.dp
private val OppositeGap = 56.dp
private val BubbleAvatarSize = 32.dp
private const val BUBBLE_MAX_FRACTION = 0.75f
private const val READ_FADE_MS = 150

/**
 * One chat message bubble (UI-SPEC surface 2): own messages on the end in `primary`, incoming on the start in
 * `surfaceContainerHigh` with the peer avatar at the end of a run. Text is selectable and plain (no link detection,
 * T-12-27). The row is one merged node read as "{You|name}, {text}, {time or Sending}, {Read}".
 */
@Composable
fun MessageBubble(
    message: ThreadItem.Message,
    peerName: String?,
    peerAvatarUrl: String?,
    showAvatar: Boolean,
    timeText: String,
    modifier: Modifier = Modifier,
) {
    val speaker =
        if (message.mine) {
            stringResource(R.string.chat_thread_you)
        } else {
            peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_thread_title_fallback)
        }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val bubbleMax = bubbleMaxWidth(maxWidth, message.mine)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!message.mine) {
                if (showAvatar) {
                    Avatar(url = peerAvatarUrl, contentDescription = null, size = BubbleAvatarSize)
                } else {
                    Spacer(Modifier.width(BubbleAvatarSize))
                }
                Spacer(Modifier.width(Spacing.sm))
            }
            Column(
                modifier =
                    Modifier
                        .widthIn(max = bubbleMax)
                        .semantics(mergeDescendants = true) { contentDescription = speaker },
                horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start,
            ) {
                BubbleBody(message)
                Spacer(Modifier.height(Spacing.xs))
                MessageStatusLine(message = message, timeText = timeText)
            }
        }
    }
}

private fun bubbleMaxWidth(
    rowWidth: Dp,
    mine: Boolean,
): Dp {
    val byFraction = rowWidth * BUBBLE_MAX_FRACTION
    val byGap = if (mine) rowWidth - OppositeGap else rowWidth - OppositeGap - BubbleAvatarSize - Spacing.sm
    return minOf(BubbleMaxWidth, byFraction, byGap)
}

@Composable
private fun BubbleBody(message: ThreadItem.Message) {
    val mine = message.mine
    val medium = MaterialTheme.shapes.medium
    val shape =
        if (mine) {
            medium.copy(bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd)
        } else {
            medium.copy(bottomStart = MaterialTheme.shapes.extraSmall.bottomStart)
        }
    Surface(
        shape = shape,
        color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        SelectionContainer {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * The line under a bubble: the time, "Sending…" while sending, and " · Read" (Read in `primary`, 150 ms fade) on my
 * read messages.
 */
@Composable
fun MessageStatusLine(
    message: ThreadItem.Message,
    timeText: String,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.extras.onSurfaceMuted
    val style = MaterialTheme.typography.bodySmall
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (message.delivery == Delivery.Sending) {
            Text(text = stringResource(R.string.chat_thread_sending), style = style, color = muted)
        } else {
            Text(text = timeText, style = style, color = muted)
            AnimatedVisibility(
                visible = message.mine && message.delivery == Delivery.Read,
                enter = fadeIn(tween(READ_FADE_MS)),
                exit = fadeOut(tween(READ_FADE_MS)),
            ) {
                Row {
                    Text(text = " · ", style = style, color = muted)
                    Text(
                        text = stringResource(R.string.chat_thread_read),
                        style = style,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
