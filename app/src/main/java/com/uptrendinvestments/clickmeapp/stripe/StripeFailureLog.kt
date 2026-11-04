package com.uptrendinvestments.clickmeapp.stripe

import android.util.Log
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult

/** Logcat tag for Stripe sheet and challenge failures. The release build keeps these lines, like `ClickMeCall`. */
const val STRIPE_LOG_TAG = "StripeSheet"

/** The status code of a [StripeException] that has none. Stripe's own constant is restricted to its library group. */
private const val NO_STATUS_CODE = 0

/**
 * BUG-08: what a Stripe failure was, for the log only. The user sees the app's own copy for anything that is not a
 * card error ([userFacingStripeMessage]), so without this line the real cause is gone.
 *
 * Only the exception class, the cause's class, and for a [StripeException] its Stripe error type and code, HTTP
 * status and request id. Never the message: Stripe's text can echo request details, and a client secret must not
 * reach logcat.
 */
internal fun describeStripeFailure(error: Throwable?): String {
    if (error == null) return "no throwable"
    val parts = mutableListOf(error.javaClass.simpleName)
    if (error is StripeException) {
        error.stripeError?.type?.let { parts += "type=$it" }
        error.stripeError?.code?.let { parts += "code=$it" }
        if (error.statusCode != NO_STATUS_CODE) parts += "status=${error.statusCode}"
        error.requestId?.let { parts += "request=$it" }
    }
    error.cause?.let { parts += "cause=${it.javaClass.simpleName}" }
    return parts.joinToString(" ")
}

/** Logs a failure from [source] (where it surfaced, for example "sheet" or "challenge"). Safe to call on any thread. */
fun logStripeFailure(
    source: String,
    error: Throwable?,
) {
    Log.w(STRIPE_LOG_TAG, "$source failed: ${describeStripeFailure(error)}")
}

/** Logs a failed [PaymentSheetResult] (see [logStripeFailure]) and returns the result unchanged. */
fun PaymentSheetResult.loggingFailure(source: String): PaymentSheetResult =
    also { if (it is PaymentSheetResult.Failed) logStripeFailure(source, it.error) }

/** Logs a failed [PaymentResult] (see [logStripeFailure]) and returns the result unchanged. */
fun PaymentResult.loggingFailure(source: String): PaymentResult =
    also { if (it is PaymentResult.Failed) logStripeFailure(source, it.throwable) }
