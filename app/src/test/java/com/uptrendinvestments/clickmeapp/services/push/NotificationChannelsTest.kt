package com.uptrendinvestments.clickmeapp.services.push

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.services.call.CallNotificationFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationChannelsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = NotificationManagerCompat.from(context)

    @Test
    fun fiveContractIds() {
        assertEquals(
            listOf("bookings", "sessions", "messages", "account_payouts", "general"),
            NotificationChannels.ALL_IDS,
        )
    }

    @Test
    fun channelIdForMapsEveryCategory() {
        assertEquals("bookings", NotificationChannels.channelIdFor("booking"))
        assertEquals("sessions", NotificationChannels.channelIdFor("session"))
        assertEquals("sessions", NotificationChannels.channelIdFor("review"))
        assertEquals("messages", NotificationChannels.channelIdFor("message"))
        assertEquals("account_payouts", NotificationChannels.channelIdFor("payout"))
        assertEquals("account_payouts", NotificationChannels.channelIdFor("account"))
        assertEquals("general", NotificationChannels.channelIdFor("promo"))
        assertEquals("general", NotificationChannels.channelIdFor(""))
        assertEquals("general", NotificationChannels.channelIdFor(null))
        assertEquals("bookings", NotificationChannels.channelIdFor(" Booking "))
        assertEquals("account_payouts", NotificationChannels.channelIdFor("PAYOUT"))
    }

    @Test
    fun ensureCreatedMakesFiveChannels() {
        CallNotificationFactory.createChannel(context)
        NotificationChannels.ensureCreated(context)

        val expected =
            mapOf(
                NotificationChannels.BOOKINGS to
                    Triple(
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                        R.string.push_channel_bookings_name,
                        R.string.push_channel_bookings_description,
                    ),
                NotificationChannels.SESSIONS to
                    Triple(
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                        R.string.push_channel_sessions_name,
                        R.string.push_channel_sessions_description,
                    ),
                NotificationChannels.MESSAGES to
                    Triple(
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                        R.string.push_channel_messages_name,
                        R.string.push_channel_messages_description,
                    ),
                NotificationChannels.ACCOUNT_PAYOUTS to
                    Triple(
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                        R.string.push_channel_account_payouts_name,
                        R.string.push_channel_account_payouts_description,
                    ),
                NotificationChannels.GENERAL to
                    Triple(
                        NotificationManagerCompat.IMPORTANCE_DEFAULT,
                        R.string.push_channel_general_name,
                        R.string.push_channel_general_description,
                    ),
            )
        expected.forEach { (id, spec) ->
            val channel = manager.getNotificationChannelCompat(id)
            assertNotNull("missing channel $id", channel)
            assertEquals(id, spec.first, channel!!.importance)
            assertEquals(id, context.getString(spec.second), channel.name.toString())
            assertEquals(id, context.getString(spec.third), channel.description)
        }

        val call = manager.getNotificationChannelCompat(CallNotificationFactory.CHANNEL_ID)
        assertNotNull(call)
        assertEquals(NotificationManagerCompat.IMPORTANCE_LOW, call!!.importance)
    }

    @Test
    fun ensureCreatedIsIdempotent() {
        NotificationChannels.ensureCreated(context)
        NotificationChannels.ensureCreated(context)

        val pushChannels = manager.notificationChannelsCompat.filter { it.id in NotificationChannels.ALL_IDS }
        assertEquals(5, pushChannels.size)
        assertEquals(5, pushChannels.map { it.id }.toSet().size)
    }
}
