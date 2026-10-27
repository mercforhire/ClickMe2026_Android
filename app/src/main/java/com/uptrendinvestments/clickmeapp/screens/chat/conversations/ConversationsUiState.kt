package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.screens.chat.ConversationsNotice
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.Instant

/** The `partner.roles` entry that marks an expert. */
private const val ROLE_EXPERT = "expert"

/**
 * One row of the Conversations list: a thread plus the partner's display data. [lastMessage] is user text, so
 * [toString] prints ids and counts only.
 *
 * [serverUnread] is the server's `unread_count`; [localUnread] is the session-local count of peer messages that
 * arrived while the thread was not open. Show [unread], never either field alone ([displayUnread]).
 */
data class ConversationUi(
    val threadId: String,
    val peerId: String,
    val name: String?,
    val avatarUrl: String?,
    val peerIsExpert: Boolean,
    val lastMessage: String?,
    val lastMsgAt: Instant?,
    val isOnline: Boolean,
    val serverUnread: Int,
    val localUnread: Int,
) {
    /** The count the badge shows. */
    val unread: Int get() = displayUnread(serverUnread, localUnread)

    /** The key that opens this thread, seeded with the partner's display data (iOS `ChatView` init). */
    fun toThreadKey(): ChatThreadKey =
        ChatThreadKey(
            threadId = threadId,
            peerId = peerId,
            peerName = name,
            peerAvatarUrl = avatarUrl,
            peerIsExpert = peerIsExpert,
        )

    override fun toString(): String =
        "ConversationUi(threadId=$threadId, peerId=$peerId, unread=$unread, isOnline=$isOnline)"
}

/** Load state of the list. Only the first load (or a retry) shows [Loading]; quiet reloads never return to it. */
sealed interface ConversationsLoad {
    data object Loading : ConversationsLoad

    data class Failed(
        val message: FieldError,
    ) : ConversationsLoad

    data object Loaded : ConversationsLoad
}

/**
 * Everything the Conversations list renders. [query] is the search text (D-18); [notice] is the one-time "Blocked
 * {name}" snackbar (D-09); [refreshFailed] asks for the pull-to-refresh failure snackbar.
 */
data class ConversationsUiState(
    val load: ConversationsLoad = ConversationsLoad.Loading,
    val conversations: List<ConversationUi> = emptyList(),
    val query: String = "",
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val notice: ConversationsNotice? = null,
) {
    /** The rows that match [query]. */
    val visible: List<ConversationUi> get() = filterConversations(conversations, query)
}

/**
 * The unread count to show on a row: the larger of the server value and the session-local one.
 *
 * The backend never increments `unread_count` and `message.read` does not reset it (12-RESEARCH Pitfall 1), so the
 * server value is usually 0 and the session-local count is the truthful one (A1 resolution). When the backend is
 * fixed, swap this to return [server] and drop the local count.
 */
fun displayUnread(
    server: Int,
    local: Int,
): Int = maxOf(server, local)

/** Keeps the rows whose name or last message contains the trimmed [query], ignoring case. A blank query keeps all. */
fun filterConversations(
    rows: List<ConversationUi>,
    query: String,
): List<ConversationUi> {
    val q = query.trim()
    if (q.isEmpty()) return rows
    return rows.filter {
        it.name?.contains(q, ignoreCase = true) == true || it.lastMessage?.contains(q, ignoreCase = true) == true
    }
}

/** Newest last message first. The sort is stable (equal times keep server order) and rows without a time sort last. */
fun sortConversations(rows: List<ConversationUi>): List<ConversationUi> =
    rows.sortedWith { a, b ->
        val at = a.lastMsgAt
        val bt = b.lastMsgAt
        when {
            at == null && bt == null -> 0
            at == null -> 1
            bt == null -> -1
            else -> bt.compareTo(at)
        }
    }

/** Maps a `GET /chats` row, applying the session-local unread count [localUnread]. */
fun ChatThreadItem.toConversationUi(localUnread: Int): ConversationUi =
    ConversationUi(
        threadId = threadId,
        peerId = partner.id,
        name = partner.name,
        avatarUrl = partner.avatarUrl,
        peerIsExpert = partner.roles.any { it.equals(ROLE_EXPERT, ignoreCase = true) },
        lastMessage = lastMessage,
        lastMsgAt = lastMsgAt,
        isOnline = isOnline,
        serverUnread = unreadCount,
        localUnread = localUnread,
    )
