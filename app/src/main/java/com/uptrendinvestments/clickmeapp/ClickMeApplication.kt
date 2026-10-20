package com.uptrendinvestments.clickmeapp

import android.app.Application
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnectionManager
import com.uptrendinvestments.clickmeapp.realtime.RealtimeDebugLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The realtime manager starts here, not in MainActivity, because an Activity can be recreated and the
 * socket must follow login state and process foreground without any screen involvement. Hilt injects the
 * fields in `super.onCreate()`. A process started by FCM is not STARTED, so nothing connects until the UI
 * is visible.
 */
@HiltAndroidApp
class ClickMeApplication : Application() {
    @Inject
    lateinit var realtimeConnectionManager: RealtimeConnectionManager

    @Inject
    lateinit var realtimeConnection: RealtimeConnection

    override fun onCreate() {
        super.onCreate()
        realtimeConnectionManager.start()
        RealtimeDebugLogger.install(realtimeConnection)
    }
}
