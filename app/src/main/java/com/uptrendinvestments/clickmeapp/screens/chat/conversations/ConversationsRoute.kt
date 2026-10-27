package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * The Chats tab root for either shell: the Conversations list. A row opens its thread on the Chats stack, so Back
 * lands here (D-13).
 */
@Composable
fun ConversationsRoute(
    navigator: Navigator,
    mode: AppMode,
    avatarUrl: String?,
    onOpenModeSwitch: () -> Unit,
) {
    ConversationsScreen(
        mode = mode,
        avatarUrl = avatarUrl,
        onAvatarClick = onOpenModeSwitch,
        onOpenThread = { navigator.navigate(it) },
    )
}
