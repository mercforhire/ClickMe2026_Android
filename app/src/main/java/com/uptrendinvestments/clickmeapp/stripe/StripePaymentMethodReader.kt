package com.uptrendinvestments.clickmeapp.stripe

import android.content.Context
import com.stripe.android.Stripe
import com.stripe.android.core.exception.StripeException
import com.uptrendinvestments.clickmeapp.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * FlowController exposes only a `PaymentOption` label, never the id, so the `pm_` id of the card the user just
 * set up is read from the confirmed SetupIntent (iOS `retrieveSetupIntent` parity, RESEARCH Pattern 4). ViewModels
 * depend on this interface so they never import the Stripe SDK.
 */
interface StripePaymentMethodReader {
    /** Returns the `pm_` id attached to the SetupIntent, or null when it cannot be read. */
    suspend fun paymentMethodIdOf(setupIntentClientSecret: String): String?
}

/**
 * Reads the SetupIntent on the platform account: the SetupIntent lives there, and only the 3DS step is scoped to
 * a connected account (Phase 2 D-12), so no `stripeAccountId` is passed. The client secret is a call argument
 * only; it is never logged or stored, and failures map to null without their message being surfaced.
 */
class DefaultStripePaymentMethodReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : StripePaymentMethodReader {
        override suspend fun paymentMethodIdOf(setupIntentClientSecret: String): String? =
            withContext(Dispatchers.IO) {
                try {
                    Stripe(context, AppConfig.stripePublishableKey)
                        .retrieveSetupIntentSynchronous(setupIntentClientSecret)
                        .paymentMethodId
                } catch (_: StripeException) {
                    null
                } catch (_: IOException) {
                    null
                }
            }
    }
