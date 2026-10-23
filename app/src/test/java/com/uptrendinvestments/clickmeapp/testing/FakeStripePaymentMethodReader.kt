package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.stripe.StripePaymentMethodReader
import kotlinx.coroutines.CompletableDeferred

/**
 * Scripted [StripePaymentMethodReader]. [result] is what every read returns (null models "could not be read");
 * [requestedSecrets] records each SetupIntent secret it was asked about. Set [gate] to hold a read in flight
 * until the test completes it.
 */
class FakeStripePaymentMethodReader : StripePaymentMethodReader {
    var result: String? = "pm_test"
    var gate: CompletableDeferred<Unit>? = null
    private val secrets = mutableListOf<String>()
    val requestedSecrets: List<String> get() = secrets

    override suspend fun paymentMethodIdOf(setupIntentClientSecret: String): String? {
        secrets += setupIntentClientSecret
        gate?.await()
        return result
    }
}
