package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /bookings/payment-intent` response. A bare body without the `{status, data}` envelope.
 *
 * The server also sends `total_amount` and `summary` as decimal major units (the PROJECT money exception).
 * They are deliberately not declared, so a `Long` adapter can never throw on `42.5` and no payment-intent
 * amount can reach the UI (D-12). `customer_id` and `ephemeral_key` are ignored too.
 *
 * `client_secret` is the booking PaymentIntent secret and is never used client-side (Path A: the server
 * confirms), so [toString] prints only the id.
 */
@JsonClass(generateAdapter = true)
data class PaymentIntentResponse(
    @Json(name = "payment_intent_id") val paymentIntentId: String,
    @Json(name = "client_secret") val clientSecret: String,
    val currency: String? = null,
) {
    override fun toString(): String = "PaymentIntentResponse(paymentIntentId=$paymentIntentId)"
}
