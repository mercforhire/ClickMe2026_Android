package com.uptrendinvestments.clickmeapp.services.push

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushManifestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val metaData
        get() =
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData

    @Test
    fun defaultChannelIsGeneral() {
        assertEquals(
            NotificationChannels.GENERAL,
            metaData.getString("com.google.firebase.messaging.default_notification_channel_id"),
        )
    }

    @Test
    fun defaultIconAndColor() {
        assertEquals(
            R.drawable.ic_stat_clickme,
            metaData.getInt("com.google.firebase.messaging.default_notification_icon"),
        )
        assertEquals(
            R.color.notification_accent,
            metaData.getInt("com.google.firebase.messaging.default_notification_color"),
        )
        assertEquals(0xFF44F697.toInt(), context.getColor(R.color.notification_accent))
    }

    @Test
    fun postNotificationsDeclaredOnce() {
        val requested =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
        assertEquals(1, requested.count { it == "android.permission.POST_NOTIFICATIONS" })
    }

    @Test
    fun messagingServiceIsPrivateAndHandlesMessagingEvent() {
        val serviceInfo =
            context.packageManager.getServiceInfo(
                ComponentName(context.packageName, ClickMeMessagingService::class.java.name),
                0,
            )
        assertFalse(serviceInfo.exported)

        val resolved =
            context.packageManager.queryIntentServices(
                Intent("com.google.firebase.MESSAGING_EVENT").setPackage(context.packageName),
                0,
            )
        assertTrue(resolved.any { it.serviceInfo.name == ClickMeMessagingService::class.java.name })
    }

    @Test
    fun generalChannelExistsAfterEnsureCreated() {
        NotificationChannels.ensureCreated(context)
        val channel = NotificationManagerCompat.from(context).getNotificationChannelCompat("general")
        assertNotNull(channel)
        assertEquals(NotificationManagerCompat.IMPORTANCE_DEFAULT, channel!!.importance)
    }
}
