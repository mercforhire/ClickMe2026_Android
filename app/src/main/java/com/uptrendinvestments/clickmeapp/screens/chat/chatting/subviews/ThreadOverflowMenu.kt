package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadMenuEntry

/**
 * The thread top bar's overflow menu (SAFE-03, D-08): "View profile" (client mode, expert peer; disabled until the
 * peer id is known) and exactly one of "Block or report", "Unblock or report" or "Report" per [entry]. The safety item
 * is hidden on a self thread ([showSafetyItem] false). With no item to show the button is not rendered.
 */
@Composable
fun ThreadOverflowMenu(
    entry: ThreadMenuEntry,
    showViewProfile: Boolean,
    viewProfileEnabled: Boolean,
    showSafetyItem: Boolean,
    onViewProfile: () -> Unit,
    onReportBlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showViewProfile && !showSafetyItem) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.chat_thread_menu_more),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (showViewProfile) {
                MenuItem(
                    text = stringResource(R.string.chat_thread_menu_view_profile),
                    icon = Icons.Outlined.Person,
                    enabled = viewProfileEnabled,
                    onClick = {
                        expanded = false
                        onViewProfile()
                    },
                )
            }
            if (showSafetyItem) {
                MenuItem(
                    text = stringResource(entry.labelRes()),
                    icon = if (entry == ThreadMenuEntry.ReportOnly) Icons.Outlined.Flag else Icons.Outlined.Block,
                    enabled = true,
                    onClick = {
                        expanded = false
                        onReportBlock()
                    },
                )
            }
        }
    }
}

private fun ThreadMenuEntry.labelRes(): Int =
    when (this) {
        ThreadMenuEntry.BlockOrReport -> R.string.chat_thread_menu_block_or_report
        ThreadMenuEntry.UnblockOrReport -> R.string.chat_thread_menu_unblock_or_report
        ThreadMenuEntry.ReportOnly -> R.string.chat_thread_menu_report
    }

@Composable
private fun MenuItem(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = text, style = MaterialTheme.typography.bodyLarge) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
    )
}
