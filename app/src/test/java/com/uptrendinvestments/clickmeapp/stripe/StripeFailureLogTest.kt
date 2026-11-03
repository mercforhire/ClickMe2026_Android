package com.uptrendinvestments.clickmeapp.stripe

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.exception.CardException
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * BUG-08: a non-card Stripe failure shows the app's own "Couldn't present the payment sheet" copy, so the real cause
 * must reach logcat, and never with a message that could carry a client secret.
 */
@RunWith(RobolectricTestRunner::class)
class StripeFailureLogTest {
    private val secret = "seti_1Abc_secret_ZZZ"

    @Before
    fun clearLogs() = ShadowLog.clear()

    private fun logged() = ShadowLog.getLogsForTag(STRIPE_LOG_TAG)

    private fun failedSheet(error: Throwable): PaymentSheetResult =
        PaymentSheetResult.Failed::class.java
            .getConstructor(Throwable::class.java)
            .newInstance(error)

    @Test
    fun aSdkStateFailureLogsItsClassAndNeverItsMessage() {
        logStripeFailure("sheet", IllegalStateException("The host activity is not in a valid state. $secret"))

        val line = logged().single().msg
        assertTrue(line, line.contains("IllegalStateException"))
        assertFalse(line, line.contains(secret))
        assertFalse(line, line.contains("host activity"))
    }

    @Test
    fun aStripeApiFailureLogsTypeCodeStatusAndRequestIdOnly() {
        val error =
            APIException(
                stripeError = StripeError(type = "invalid_request_error", code = "resource_missing", message = secret),
                requestId = "req_123",
                statusCode = 404,
                message = "No such setupintent: '$secret'",
            )

        logStripeFailure("configure", error)

        val line = logged().single().msg
        assertEquals(
            "configure failed: APIException type=invalid_request_error code=resource_missing status=404 request=req_123",
            line,
        )
        assertFalse(line, line.contains(secret))
    }

    @Test
    fun aLocalStripeFailureLogsItsClassAndType() {
        logStripeFailure(
            "challenge",
            LocalStripeException(
                displayMessage = secret,
                analyticsValue = "failedIntentOutcomeError",
                type = "api_error",
            ),
        )

        val line = logged().single().msg
        assertTrue(line, line.startsWith("challenge failed: LocalStripeException"))
        assertFalse(line, line.contains(secret))
    }

    @Test
    fun theCauseClassIsLoggedWithoutItsMessage() {
        logStripeFailure("sheet", RuntimeException("wrapper $secret", java.io.IOException("socket $secret")))

        val line = logged().single().msg
        assertEquals("sheet failed: RuntimeException cause=IOException", line)
    }

    @Test
    fun aMissingThrowableIsStillLogged() {
        logStripeFailure("sheet", null)
        assertEquals("sheet failed: no throwable", logged().single().msg)
    }

    @Test
    fun aFailedSheetResultIsLoggedAndReturnedUnchanged() {
        val result = failedSheet(IllegalStateException(secret))

        assertEquals(result, result.loggingFailure("sheet"))
        assertTrue(logged().single().msg.contains("IllegalStateException"))
    }

    @Test
    fun aFailedChallengeResultIsLoggedAndReturnedUnchanged() {
        val result = PaymentResult.Failed(CardException(StripeError(type = "card_error", code = "card_declined")))

        assertEquals(result, result.loggingFailure("challenge"))
        assertTrue(logged().single().msg.contains("code=card_declined"))
    }

    @Test
    fun completedAndCanceledResultsLogNothing() {
        PaymentResult.Completed.loggingFailure("challenge")
        PaymentResult.Canceled.loggingFailure("challenge")
        assertTrue(logged().isEmpty())
    }
}
