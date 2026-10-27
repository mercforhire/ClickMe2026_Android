package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.model.requests.InitiateChatRequest
import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatSendAckData
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.model.responses.ChatsPayload
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat threads, messages and the report and block actions (`/chats`, `/users/{id}/report`). Ports iOS
 * `ClickMeAPI+Chats`, with request bodies that follow the backend schemas rather than iOS where they differ.
 *
 * Stateless: nothing here is cleared on logout, so it caches nothing; throws ApiException only; message text is
 * never logged.
 */
@Singleton
class ChatsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `POST /chats/initiate`: opens (or returns) the thread with [peerId]. */
        suspend fun initiate(peerId: String): InitiateChatData =
            apiCall { api.initiateChat(InitiateChatRequest(peerId)) }.data

        /** `GET /chats`: page 1 only (D-17), newest activity first. */
        suspend fun threads(limit: Int = LIST_LIMIT): ChatsPayload =
            apiCall { api.getChats(page = 1, limit = limit) }.data

        /**
         * Finds [threadId] in the first [SCAN_LIMIT] threads, ignoring case, or null when it is not there. There is
         * no single-thread endpoint; 100 is the backend page cap (RESEARCH A3).
         */
        suspend fun findThread(threadId: String): ChatThreadItem? =
            threads(SCAN_LIMIT).threads.firstOrNull { it.threadId.equals(threadId, ignoreCase = true) }

        /** `GET /chats/{id}/messages`: one page of [limit] messages. */
        suspend fun messages(
            threadId: String,
            page: Int,
            limit: Int = PAGE_SIZE,
        ): ChatMessagesPayload = apiCall { api.getChatMessages(threadId, page, limit) }.data

        /** `POST /chats/{id}/send`: a text message with the client-generated [clientMsgId]. */
        suspend fun send(
            threadId: String,
            content: String,
            clientMsgId: String,
        ): ChatSendAckData =
            apiCall { api.sendChatMessage(threadId, SendMessageRequest(TYPE_TEXT, content, clientMsgId)) }.data

        companion object {
            const val LIST_LIMIT = 50
            const val SCAN_LIMIT = 100
            const val PAGE_SIZE = 30
            const val TYPE_TEXT = "text"
        }
    }
