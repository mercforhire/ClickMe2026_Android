package com.uptrendinvestments.clickmeapp.services.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FCM entry point. Thin by design: [onNewToken] re-registers the rotated token (the registrar ignores it when
 * there is no session), and 13-10 adds `onMessageReceived`. It runs in a process FCM may have started in the
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

    override fun onNewToken(token: String) {
        scope.launch { registrar.register(token) }
    }
}
