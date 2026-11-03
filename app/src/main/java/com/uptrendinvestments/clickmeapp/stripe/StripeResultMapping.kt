package com.uptrendinvestments.clickmeapp.stripe

import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult

/** Stripe's `StripeError.type` for errors written for the card holder (declines, failed authentication). */
private const val STRIPE_CARD_ERROR = "card_error"

/**
 * WR-05: the text of a Stripe failure that may be shown to the user, or null. Only card errors qualify: their
 * message is written for card holders ("Your card was declined."), the same rule the Stripe SDK applies to its own
 * error text. Every other failure (SDK state such as "The host activity is not in a valid state (DESTROYED).", an
 * unconfigured FlowController, network or API errors) carries developer text, so it maps to null and the caller
 * shows the app's own copy instead.
 */
fun Throwable?.userFacingStripeMessage(): String? {
    val stripe = this as? StripeException ?: return null
    if (stripe.stripeError?.type != STRIPE_CARD_ERROR) return null
    return stripe.message?.takeIf { it.isNotBlank() }
}

/** Maps the PaymentSheet result to the SDK-free [SheetOutcome] the ViewModels understand. */
fun PaymentSheetResult.toSheetOutcome(): SheetOutcome =
    when (this) {
        is PaymentSheetResult.Completed -> SheetOutcome.Completed
        is PaymentSheetResult.Canceled -> SheetOutcome.Canceled
        is PaymentSheetResult.Failed -> SheetOutcome.Failed(error.userFacingStripeMessage())
    }

/** Maps the PaymentLauncher (3DS challenge) result to the SDK-free [ChallengeOutcome]. */
fun PaymentResult.toChallengeOutcome(): ChallengeOutcome =
    when (this) {
        is PaymentResult.Completed -> ChallengeOutcome.Completed
        is PaymentResult.Canceled -> ChallengeOutcome.Canceled
        is PaymentResult.Failed -> ChallengeOutcome.Failed(throwable.userFacingStripeMessage())
    }

/**
 * Whether the FlowController selection should be confirmed after its sheet closes.
 *
 * D-18, decided 2026-10-22: dismissing the sheet cancels silently even when a saved card is preselected. This
 * deliberately differs from iOS, which checks only `paymentOption != nil` and authorizes the preselected card
 * (RESEARCH Pitfall 6).
 */
fun shouldConfirmSelection(
    didCancel: Boolean,
    hasPaymentOption: Boolean,
): Boolean = !didCancel && hasPaymentOption

/**
 * Whether a FlowController option callback is the SDK clearing its selection after a completed setup, not a sheet
 * closing. `DefaultFlowController.onPaymentResult` sends `PaymentOptionResult(null, false)` and only then launches
 * the `Completed` result, so treating it as a dismissal resets the flow and drops the `Completed`.
 *
 * Checked against every call site in Stripe 23.24.0; recheck on an SDK upgrade. A real dismissal always has
 * `didCancel = true`, including a dismissal that leaves no selection. Every other `didCancel = false` call carries a
 * non-null option.
 */
fun isPostCompletionReset(
    didCancel: Boolean,
    hasPaymentOption: Boolean,
): Boolean = !didCancel && !hasPaymentOption
