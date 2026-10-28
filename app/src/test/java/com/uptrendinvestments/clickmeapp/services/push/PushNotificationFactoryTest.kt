package com.uptrendinvestments.clickmeapp.services.push

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PushNotificationFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    private fun push(
        title: String? = "Title",
        body: String? = "Body",
        channelId: String? = null,
        data: Map<String, String> = emptyMap(),
    ) = ForegroundPush(data, title, body, channelId)

    private fun build(push: ForegroundPush): Notification = PushNotificationFactory.build(context, push, "tag")

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    @Test
    fun channelChoice() {
        assertEquals("messages", PushNotificationFactory.channelFor(push(channelId = "messages")))
        assertEquals(
            "sessions",
            PushNotificationFactory.channelFor(push(channelId = "promo", data = mapOf("category" to "review"))),
        )
        assertEquals("sessions", PushNotificationFactory.channelFor(push(data = mapOf("category" to "review"))))
        assertEquals("general", PushNotificationFactory.channelFor(push()))
        assertEquals("messages", build(push(channelId = "messages")).channelId)
    }

    @Test
    fun contentRules() {
        val stripped = build(push(title = "**Ada** accepted", body = "Your **booking** is set"))
        assertEquals("Ada accepted", stripped.title())
        assertEquals("Your booking is set", stripped.text())

        val long = "word ".repeat(80).trim()
        val big = build(push(body = long))
        assertEquals(long, big.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
        assertEquals(
            "android.app.Notification\$BigTextStyle",
            big.extras.getString(Notification.EXTRA_TEMPLATE),
        )

        val noTitle = build(push(title = null))
        assertNull(noTitle.title())
        assertEquals("Body", noTitle.text())

        val noBody = build(push(body = null))
        assertEquals("Title", noBody.title())
        assertNull(noBody.text())

        val neither = build(push(title = null, body = null))
        assertEquals("ClickMe", neither.title())
        assertNull(neither.text())
    }

    @Test
    fun categoryMapping() {
        assertEquals(Notification.CATEGORY_MESSAGE, build(push(data = mapOf("category" to "message"))).category)
        assertEquals(Notification.CATEGORY_EVENT, build(push(data = mapOf("category" to "booking"))).category)
        assertEquals(Notification.CATEGORY_EVENT, build(push(data = mapOf("category" to "session"))).category)
        assertEquals(Notification.CATEGORY_STATUS, build(push(data = mapOf("category" to "payout"))).category)
        assertEquals(Notification.CATEGORY_STATUS, build(push()).category)
    }

    @Test
    fun privateWithGenericPublicVersion() {
        val n = build(push(title = "Secret", body = "Hidden", data = mapOf("category" to "booking")))

        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility)
        val public = n.publicVersion
        assertNotNull(public)
        assertEquals("ClickMe", public.title())
        assertEquals("You have a new notification", public.text())
    }

    @Test
    fun stableTagPerEntity() {
        val first = push(data = mapOf("category" to "booking", "booking_id" to B1))
        val again = push(data = mapOf("category" to "booking", "booking_id" to B1, "sub_category" to "reschedules"))
        val other = push(data = mapOf("category" to "booking", "booking_id" to B2))
        assertEquals(PushNotificationFactory.tagFor(first), PushNotificationFactory.tagFor(again))
        assertNotEquals(PushNotificationFactory.tagFor(first), PushNotificationFactory.tagFor(other))

        val chat = push(data = mapOf("category" to "message", "thread_id" to T1))
        assertEquals("push:message:$T1", PushNotificationFactory.tagFor(chat))

        val bare = push(data = mapOf("category" to "account"))
        assertNotEquals(PushNotificationFactory.tagFor(bare), PushNotificationFactory.tagFor(bare))
    }

    @Test
    fun immutableIntentWithOnlyPushKeys() {
        val data =
            mapOf(
                "category" to "booking",
                "booking_id" to B1,
                "sender_id" to "someone",
                "refund_amount" to "1200",
            )
        val pending = PushNotificationFactory.contentIntent(context, push(data = data), "tag")
        val shadow = shadowOf(pending)
        val intent = shadow.savedIntent

        assertTrue(shadow.flags and PendingIntent.FLAG_IMMUTABLE != 0)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertEquals(B1, intent.getStringExtra("booking_id"))
        assertEquals("booking", intent.getStringExtra("category"))
        assertFalse(intent.hasExtra("sender_id"))
        assertFalse(intent.hasExtra("refund_amount"))
    }

    @Test
    fun disabledNotificationsPostNothing() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val shadowManager = shadowOf(manager)
        val poster = SystemPushNotificationPoster(context)
        val booking = push(data = mapOf("category" to "booking", "booking_id" to B1))

        shadowManager.setNotificationsEnabled(false)
        poster.post(booking)
        assertEquals(0, shadowManager.size())

        shadowManager.setNotificationsEnabled(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Not granted: nothing is posted.
            shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            poster.post(booking)
            assertEquals(0, shadowManager.size())
            shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
        poster.post(booking)
        assertEquals(1, shadowManager.size())
        assertNotNull(shadowManager.getNotification("push:booking:$B1", PushNotificationFactory.PUSH_NOTIFICATION_ID))

        poster.post(booking)
        assertEquals(1, shadowManager.size())
    }

    private companion object {
        const val B1 = "11111111-1111-4111-8111-111111111111"
        const val B2 = "22222222-2222-4222-8222-222222222222"
        const val T1 = "33333333-3333-4333-8333-333333333333"
    }
}
