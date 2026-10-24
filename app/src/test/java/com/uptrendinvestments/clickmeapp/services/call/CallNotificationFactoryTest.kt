package com.uptrendinvestments.clickmeapp.services.call

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.MainActivity
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallNotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class CallNotificationFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val start = Instant.parse("2026-12-01T15:00:00Z")

    private fun info(name: String? = "Ada") = CallNotificationInfo("b1", name, start)

    private fun build(
        name: String? = "Ada",
        now: Instant = start.minusSeconds(600),
    ): Notification = CallNotificationFactory.build(context, info(name), now) { "3:00 PM" }

    @Test
    fun createChannel_registersLowImportanceChannel() {
        CallNotificationFactory.createChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel("ongoing_call")
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertEquals("Ongoing call", channel.name.toString())
        assertEquals("Shown while you are on a ClickMe voice call.", channel.description)
    }

    @Test
    fun beforeStart_showsTitleAndStartsAtText() {
        val n = build()
        assertEquals("ongoing_call", n.channelId)
        assertEquals("Call with Ada", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("Starts at 3:00 PM", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertFalse(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(Notification.CATEGORY_CALL, n.category)
        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility)
        assertEquals(R.drawable.ic_stat_call, n.smallIcon.resId)
    }

    @Test
    fun publicVersion_hasNoPeerName() {
        val public = build().publicVersion
        assertNotNull(public)
        val title = public.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        assertEquals("ClickMe call in progress", title)
        assertFalse(title.contains("Ada"))
    }

    @Test
    fun fromStart_usesChronometerAnchoredAtScheduledStart() {
        val n = build(now = start.plusSeconds(5))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals(start.toEpochMilli(), n.`when`)
        assertNull(n.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun atExactlyStart_usesChronometer() {
        val n = build(now = start)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
    }

    @Test
    fun missingOrBlankName_usesFallbackTitle() {
        listOf(null, "", "   ").forEach { name ->
            val title = build(name).extras.getCharSequence(Notification.EXTRA_TITLE).toString()
            assertEquals("ClickMe call", title)
        }
    }

    @Test
    fun hasExactlyOneEndActionAndNoMute() {
        val n = build()
        assertEquals(1, n.actions.size)
        val action = n.actions[0]
        assertEquals("End call", action.title.toString())
        assertFalse(n.actions.any { it.title.toString().contains("Mute", ignoreCase = true) })
        val shadow = shadowOf(action.actionIntent)
        assertTrue(shadow.isServiceIntent)
        assertEquals(CallService::class.java.name, shadow.savedIntent.component?.className)
        assertEquals(CallService.ACTION_END, shadow.savedIntent.action)
        assertTrue(action.actionIntent.isImmutableFlag())
    }

    @Test
    fun contentIntent_opensMainActivityExpandedAndImmutable() {
        val pi = build().contentIntent
        val shadow = shadowOf(pi)
        assertTrue(shadow.isActivityIntent)
        assertEquals(MainActivity::class.java.name, shadow.savedIntent.component?.className)
        assertTrue(shadow.savedIntent.getBooleanExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, false))
        assertTrue(pi.isImmutableFlag())
    }

    private fun PendingIntent.isImmutableFlag(): Boolean = shadowOf(this).flags and PendingIntent.FLAG_IMMUTABLE != 0
}
