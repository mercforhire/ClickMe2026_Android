package com.uptrendinvestments.clickmeapp.stripe

import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StripeResultMappingTest {
    @Test
    fun paymentSheetCompletedMapsToCompleted() {
        assertEquals(SheetOutcome.Completed, PaymentSheetResult.Completed.toSheetOutcome())
    }

    @Test
    fun paymentSheetCanceledMapsToCanceled() {
        assertEquals(SheetOutcome.Canceled, PaymentSheetResult.Canceled.toSheetOutcome())
    }

    @Test
    fun paymentSheetFailedCarriesTheErrorMessage() {
        val result = PaymentSheetResult.Failed(IllegalStateException("Card declined"))
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
