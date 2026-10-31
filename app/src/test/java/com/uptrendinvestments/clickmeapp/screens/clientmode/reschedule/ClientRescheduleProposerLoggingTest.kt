package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.time.Instant
import java.time.ZoneId

/** Robolectric companion of [ClientRescheduleProposerTest] (needs ShadowLog, which the plain JUnit class lacks). */
@RunWith(RobolectricTestRunner::class)
class ClientRescheduleProposerLoggingTest {
    @Test
    fun neverLogsCardIdOrNote() =
        runTest {
            val api = FakeClickMeApi()
            val proposer = ClientRescheduleProposer(ClientBookingsRepository(api), PaymentMethodsRepository(api))
            val cardId = "pm_secret_card_9f3a"
            val note = "distinctive-note-zq81"
            api.rescheduleClientBookingHandler = { _, body ->
                if (body.stripePaymentMethodId == null) {
                    throw api.httpError(422, """{"code":"PAYMENT_METHOD_REQUIRED","message":"Need card"}""")
                } else {
                    RescheduleResponseData(bookingId = "b1", status = "pending_reschedule")
                }
            }
            api.getPaymentMethodsHandler = {
                PaymentMethodsData(
                    listOf(PaymentMethodItem(id = cardId, brand = "visa", last4 = "4242", isDefault = true)),
                )
            }
            ShadowLog.clear()

            val outcome =
                proposer.propose("b1", Instant.parse("2026-11-02T15:00:00Z"), ZoneId.of("America/Toronto"), note)

            assertEquals(ProposeOutcome.Sent, outcome)
            assertEquals(
                listOf<String?>(null, cardId),
                api.rescheduleClientBookingRequests.map { it.stripePaymentMethodId },
            )
            val logged =
                ShadowLog.getLogs().joinToString(
                    "\n",
                ) { "${it.tag} ${it.msg} ${it.throwable?.stackTraceToString()}" }
            assertFalse("card id leaked into logs", logged.contains(cardId))
            assertFalse("card id prefix leaked into logs", logged.contains("pm_"))
            assertFalse("note leaked into logs", logged.contains(note))
        }
}
