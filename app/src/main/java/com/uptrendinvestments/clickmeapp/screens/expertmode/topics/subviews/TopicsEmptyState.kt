package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/**
 * Body for a loaded Topics tab with no topics (EXP-01, D-05): the single CTA is "Add Topic" (no FAB in this state).
 * Scrollable and full-size so pull-to-refresh still works.
 */
@Composable
fun TopicsEmptyContent(
    onAddTopic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(
        icon = Icons.Outlined.LibraryAdd,
        heading = stringResource(R.string.topics_empty_heading),
        body = stringResource(R.string.topics_empty_body),
        action = EmptyStateAction(stringResource(R.string.topics_add_topic), onAddTopic),
        modifier = modifier,
    )
}

/** Body for a failed first load of the Topics tab: [message] is the server text or the network fallback. */
@Composable
fun TopicsErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(
        icon = Icons.Outlined.CloudOff,
        heading = stringResource(R.string.topics_load_error_heading),
        body = message,
        action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
        modifier = modifier,
    )
}

@Composable
private fun CenteredState(
    icon: ImageVector,
    heading: String,
    body: String,
    action: EmptyStateAction,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(icon = icon, heading = heading, body = body, action = action)
    }
}

@Preview(name = "TopicsEmptyContent")
@Composable
private fun TopicsEmptyContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicsEmptyContent(onAddTopic = {})
        }
    }
}

@Preview(name = "TopicsErrorContent")
@Composable
private fun TopicsErrorContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicsErrorContent(message = "Check your connection and try again.", onRetry = {})
        }
    }
}
