@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `GET /me/payment-methods` payload. The list keeps server order. */
@JsonClass(generateAdapter = true)
data class PaymentMethodsData(
    @Json(name = "payment_methods") val paymentMethods: List<PaymentMethodItem>,
)

/**
 * One saved card. The server maps `pm.card && pm.card.last4`, so every card field may be null. The brand is
 * lowercased by the server and unknown brands arrive as `unknown`.
 */
@JsonClass(generateAdapter = true)
data class PaymentMethodItem(
    val id: String,
    val brand: String? = null,
    val last4: String? = null,
    @Json(name = "exp_month") val expMonth: Int? = null,
    @Json(name = "exp_year") val expYear: Int? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

/**
 * `POST /me/payment-methods/setup-intent` payload for the Stripe PaymentSheet. [clientSecret] and
 * [ephemeralKey] are secrets, so [toString] prints only the customer id.
 */
@JsonClass(generateAdapter = true)
data class PaymentMethodSetupData(
    @Json(name = "client_secret") val clientSecret: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "ephemeral_key") val ephemeralKey: String,
) {
    override fun toString(): String = "PaymentMethodSetupData(customerId=$customerId)"
}
