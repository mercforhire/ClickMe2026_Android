package com.uptrendinvestments.clickmeapp

import android.content.Context
import android.content.Intent
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

    private companion object {
        const val BOOKING = "11111111-1111-4111-8111-111111111111"
    }
}
