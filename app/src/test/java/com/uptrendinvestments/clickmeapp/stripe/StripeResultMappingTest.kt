package com.uptrendinvestments.clickmeapp.stripe

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.exception.CardException
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StripeResultMappingTest {
    /**
     * Stripe 23.24.0 gives the PaymentSheetResult classes internal constructors, which are public in bytecode, so the
     * tests build real results through reflection and still exercise the production mapping.
     */
    private fun completedResult(): PaymentSheetResult =
        PaymentSheetResult.Completed::class.java
            .getConstructor(Boolean::class.javaPrimitiveType)
            .newInstance(true)

    private fun canceledResult(): PaymentSheetResult =
        PaymentSheetResult.Canceled::class.java
            .getConstructor(Boolean::class.javaPrimitiveType)
            .newInstance(true)

    private fun failedResult(error: Throwable): PaymentSheetResult =
        PaymentSheetResult.Failed::class.java
            .getConstructor(Throwable::class.java)
            .newInstance(error)

    @Test
    fun paymentSheetCompletedMapsToCompleted() {
        assertEquals(SheetOutcome.Completed, completedResult().toSheetOutcome())
    }

    @Test
    fun paymentSheetCanceledMapsToCanceled() {
        assertEquals(SheetOutcome.Canceled, canceledResult().toSheetOutcome())
    }

    private fun cardError(message: String?) = CardException(StripeError(type = "card_error", message = message))

    /** What PaymentLauncher reports for a failed intent outcome, typed with the intent's last error. */
    private fun intentFailure(
        message: String?,
        type: String?,
    ) = LocalStripeException(displayMessage = message, analyticsValue = "failedIntentOutcomeError", type = type)

    @Test
    fun paymentSheetFailedCarriesACardErrorMessage() {
        val result = failedResult(cardError("Your card was declined."))
        assertEquals(SheetOutcome.Failed("Your card was declined."), result.toSheetOutcome())
    }

    /** WR-05: SDK state errors (a destroyed host after rotation, an unconfigured controller) are developer text. */
    @Test
    fun paymentSheetFailedHidesDeveloperText() {
        listOf(
            IllegalStateException("The host activity is not in a valid state (DESTROYED)."),
            IllegalStateException("FlowController must be successfully initialized"),
            APIException(message = "No such setupintent: 'seti_1'"),
            intentFailure("Invalid request", type = "invalid_request_error"),
            cardError(" "),
        ).forEach { error ->
            val outcome = failedResult(error).toSheetOutcome()
            assertEquals(error.toString(), SheetOutcome.Failed(null), outcome)
        }
    }

    @Test
    fun userFacingMessageIsNullForNoThrowable() {
        assertNull(null.userFacingStripeMessage())
    }

    @Test
    fun paymentResultCompletedMapsToCompleted() {
        assertEquals(ChallengeOutcome.Completed, PaymentResult.Completed.toChallengeOutcome())
    }

    @Test
    fun paymentResultCanceledMapsToCanceled() {
        assertEquals(ChallengeOutcome.Canceled, PaymentResult.Canceled.toChallengeOutcome())
    }

    @Test
    fun paymentResultFailedCarriesACardErrorMessage() {
        val text = "We are unable to authenticate your payment method."
        val result = PaymentResult.Failed(intentFailure(text, "card_error"))
        assertEquals(ChallengeOutcome.Failed(text), result.toChallengeOutcome())
    }

    @Test
    fun paymentResultFailedHidesDeveloperText() {
        listOf(
            RuntimeException("Auth failed"),
            intentFailure("Unknown error: unexpected state", type = null),
            APIException(message = "No such payment_intent"),
        ).forEach { error ->
            val outcome = PaymentResult.Failed(error).toChallengeOutcome()
            assertEquals(error.toString(), ChallengeOutcome.Failed(null), outcome)
        }
    }

    @Test
    fun selectionConfirmsOnlyWhenNotDismissedAndAnOptionExists() {
        assertTrue(shouldConfirmSelection(didCancel = false, hasPaymentOption = true))
        assertFalse(shouldConfirmSelection(didCancel = true, hasPaymentOption = true))
        assertFalse(shouldConfirmSelection(didCancel = false, hasPaymentOption = false))
        assertFalse(shouldConfirmSelection(didCancel = true, hasPaymentOption = false))
    }
}
