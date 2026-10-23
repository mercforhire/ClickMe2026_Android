@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /bookings/payment-intent` body (short-lead paid path, PAY-03). The backend schema is strict: exactly
 * these keys. No amount or currency is ever sent (the server derives the price from the topic), and
 * `promo_code` is never declared (out of scope).
 *
 * @property selectedDate UTC `yyyy-MM-dd`.
 * @property timeSlot UTC `HH:mm`.
 * @property durationMinutes a multiple of 30.
 * @property bookingAttemptId a UUID string identifying this booking attempt on the payment-intent call only.
 */
@JsonClass(generateAdapter = true)
data class PaymentIntentRequest(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "topic_id") val topicId: String,
    @Json(name = "selected_date") val selectedDate: String,
    @Json(name = "time_slot") val timeSlot: String,
    @Json(name = "duration_minutes") val durationMinutes: Int,
    @Json(name = "booking_attempt_id") val bookingAttemptId: String,
)

/**
 * `POST /bookings/confirm` body. The backend schema is strict. It must never carry `booking_attempt_id`
 * (research correction 5).
 *
 * @property paymentIntentId null is omitted by the converter (no `withNullSerialization`), which routes the
 * server to the long-lead branch (PAY-04). The strict schema rejects an explicit `null`.
 * @property startTime ISO-8601 instant.
 * @property meetingType wire value of the meeting type.
 * @property stripePaymentMethodId the saved `pm_...` id.
 * @property clientNotes null is omitted; never send an empty string.
 */
@JsonClass(generateAdapter = true)
data class ConfirmBookingRequest(
    @Json(name = "payment_intent_id") val paymentIntentId: String? = null,
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "topic_id") val topicId: String,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "meeting_type") val meetingType: String,
    @Json(name = "stripe_payment_method_id") val stripePaymentMethodId: String,
    @Json(name = "client_notes") val clientNotes: String? = null,
)
