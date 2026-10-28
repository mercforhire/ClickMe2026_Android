package com.uptrendinvestments.clickmeapp.services.push

import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.push.PushEvent
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A push that reached `onMessageReceived`, which FCM only calls while the app is in the foreground or for a data-only
 * message. [data] is the raw FCM data map; [channelId] is the FCM notification channel, when the payload had one.
 *
 * It holds a title, a body and ids. [toString] prints the channel and the data keys only, so an accidental log or
 * crash report never carries message content.
 */
data class ForegroundPush(
    val data: Map<String, String>,
    val title: String?,
    val body: String?,
    val channelId: String?,
) {
    override fun toString(): String = "ForegroundPush(channelId=$channelId, keys=${data.keys})"
}

/** Posts a [ForegroundPush] as a system notification. A seam so the handler can be tested without a notification. */
interface PushNotificationPoster {
    fun post(push: ForegroundPush)
}

/**
 * Handles a push that arrives while the app is running (D-12).
 *
 * There is no navigation here: a tap on the posted notification carries the destination to `MainActivity`, exactly
 * like a system-displayed background push, and the root host acts on it (RESEARCH Anti-Patterns). It never logs
 * titles, bodies or data values.
 */
@Singleton
class PushMessageHandler
    @Inject
    constructor(
        private val launchRequest: PushLaunchRequest,
        private val chatSession: ChatSessionState,
        private val poster: PushNotificationPoster,
    ) {
        /**
         * Every push ticks the list first (D-12). A chat push for the thread on screen is then dropped (D-13): the
         * realtime message already shows in the open thread. This differs from iOS, which presents it; Android posts
         * nothing so a visible conversation does not also raise a heads-up for itself.
         */
        fun onMessage(push: ForegroundPush) {
            launchRequest.onForegroundArrival()
            val event = PushEvent.from(push.data)
            val openThread = chatSession.openThreadId.value
            val threadId = event?.threadId
            if (event?.category == "message" &&
                threadId != null &&
                threadId.equals(openThread, ignoreCase = true)
            ) {
                return
            }
            poster.post(push)
        }
    }
