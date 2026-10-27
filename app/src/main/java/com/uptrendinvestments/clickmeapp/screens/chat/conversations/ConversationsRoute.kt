package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PlaceholderTabContent
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * The Chats tab root for either shell. The body is a placeholder until the Conversations screen replaces it; the
 * signature stays, so neither shell is edited again.
 */
@Composable
fun ConversationsRoute(
    navigator: Navigator,
    mode: AppMode,
    avatarUrl: String?,
    onOpenModeSwitch: () -> Unit,
) {
    PlaceholderTabContent(
        title = stringResource(R.string.tab_chats),
        icon = Icons.Outlined.ChatBubbleOutline,
        actions = { TopBarAvatarButton(avatarUrl, onClick = onOpenModeSwitch) },
    )
}
