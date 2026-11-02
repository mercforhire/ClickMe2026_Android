package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.chat.openChatBooking
import com.uptrendinvestments.clickmeapp.screens.chat.openChatPeerProfile
import com.uptrendinvestments.clickmeapp.screens.chat.openChatReportBlock

/**
 * The chat thread route: builds the ViewModel for [key] and renders the thread. View profile and Report & Block use
 * the live state's peer (filled in from the scanned thread row when the key had none), not the stale route key.
 */
@Composable
fun ChatThreadRoute(
    key: ChatThreadKey,
    navigator: Navigator,
    mode: AppMode,
) {
    val viewModel =
        hiltViewModel<ChatThreadViewModel, ChatThreadViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) },
        )
    ChatThreadScreen(
        viewModel = viewModel,
        mode = mode,
        onBack = navigator::goBack,
        onOpenBooking = { bookingId, isTerminal -> navigator.openChatBooking(mode, bookingId, isTerminal) },
        onViewProfile = { navigator.openChatPeerProfile(viewModel.uiState.value.toThreadKey(), mode) },
        onOpenReportBlock = { navigator.openChatReportBlock(viewModel.uiState.value.toThreadKey()) },
    )
}
