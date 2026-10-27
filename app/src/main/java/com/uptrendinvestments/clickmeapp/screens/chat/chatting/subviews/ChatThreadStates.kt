package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val SkeletonBubbleHeight = 40.dp
private val SkeletonBubbleWidths = listOf(200.dp, 140.dp, 240.dp, 120.dp, 180.dp)

/**
 * First-load placeholder copying the real layout: five bubble blocks alternating start and end, announced once as
 * "Loading". Quiet reloads never return here.
 */
@Composable
fun ChatThreadSkeleton(modifier: Modifier = Modifier) {
    val loading = stringResource(R.string.chat_thread_loading)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .semantics(mergeDescendants = true) { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SkeletonBubbleWidths.forEachIndexed { index, width ->
            SkeletonBubble(width = width, atEnd = index % 2 == 1)
        }
    }
}

@Composable
private fun SkeletonBubble(
    width: Dp,
    atEnd: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (atEnd) Alignment.End else Alignment.Start) {
        SkeletonBlock(Modifier.width(width).height(SkeletonBubbleHeight), shape = MaterialTheme.shapes.medium)
    }
}

/** First-load failure: heading, the server message or the network line, and Retry. */
@Composable
fun ChatThreadError(
    message: FieldError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.ErrorOutline,
        heading = stringResource(R.string.chat_thread_error_heading),
        body = message.asString(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        action = EmptyStateAction(stringResource(R.string.chat_thread_retry), onRetry),
    )
}

/** A loaded thread with no messages. The composer (12-14) passes the "Send a message" [action]. */
@Composable
fun ChatEmptyThread(
    modifier: Modifier = Modifier,
    action: EmptyStateAction? = null,
) {
    EmptyState(
        icon = Icons.Outlined.ChatBubbleOutline,
        heading = stringResource(R.string.chat_thread_empty_heading),
        body = stringResource(R.string.chat_thread_empty_body),
        modifier = modifier,
        action = action,
    )
}
