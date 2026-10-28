package com.uptrendinvestments.clickmeapp.services.push

import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
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
        fun onMessage(push: ForegroundPush) {
            launchRequest.onForegroundArrival()
            poster.post(push)
        }
    }
