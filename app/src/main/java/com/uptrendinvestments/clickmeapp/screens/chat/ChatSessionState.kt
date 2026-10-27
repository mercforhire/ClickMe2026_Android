package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat state that lives for one signed-in session and only in memory.
 *
 * - [openThreadId]: the thread on screen. The Conversations list uses it to skip its session-local unread count for
 *   that thread (12-RESEARCH User Resolutions A1).
 * - The blocked-by-me set: threads this session blocked. `GET /chats` does not list a thread that has no messages,
 *   so a block scan cannot find it (A4), and this set tells the block state it is still blocked by me.
 *
 * Bound as a [SessionDataCleaner] so one account never sees another account's state: nothing clears singletons on
 * logout by itself (see BookingRequestsRepository).
 */
@Singleton
class ChatSessionState
    @Inject
    constructor() : SessionDataCleaner {
        private val _openThreadId = MutableStateFlow<String?>(null)
        val openThreadId: StateFlow<String?> = _openThreadId.asStateFlow()

        private val blockedByMe: MutableSet<String> = ConcurrentHashMap.newKeySet()

        /** The thread [threadId] is now on screen. */
        fun onThreadShown(threadId: String) {
            _openThreadId.value = threadId
        }

        /** The thread [threadId] left the screen. Clears the open thread only when it is the one shown. */
        fun onThreadHidden(threadId: String) {
            _openThreadId.value = _openThreadId.value?.takeUnless { it.equals(threadId, ignoreCase = true) }
        }

        fun markBlockedByMe(threadId: String) {
            blockedByMe.add(threadId.lowercase())
        }

        fun clearBlockedByMe(threadId: String) {
            blockedByMe.remove(threadId.lowercase())
        }

        fun isBlockedByMe(threadId: String): Boolean = threadId.lowercase() in blockedByMe

        override suspend fun clearSessionData() {
            _openThreadId.value = null
            blockedByMe.clear()
        }
    }
