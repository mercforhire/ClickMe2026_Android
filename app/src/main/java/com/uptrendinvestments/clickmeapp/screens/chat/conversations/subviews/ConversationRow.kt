package com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.ConversationUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Instant

/** Test tag on the presence dot of a [ConversationAvatar]. */
const val CONVERSATION_PRESENCE_TAG = "conversation_presence"

/** Test tag on an [UnreadBadge]. */
const val CONVERSATION_BADGE_TAG = "conversation_badge"

/** Test tag on a [ConversationRow]. */
const val CONVERSATION_ROW_TAG = "conversation_row"

private val AvatarSize = 56.dp
private val PresenceDotSize = 12.dp
private val PresenceRing = 2.dp
private val BadgeMinSize = 24.dp
private const val BADGE_CAP = 99

/**
 * One conversation: avatar with presence dot, name, time label and a one-line preview with the unread badge
 * (UI-SPEC surface 1, iOS `ChatConversationRow`). The whole row is one button and one merged accessibility node that
 * reads "{name}, {last message}, {time}" then ", {n} unread": the visible preview is one line, the node keeps the full
 * last message (UI-SPEC Flow 4).
 *
 * [timeLabel] is computed by the caller (it needs the clock and locale). Blocked threads look like any other row
 * (D-19).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationRow(
    row: ConversationUi,
    timeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = row.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_conversations_name_fallback)
    val preview = row.lastMessage?.takeIf { it.isNotBlank() }
    val openLabel = stringResource(R.string.chat_conversations_open)
    val unreadText =
        if (row.unread > 0) pluralStringResource(R.plurals.chat_conversations_unread, row.unread, row.unread) else null
    val onlineText = if (row.isOnline) stringResource(R.string.chat_conversations_online) else null
    val description =
        listOfNotNull(name, preview, timeLabel.takeIf { it.isNotEmpty() }, unreadText, onlineText)
            .joinToString(", ")

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(CONVERSATION_ROW_TAG)
                .semantics { contentDescription = description }
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ConversationAvatar(url = row.avatarUrl, isOnline = row.isOnline)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (timeLabel.isNotEmpty()) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
                if (preview != null || row.unread > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = preview.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (row.unread > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        UnreadBadge(count = row.unread)
                    }
                }
            }
        }
    }
}

/** A 56 dp [Avatar] with a 12 dp `primary` presence dot, ringed in `surface`, at the bottom end when [isOnline]. */
@Composable
fun ConversationAvatar(
    url: String?,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(AvatarSize)) {
        Avatar(url = url, contentDescription = null, size = AvatarSize)
        if (isOnline) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(PresenceDotSize + PresenceRing * 2)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(PresenceRing)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag(CONVERSATION_PRESENCE_TAG),
            )
        }
    }
}

/** The unread count in a `primary` circle: hidden at 0, "99+" above 99. */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    val label = if (count > BADGE_CAP) stringResource(R.string.chat_conversations_badge_overflow) else count.toString()
    Box(
        modifier =
            modifier
                .testTag(CONVERSATION_BADGE_TAG)
                .defaultMinSize(minWidth = BadgeMinSize, minHeight = BadgeMinSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(horizontal = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(name = "ConversationRow")
@Composable
private fun ConversationRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConversationRow(
                row =
                    ConversationUi(
                        threadId = "t1",
                        peerId = "p1",
                        name = "Ada Lovelace",
                        avatarUrl = null,
                        peerIsExpert = true,
                        lastMessage = "See you at 3, bring the notes from last time.",
                        lastMsgAt = Instant.parse("2026-10-27T14:00:00Z"),
                        isOnline = true,
                        serverUnread = 0,
                        localUnread = 3,
                    ),
                timeLabel = "10:23 AM",
                onClick = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
