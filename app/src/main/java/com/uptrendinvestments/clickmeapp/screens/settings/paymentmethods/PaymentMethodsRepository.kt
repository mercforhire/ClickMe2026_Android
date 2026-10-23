package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saved-card management. Mirrors iOS `Settings/Payment Methods` (D-04). Every call throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Shared by the Payment Methods screen and the booking gate (D-08). The server owns the default card (D-07),
 * so the app re-lists after a change instead of tracking it locally. `setup-intent` is rate-limited to 10
 * per 15 minutes per user, so callers must not auto-retry it (research correction 4).
 */
@Singleton
class PaymentMethodsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /me/payment-methods`, in server order. */
        suspend fun list(): List<PaymentMethodItem> = apiCall { api.getPaymentMethods() }.data.paymentMethods

        /** `POST /me/payment-methods/setup-intent`: the secrets the Stripe PaymentSheet needs to save a card. */
        suspend fun createSetupIntent(): PaymentMethodSetupData = apiCall { api.createPaymentMethodSetupIntent() }.data

        /** `DELETE /me/payment-methods/{id}`. The id must come from [list]; the server 404s on foreign ids. */
        suspend fun delete(paymentMethodId: String) {
            apiCall { api.deletePaymentMethod(paymentMethodId) }
        }

        /** `PATCH /me/payment-methods/{id}/default`. */
        suspend fun setDefault(paymentMethodId: String) {
            apiCall { api.setDefaultPaymentMethod(paymentMethodId) }
        }
    }
