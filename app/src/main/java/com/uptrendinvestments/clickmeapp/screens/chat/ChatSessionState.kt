package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
 * - Unsent text (WR-03): the text of a send that failed or was refused after its thread screen had closed. A send
 *   keeps running when the screen goes away, so when it does not succeed there is no composer to give the text back
 *   to; it waits here until the same user opens that thread again. It is kept only in this process, never written to
 *   disk, nothing is resent automatically (D-02), and it is restored only for the user who typed it.
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

        /** Kept unsent text by `owner|thread` (both lower-cased). Never exposed, logged or printed. */
        private val unsentText = MutableStateFlow<Map<String, String>>(emptyMap())

        /**
         * Change signal for [unsentText]: the keys that currently hold text, never the text itself. A thread screen
         * collects it and calls [takeUnsentText] on each emission.
         */
        val unsentKeys: Flow<Set<String>> = unsentText.map { it.keys.toSet() }

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

        /**
         * Keeps [text] for [ownerId] and [threadId]. Text already kept for that pair stays first, separated from the
         * new text by a new line.
         */
        fun keepUnsentText(
            threadId: String,
            ownerId: String,
            text: String,
        ) {
            val key = unsentKey(threadId, ownerId)
            unsentText.update { it + (key to (it[key]?.let { kept -> kept + "\n" + text } ?: text)) }
        }

        /** Removes and returns the text kept for [ownerId] and [threadId], or null. A null owner never has any. */
        fun takeUnsentText(
            threadId: String,
            ownerId: String?,
        ): String? {
            if (ownerId == null) return null
            val key = unsentKey(threadId, ownerId)
            return unsentText.getAndUpdate { it - key }[key]
        }

        private fun unsentKey(
            threadId: String,
            ownerId: String,
        ): String = ownerId.lowercase() + "|" + threadId.lowercase()

        override suspend fun clearSessionData() {
            _openThreadId.value = null
            blockedByMe.clear()
            unsentText.value = emptyMap()
        }
    }
