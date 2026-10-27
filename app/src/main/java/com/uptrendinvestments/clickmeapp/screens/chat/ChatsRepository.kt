package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.ChatActionRequest
import com.uptrendinvestments.clickmeapp.model.requests.InitiateChatRequest
import com.uptrendinvestments.clickmeapp.model.requests.ReportUserRequest
import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatSendAckData
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.model.responses.ChatsPayload
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.model.responses.ReportUserData
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

        /** `POST /chats/{id}/actions` with `{action: "block"}`. */
        suspend fun block(threadId: String) {
            apiCall { api.chatAction(threadId, ChatActionRequest(ACTION_BLOCK)) }
        }

        /** `POST /chats/{id}/actions` with `{action: "unblock"}`. 403 `NOT_BLOCKER` when the caller is not the blocker. */
        suspend fun unblock(threadId: String) {
            apiCall { api.chatAction(threadId, ChatActionRequest(ACTION_UNBLOCK)) }
        }

        /**
         * `POST /chats/{id}/actions` with `{action: "report", reason, details?}`. [reason] goes out as its wire
         * value, never a label. Blank [details] are omitted and the rest is trimmed. The repository does not cap
         * length: the UI caps details at 1000, below both backend limits (1000 for profile reports, 2000 for chat
         * reports).
         */
        suspend fun reportInChat(
            threadId: String,
            reason: UserReportReason,
            details: String?,
        ) {
            val body = ChatActionRequest(ACTION_REPORT, reason.wire, cleanDetails(details))
            apiCall { api.chatAction(threadId, body) }
        }

        /** `POST /users/{id}/report` with `{reason, details?}`; same details handling as [reportInChat]. */
        suspend fun reportUser(
            userId: String,
            reason: UserReportReason,
            details: String?,
        ): ReportUserData {
            val body = ReportUserRequest(reason.wire, cleanDetails(details))
            return apiCall { api.reportUser(userId, body) }.data
        }

        private fun cleanDetails(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

        companion object {
            const val ACTION_BLOCK = "block"
            const val ACTION_UNBLOCK = "unblock"
            const val ACTION_REPORT = "report"
            const val LIST_LIMIT = 50
            const val SCAN_LIMIT = 100
            const val PAGE_SIZE = 30
            const val TYPE_TEXT = "text"
        }
    }
