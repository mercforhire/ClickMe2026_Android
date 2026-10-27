package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val LoadEarlierMinHeight = 48.dp
private val LoadEarlierGlyph = 16.dp
private val LoadEarlierStroke = 2.dp
private const val TYPING_VISIBILITY_MS = 200
private const val JUMP_FADE_MS = 150

/** A centered date label ("Today", "Yesterday" or a medium date) between messages of different local days. */
@Composable
fun DateSeparator(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = Spacing.md), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

/**
 * "Load earlier" at the top of the list while more pages exist. While [loading] it shows a spinner and "Loading…" and
 * is disabled; a failure is silent, so the button simply becomes tappable again.
 */
@Composable
fun LoadEarlierButton(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(
            onClick = onClick,
            enabled = !loading,
            modifier = Modifier.heightIn(min = LoadEarlierMinHeight),
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(LoadEarlierGlyph),
                        strokeWidth = LoadEarlierStroke,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(LoadEarlierGlyph),
                    )
                }
                Text(
                    text =
                        stringResource(
                            if (loading) R.string.chat_thread_loading_earlier else R.string.chat_thread_load_earlier,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** "{name} is typing…" between the list and the bottom area; fades and expands in 200 ms. Not a live region. */
@Composable
fun TypingStrip(
    visible: Boolean,
    name: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(TYPING_VISIBILITY_MS)) + expandVertically(tween(TYPING_VISIBILITY_MS)),
        exit = fadeOut(tween(TYPING_VISIBILITY_MS)) + shrinkVertically(tween(TYPING_VISIBILITY_MS)),
    ) {
        Text(
            text = stringResource(R.string.chat_thread_typing, name),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Small floating button that scrolls to the newest message, fading in when the reader is away from it. */
@Composable
fun JumpToLatestButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(JUMP_FADE_MS)),
        exit = fadeOut(tween(JUMP_FADE_MS)),
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = stringResource(R.string.chat_thread_jump_latest),
            )
        }
    }
}
