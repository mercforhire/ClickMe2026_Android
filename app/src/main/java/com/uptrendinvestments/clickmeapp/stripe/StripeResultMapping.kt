package com.uptrendinvestments.clickmeapp.stripe

import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult

/** Maps the PaymentSheet result to the SDK-free [SheetOutcome] the ViewModels understand. */
fun PaymentSheetResult.toSheetOutcome(): SheetOutcome =
    when (this) {
        is PaymentSheetResult.Completed -> SheetOutcome.Completed
        is PaymentSheetResult.Canceled -> SheetOutcome.Canceled
        is PaymentSheetResult.Failed -> SheetOutcome.Failed(error.message)
    }

/** Maps the PaymentLauncher (3DS challenge) result to the SDK-free [ChallengeOutcome]. */
fun PaymentResult.toChallengeOutcome(): ChallengeOutcome =
    when (this) {
        is PaymentResult.Completed -> ChallengeOutcome.Completed
        is PaymentResult.Canceled -> ChallengeOutcome.Canceled
        is PaymentResult.Failed -> ChallengeOutcome.Failed(throwable.message)
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
