package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.screens.chat.ComposerMode
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadMenuEntry
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.screens.chat.composerMode
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** Composer limit in UTF-16 code units, the backend `content` max (`z.string().min(1).max(10000)`, D-04). */
const val CHAT_COMPOSER_MAX = 10_000

/** Load state of the first page of a thread. A quiet reload never leaves [Loaded]. */
sealed interface ThreadLoad {
    data object Loading : ThreadLoad

    data class Failed(
        val message: FieldError,
    ) : ThreadLoad

    data object Loaded : ThreadLoad
}

/**
 * One state for the whole thread screen. The read side (items, paging, typing, block flags, notice) is written by
 * [ChatThreadViewModel]; the send-related fields ([composerText], [recipientUnavailable], [sendError]) are written by
 * the send flow (12-08). The peer fields start as the seeds of the route key and are filled in from the scanned
 * thread row when the key had none.
 */
data class ChatThreadUiState(
    val threadId: String,
    val peerId: String? = null,
    val peerName: String? = null,
    val peerAvatarUrl: String? = null,
    val peerIsExpert: Boolean? = null,
    val isSelfThread: Boolean = false,
    val load: ThreadLoad = ThreadLoad.Loading,
    val items: List<ThreadItem> = emptyList(),
    val hasMoreEarlier: Boolean = false,
    val isLoadingEarlier: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val peerTyping: Boolean = false,
    val composerText: String = "",
    val block: BlockState = BlockState(),
    val recipientUnavailable: Boolean = false,
    val sendError: FieldError? = null,
    val notice: ThreadNotice? = null,
) {
    val composerMode: ComposerMode
        get() = composerMode(block, recipientUnavailable)

    val menuEntry: ThreadMenuEntry
        get() = block.menuEntry

    /** True when the composer holds [CHAT_COMPOSER_MAX] units: the UI shows its counter. */
    val isComposerAtCap: Boolean
        get() = composerText.length >= CHAT_COMPOSER_MAX

    val canSend: Boolean
        get() = load is ThreadLoad.Loaded && composerMode == ComposerMode.Normal && composerText.isNotBlank()
}
