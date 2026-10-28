package com.uptrendinvestments.clickmeapp

import android.app.Application
import com.stripe.android.PaymentConfiguration
import com.uptrendinvestments.clickmeapp.config.AppConfig
import com.uptrendinvestments.clickmeapp.managers.PushRegistrationManager
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnectionManager
import com.uptrendinvestments.clickmeapp.realtime.RealtimeDebugLogger
import com.uptrendinvestments.clickmeapp.services.call.CallNotificationFactory
import com.uptrendinvestments.clickmeapp.services.push.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The realtime manager starts here, not in MainActivity, because an Activity can be recreated and the
 * socket must follow login state and process foreground without any screen involvement. Hilt injects the
 * fields in `super.onCreate()`. A process started by FCM is not STARTED, so nothing connects until the UI
 * is visible. The push registration manager starts here for the same reason: it registers the FCM token each time a
 * logged-in user brings the app to the foreground. Stripe's publishable key is registered once here, before any PaymentSheet, FlowController or
 * PaymentLauncher can launch; Robolectric tests run `android.app.Application`, so this never runs under unit tests.
 */
@HiltAndroidApp
class ClickMeApplication : Application() {
    @Inject
    lateinit var realtimeConnectionManager: RealtimeConnectionManager

    @Inject
    lateinit var realtimeConnection: RealtimeConnection

    @Inject
    lateinit var pushRegistrationManager: PushRegistrationManager

    override fun onCreate() {
        super.onCreate()
        CallNotificationFactory.createChannel(this)
        NotificationChannels.ensureCreated(this)
        PaymentConfiguration.init(this, AppConfig.stripePublishableKey)
        realtimeConnectionManager.start()
        pushRegistrationManager.start()
        RealtimeDebugLogger.install(realtimeConnection)
    }
}
