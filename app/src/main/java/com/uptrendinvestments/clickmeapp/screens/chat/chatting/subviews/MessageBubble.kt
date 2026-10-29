package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
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
private val FailedIconSize = 16.dp
private const val READ_FADE_MS = 150
private const val FAILED_FILL_ALPHA = 0.4f
private const val FAILED_BORDER_ALPHA = 0.5f

/**
 * One chat message bubble (UI-SPEC surface 2): own messages on the end in `primary`, incoming on the start in
 * `surfaceContainerHigh` with the peer avatar at the end of a run. Text is selectable and plain (no link detection,
 * T-12-27). The row is one merged node read as "{You|name}: {text}", then the time and, on my messages, the status
 * ("Sent", "Read", "Failed to send"; UI-SPEC Flow 4). [announceStatus] makes that node a polite live region: the
 * caller sets it only on the newest message of mine, so a Sent to Read change is spoken once.
 */
@Composable
fun MessageBubble(
    message: ThreadItem.Message,
    peerName: String?,
    peerAvatarUrl: String?,
    showAvatar: Boolean,
    timeText: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    onDelete: () -> Unit = {},
    announceStatus: Boolean = false,
) {
    val failed = message.mine && message.delivery == Delivery.Failed
    var menuOpen by remember { mutableStateOf(false) }
    val retryLabel = stringResource(R.string.chat_failed_retry)
    val deleteLabel = stringResource(R.string.chat_failed_delete_a11y)
    val speaker =
        if (message.mine) {
            stringResource(R.string.chat_thread_you)
        } else {
            peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_thread_title_fallback)
        }
    val failedStatus = stringResource(R.string.chat_failed_status_a11y)
    val spoken = stringResource(R.string.chat_thread_bubble_a11y, speaker, message.text)
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
                        .then(
                            if (failed) {
                                Modifier.combinedClickable(
                                    onClickLabel = retryLabel,
                                    role = Role.Button,
                                    onClick = onRetry,
                                    onLongClick = { menuOpen = true },
                                )
                            } else {
                                Modifier
                            },
                        ).semantics(mergeDescendants = true) {
                            contentDescription = if (failed) "$spoken, $failedStatus" else spoken
                            if (announceStatus) liveRegion = LiveRegionMode.Polite
                            if (failed) {
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction(retryLabel) {
                                            onRetry()
                                            true
                                        },
                                        CustomAccessibilityAction(deleteLabel) {
                                            onDelete()
                                            true
                                        },
                                    )
                            }
                        },
                horizontalAlignment = if (message.mine) Alignment.End else Alignment.Start,
            ) {
                BubbleBody(message)
                Spacer(Modifier.height(Spacing.xs))
                MessageStatusLine(message = message, timeText = timeText)
                if (failed) {
                    FailedMessageMenu(
                        expanded = menuOpen,
                        onDelete = {
                            menuOpen = false
                            onDelete()
                        },
                        onDismiss = { menuOpen = false },
                    )
                }
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
    val failed = mine && message.delivery == Delivery.Failed
    val scheme = MaterialTheme.colorScheme
    val medium = MaterialTheme.shapes.medium
    val shape =
        if (mine) {
            medium.copy(bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd)
        } else {
            medium.copy(bottomStart = MaterialTheme.shapes.extraSmall.bottomStart)
        }
    // A failed bubble is tinted (never faded as a whole, so the text keeps its contrast) with an error border.
    Surface(
        shape = shape,
        color =
            when {
                failed -> scheme.primary.copy(alpha = FAILED_FILL_ALPHA)
                mine -> scheme.primary
                else -> scheme.surfaceContainerHigh
            },
        contentColor = if (mine && !failed) scheme.onPrimary else scheme.onSurface,
        border = if (failed) BorderStroke(1.dp, scheme.error.copy(alpha = FAILED_BORDER_ALPHA)) else null,
    ) {
        val body: @Composable () -> Unit = {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        // Selection would swallow the long-press that opens the failed bubble's Delete menu.
        if (failed) body() else SelectionContainer(content = body)
    }
}

/**
 * The line under a bubble: the time, "Sending…" while sending, and the chat_thread_read_separator plus Read (Read in
 * `primary`, 150 ms fade) on my read messages. TalkBack reads the row as one phrase: "{time}, Read" on my read
 * messages, "{time}, Sent" on my other sent ones and the bare time on theirs.
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
        if (message.mine && message.delivery == Delivery.Failed) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(FailedIconSize),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.chat_failed_status),
                style = style,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (message.delivery == Delivery.Sending) {
            Text(text = stringResource(R.string.chat_thread_sending), style = style, color = muted)
        } else {
            val showRead = message.mine && message.delivery == Delivery.Read
            val statusDescription =
                when {
                    showRead -> stringResource(R.string.chat_thread_read_status_a11y, timeText)
                    message.mine -> stringResource(R.string.chat_thread_sent_status_a11y, timeText)
                    else -> timeText
                }
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = statusDescription },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = timeText, style = style, color = muted)
                AnimatedVisibility(
                    visible = showRead,
                    enter = fadeIn(tween(READ_FADE_MS)),
                    exit = fadeOut(tween(READ_FADE_MS)),
                ) {
                    Row {
                        Text(text = stringResource(R.string.chat_thread_read_separator), style = style, color = muted)
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
}
