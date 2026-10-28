package com.uptrendinvestments.clickmeapp.services.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM entry point. Thin by design: [onNewToken] re-registers the rotated token (the registrar ignores it when
 * there is no session), and [onMessageReceived] hands a foreground push to [PushMessageHandler]. It runs in a process FCM may have started in the
 * background, which is why rotation is handled here and not only by the foreground registration policy.
 * It never logs the token.
 */
@AndroidEntryPoint
class ClickMeMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var registrar: PushTokenRegistrar

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var handler: PushMessageHandler

    override fun onMessageReceived(message: RemoteMessage) {
        handler.onMessage(
            ForegroundPush(
                data = message.data,
                title = message.notification?.title,
                body = message.notification?.body,
                channelId = message.notification?.channelId,
            ),
        )
    }

    override fun onNewToken(token: String) {
        scope.launch { registrar.register(token) }
    }
}
