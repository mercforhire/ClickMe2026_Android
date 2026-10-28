package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.chat.ComposerMode
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val BannerIconSize = 24.dp

/**
 * Replaces the composer in a thread that cannot take a reply (CHAT-UNBLOCK section 5, D-03): "You blocked this
 * conversation" with an unblock hint, "You can't reply to this conversation" when the peer blocked me, and "This user
 * is unavailable." after a RECIPIENT_UNAVAILABLE send. One merged polite live region. Earlier messages stay readable
 * above it. The Scaffold content padding already carries the navigation-bar inset.
 */
@Composable
fun ComposerBlockedBanner(
    mode: ComposerMode,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val icon: ImageVector
    val iconTint: Color
    val title: String
    val hint: String?
    when (mode) {
        ComposerMode.BlockedByPeer -> {
            icon = Icons.Outlined.Block
            iconTint = scheme.onSurfaceVariant
            title = stringResource(R.string.chat_banner_blocked_by_peer)
            hint = null
        }
        ComposerMode.Unavailable -> {
            icon = Icons.Outlined.PersonOff
            iconTint = scheme.tertiary
            title = stringResource(R.string.chat_banner_unavailable)
            hint = null
        }
        // Normal never reaches the banner; the blocked-by-me copy is the safe fallback.
        ComposerMode.BlockedByMe, ComposerMode.Normal -> {
            icon = Icons.Outlined.Block
            iconTint = scheme.onSurfaceVariant
            title = stringResource(R.string.chat_banner_blocked_by_me)
            hint = stringResource(R.string.chat_banner_blocked_by_me_hint)
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerLow)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(BannerIconSize),
                tint = iconTint,
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            }
        }
    }
}
