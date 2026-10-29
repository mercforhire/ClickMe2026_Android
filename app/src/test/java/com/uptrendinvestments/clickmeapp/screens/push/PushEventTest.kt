package com.uptrendinvestments.clickmeapp.screens.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PushEventTest {
    private val b1 = "0a1b2c3d-1111-4222-8333-444455556666"
    private val t1 = "9f8e7d6c-aaaa-4bbb-8ccc-ddddeeeeffff"

    private val fallbackTable =
        mapOf(
            "NEW_MESSAGE" to ("message" to "new_message"),
            "NEW_BOOKING_REQUEST" to ("booking" to "requests"),
            "BOOKING_REQUEST" to ("booking" to "requests"),
            "REQUEST_CREATED" to ("booking" to "requests"),
            "BOOKING_CONFIRMED" to ("booking" to "confirmations"),
            "BOOKING_DECLINED" to ("booking" to "declines"),
            "BOOKING_CANCELLED" to ("booking" to "cancellations"),
            "BOOKING_RESCHEDULED" to ("booking" to "reschedules"),
            "BOOKING_EXPIRED" to ("booking" to "expirations"),
            "BOOKING_COMPLETED" to ("session" to "ended"),
            "REVIEW_SUBMITTED" to ("review" to "received"),
            "REFUND_ISSUED" to ("booking" to "refunds"),
            "PAYMENT_ACTION_REQUIRED" to ("booking" to "payment_actions"),
            "PAYOUT_PAID" to ("payout" to "paid"),
            "PAYOUT_FAILED" to ("payout" to "failed"),
            "KYC_ACTION_REQUIRED" to ("payout" to "kyc_action"),
            "DISPUTE_CREATED" to ("payout" to "dispute"),
            "PASSWORD_CHANGED" to ("account" to "password_changed"),
            "DELETION_REQUESTED" to ("account" to "deletion_requested"),
        )

    @Test
    fun eventTypeFallbackCoversIosAndBackendValues() {
        assertEquals(19, fallbackTable.size)
        for ((eventType, expected) in fallbackTable) {
            for (variant in listOf(eventType, eventType.lowercase())) {
                val event = PushEvent.from(mapOf("event_type" to variant))
                assertNotNull(variant, event)
                assertEquals(variant, expected.first, event!!.category)
                assertEquals(variant, expected.second, event.subCategory)
            }
        }
    }

    @Test
    fun theBackendPaymentActionsPayloadParses() {
        val full =
            PushEvent.from(
                mapOf(
                    "category" to "booking",
                    "sub_category" to "payment_actions",
                    "booking_id" to b1,
                    "event_type" to "PAYMENT_ACTION_REQUIRED",
                ),
            )!!
        assertEquals("booking", full.category)
        assertEquals("payment_actions", full.subCategory)
        assertEquals(b1, full.bookingId)

        listOf("PAYMENT_ACTION_REQUIRED", "payment_action_required").forEach { eventType ->
            val fallback = PushEvent.from(mapOf("booking_id" to b1, "event_type" to eventType))!!
            assertEquals("booking", fallback.category)
            assertEquals("payment_actions", fallback.subCategory)
            assertEquals(b1, fallback.bookingId)
        }
    }

    @Test
    fun categoryWinsOverEventType() {
        val event = PushEvent.from(mapOf("category" to "session", "event_type" to "NEW_MESSAGE"))!!
        assertEquals("session", event.category)
        assertNull(event.subCategory)
    }

    @Test
    fun bookingIdAloneIsABookingPush() {
        val event = PushEvent.from(mapOf("booking_id" to b1))!!
        assertEquals("booking", event.category)
        assertNull(event.subCategory)
        assertEquals(b1, event.bookingId)
    }

    @Test
    fun idAliasesAccepted() {
        assertEquals(b1, PushEvent.from(mapOf("bookingId" to b1))!!.bookingId)
        for (key in listOf("thread_id", "threadId", "chatThreadId", "chat_thread_id")) {
            val event = PushEvent.from(mapOf("category" to "message", key to t1))!!
            assertEquals(key, t1, event.threadId)
        }
    }

    @Test
    fun nonUuidIdsAreDropped() {
        for (bad in listOf("../../admin", "12345", "", "{$b1}")) {
            val event = PushEvent.from(mapOf("category" to "booking", "booking_id" to bad))
            assertNull(bad, event?.bookingId)
        }
        val upper = PushEvent.from(mapOf("category" to "booking", "booking_id" to b1.uppercase()))!!
        assertEquals(b1, upper.bookingId)
    }

    @Test
    fun noPushKeysGivesNull() {
        assertNull(PushEvent.from(emptyMap()))
        assertNull(PushEvent.from(mapOf("foo" to "bar")))
        assertNull(PushEvent.from(mapOf("com.uptrendinvestments.clickmeapp.extra.EXPAND_CALL" to "true")))
    }

    @Test
    fun unknownCategoryStillParses() {
        val event = PushEvent.from(mapOf("category" to "promo"))
        assertNotNull(event)
        assertEquals("promo", event!!.category)
    }

    @Test
    fun fromRowUsesColumnsAndMetaData() {
        val event =
            PushEvent.fromRow(
                "review",
                "nudge",
                mapOf("booking_id" to b1, "event_type" to "X", "amount" to 12.5),
            )!!
        assertEquals("review", event.category)
        assertEquals("nudge", event.subCategory)
        assertEquals(b1, event.bookingId)
    }

    @Test
    fun fromRowWithNothingGivesNull() {
        assertNull(PushEvent.fromRow(null, null, null))
        assertNull(PushEvent.fromRow(" ", null, mapOf("amount" to 1)))
    }
}
