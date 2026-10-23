package com.uptrendinvestments.clickmeapp.stripe

import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun paymentSheetFailedCarriesTheErrorMessage() {
        val result = failedResult(IllegalStateException("Card declined"))
        assertEquals(SheetOutcome.Failed("Card declined"), result.toSheetOutcome())
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
    fun paymentResultFailedCarriesTheThrowableMessage() {
        val result = PaymentResult.Failed(RuntimeException("Auth failed"))
        assertEquals(ChallengeOutcome.Failed("Auth failed"), result.toChallengeOutcome())
    }

    @Test
    fun selectionConfirmsOnlyWhenNotDismissedAndAnOptionExists() {
        assertTrue(shouldConfirmSelection(didCancel = false, hasPaymentOption = true))
        assertFalse(shouldConfirmSelection(didCancel = true, hasPaymentOption = true))
        assertFalse(shouldConfirmSelection(didCancel = false, hasPaymentOption = false))
        assertFalse(shouldConfirmSelection(didCancel = true, hasPaymentOption = false))
    }
}
