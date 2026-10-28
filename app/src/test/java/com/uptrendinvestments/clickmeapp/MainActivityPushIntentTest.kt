package com.uptrendinvestments.clickmeapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushEvent
import com.uptrendinvestments.clickmeapp.services.call.CallNotificationFactory
import com.uptrendinvestments.clickmeapp.services.push.ForegroundPush
import com.uptrendinvestments.clickmeapp.services.push.PushNotificationFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivityPushIntentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun postedNotificationIntentRoutesToBookingDetail() {
        val push =
            ForegroundPush(
                mapOf("category" to "booking", "sub_category" to "confirmations", "booking_id" to BOOKING),
                "Booking confirmed",
                "Your booking was confirmed",
                null,
            )
        val pending = PushNotificationFactory.contentIntent(context, push, "push:booking:$BOOKING")
        val intent = shadowOf(pending).savedIntent
        assertNotNull(intent)

        assertEquals(PushDestination.BookingDetail(BOOKING, PushAudience.Client), intent.consumePushDestination())
        PushEvent.PUSH_KEYS.forEach { assertFalse(intent.hasExtra(it)) }
    }

    @Test
    fun plainLauncherIntentPostsNothing() {
        assertNull(Intent(Intent.ACTION_MAIN).consumePushDestination())

        val call = Intent().putExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, true)
        assertNull(call.consumePushDestination())
        assertTrue(call.getBooleanExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, false))
    }

    @Test
    fun restoredActivityDoesNotReread() {
        val fresh = Intent(Intent.ACTION_MAIN)
        val fromHistory = Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        assertFalse(shouldReadLaunchPush(Bundle(), fresh))
        assertFalse(shouldReadLaunchPush(null, fromHistory))
        assertFalse(shouldReadLaunchPush(null, null))
        assertTrue(shouldReadLaunchPush(null, fresh))
    }

    @Test
    fun extrasConsumedOnce() {
        val intent = Intent().putExtra("booking_id", BOOKING).putExtra("category", "booking")

        assertEquals(PushDestination.BookingDetail(BOOKING, PushAudience.Either), intent.consumePushDestination())
        assertNull(intent.consumePushDestination())
    }

    @Test
    fun unknownCategoryOpensList() {
        val promo = Intent().putExtra("category", "promo")
        val badId = Intent().putExtra("booking_id", "not-a-uuid")

        assertEquals(PushDestination.NotificationsList, promo.consumePushDestination())
        assertEquals(PushDestination.NotificationsList, badId.consumePushDestination())
    }

    @Test
    fun nonStringExtrasIgnored() {
        assertNull(Intent().putExtra("booking_id", 42).consumePushDestination())
    }

    @Test
    fun nullIntentPostsNothing() {
        val intent: Intent? = null

        assertNull(intent.consumePushDestination())
    }

    private companion object {
        const val BOOKING = "11111111-1111-4111-8111-111111111111"
    }
}
