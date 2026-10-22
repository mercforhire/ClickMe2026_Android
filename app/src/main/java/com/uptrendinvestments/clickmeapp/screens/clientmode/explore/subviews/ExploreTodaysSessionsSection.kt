package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.TodaysSessionUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the (always disabled) Join Call button of a Today's Sessions card. */
const val TODAYS_SESSION_JOIN_TAG = "todays_session_join"

private val IconSize = 20.dp
private val SessionAvatarSize = 40.dp
private val JoinButtonMinHeight = 56.dp

/**
 * Today's Sessions strip (D-01). Renders nothing for an empty list. Cards are inert in Phase 5: no tap action
 * (Phase 8 adds it) and a permanently disabled Join Call (D-02, Phase 9 adds the join window).
 */
@Composable
fun ExploreTodaysSessionsSection(
    sessions: List<TodaysSessionUi>,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.explore_section_todays_sessions))
        Spacer(Modifier.height(Spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            sessions.forEach { session -> TodaysSessionCard(session) }
        }
    }
}

@Composable
private fun TodaysSessionCard(session: TodaysSessionUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = session.expertAvatarUrl, contentDescription = null, size = SessionAvatarSize)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.expertName ?: stringResource(R.string.explore_expert_name_fallback),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = session.topic ?: stringResource(R.string.explore_session_topic_fallback),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                    tint = MaterialTheme.extras.onSurfaceMuted,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = session.timeRange,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            val joinState = stringResource(R.string.explore_join_call_state)
            // PHASE-9: join window + click (D-02)
            PressScaleButton(
                onClick = {},
                enabled = false,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = JoinButtonMinHeight)
                        .testTag(TODAYS_SESSION_JOIN_TAG)
                        .semantics { stateDescription = joinState },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.explore_join_call))
            }
        }
    }
}

@Preview(name = "ExploreTodaysSessionsSection")
@Composable
private fun ExploreTodaysSessionsSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExploreTodaysSessionsSection(
                sessions =
                    listOf(
                        TodaysSessionUi(
                            bookingId = "b1",
                            expertName = "Ada Lovelace",
                            expertAvatarUrl = null,
                            topic = "Algorithms",
                            timeRange = "3:00 PM – 4:00 PM",
                        ),
                    ),
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
