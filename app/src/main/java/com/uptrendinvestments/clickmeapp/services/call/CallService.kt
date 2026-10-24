package com.uptrendinvestments.clickmeapp.services.call

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Skeleton so the notification factory and manifest compile; completed by the bridge task. */
class CallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.uptrendinvestments.clickmeapp.call.START"
        const val ACTION_END = "com.uptrendinvestments.clickmeapp.call.END"
    }
}
