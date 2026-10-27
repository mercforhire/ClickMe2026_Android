package com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private const val SKELETON_ROW_COUNT = 5
private val SkeletonSearchHeight = 56.dp
private val SkeletonAvatar = 56.dp

/**
 * First-load placeholder copying the real layout: a search-field bar and five rows (56 dp circle, name bar, time bar,
 * preview bar), announced once as "Loading". Quiet reloads never return here.
 */
@Composable
fun ConversationsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SkeletonBlock(Modifier.fillMaxWidth().height(SkeletonSearchHeight), shape = MaterialTheme.shapes.extraLarge)
            repeat(SKELETON_ROW_COUNT) { SkeletonRow() }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(modifier = Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SkeletonBlock(Modifier.size(SkeletonAvatar), shape = CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(Modifier.width(160.dp).height(20.dp))
                    Spacer(Modifier.width(Spacing.md))
                    SkeletonBlock(Modifier.width(72.dp).height(16.dp))
                }
                SkeletonBlock(Modifier.width(240.dp).height(16.dp))
            }
        }
    }
}

/** Zero threads (no search field). The body names what starts a conversation in each mode. */
@Composable
fun ConversationsEmptyState(
    mode: AppMode,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.Forum,
        heading = stringResource(R.string.chat_conversations_empty_heading),
        body =
            stringResource(
                when (mode) {
                    AppMode.Client -> R.string.chat_conversations_empty_body_client
                    AppMode.Expert -> R.string.chat_conversations_empty_body_expert
                },
            ),
        modifier = modifier,
    )
}

/** A search matched nothing. The search field stays so the query can be changed. */
@Composable
fun ConversationsNoResults(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Search,
        heading = stringResource(R.string.chat_conversations_no_results_heading),
        body = stringResource(R.string.chat_conversations_no_results_body),
        modifier = modifier,
    )
}

/** First-load failure: heading, the server message or the network line, and Retry. */
@Composable
fun ConversationsError(
    message: FieldError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.ErrorOutline,
        heading = stringResource(R.string.chat_conversations_error_heading),
        body = message.asString(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        action = EmptyStateAction(stringResource(R.string.chat_conversations_retry), onRetry),
    )
}

@Preview(name = "ConversationsSkeleton")
@Composable
private fun ConversationsSkeletonPreview() {
    ClickMeTheme { Surface(color = MaterialTheme.colorScheme.surface) { ConversationsSkeleton() } }
}

@Preview(name = "ConversationsEmptyState")
@Composable
private fun ConversationsEmptyStatePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) { ConversationsEmptyState(mode = AppMode.Client) }
    }
}
