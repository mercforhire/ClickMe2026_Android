package com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R

/**
 * The long-press menu of a failed bubble: one "Delete" item. There is no confirmation because the message never
 * reached the server (UI-SPEC A10).
 */
@Composable
fun FailedMessageMenu(
    expanded: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = MaterialTheme.colorScheme.error
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.chat_failed_delete), color = error) },
            onClick = onDelete,
            modifier = Modifier.heightIn(min = 48.dp),
            leadingIcon = { Icon(imageVector = Icons.Outlined.Delete, contentDescription = null, tint = error) },
        )
    }
}
