package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

class ClientRescheduleProposerTest {
    private val api = FakeClickMeApi()
    private val proposer = ClientRescheduleProposer(ClientBookingsRepository(api), PaymentMethodsRepository(api))
    private val ok = RescheduleResponseData(bookingId = "b1", status = "pending_reschedule")
    private val start = Instant.parse("2026-11-02T15:00:00.250Z")
    private val zone = ZoneId.of("America/Toronto")

    private fun card(
        id: String,
        isDefault: Boolean = false,
    ) = PaymentMethodItem(id = id, brand = "visa", last4 = "4242", isDefault = isDefault)

    private fun serverError(
        status: Int,
        code: String,
    ) = api.httpError(status, """{"code":"$code","message":"Server says no"}""")

    private fun paymentMethodRequired() = serverError(422, "PAYMENT_METHOD_REQUIRED")

    private fun sentCards() = api.rescheduleClientBookingRequests.map { it.stripePaymentMethodId }

    private suspend fun propose(note: String? = null) = proposer.propose("b1", start, zone, note)

    private fun failedCode(outcome: ProposeOutcome): ErrorCode? =
        ((outcome as ProposeOutcome.Failed).error as ApiException.Http).code

    @Test
    fun firstTrySuccess() =
        runTest {
            api.rescheduleClientBookingHandler = { _, _ -> ok }

            assertEquals(ProposeOutcome.Sent, propose("Running late"))
            assertEquals(1, api.rescheduleClientBookingCalls)
            assertEquals(0, api.getPaymentMethodsCalls)
            assertEquals(
                listOf(RescheduleProposalRequest("2026-11-02T15:00:00Z", "America/Toronto", "Running late", null)),
                api.rescheduleClientBookingRequests,
            )
        }

    @Test
    fun pmrRetriesOnceWithDefaultCard() =
        runTest {
            api.rescheduleClientBookingHandler = { _, body ->
                if (body.stripePaymentMethodId == null) throw paymentMethodRequired() else ok
            }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_a"), card("pm_b", isDefault = true))) }

            assertEquals(ProposeOutcome.Sent, propose())
            assertEquals(2, api.rescheduleClientBookingCalls)
            assertEquals(1, api.getPaymentMethodsCalls)
            assertEquals(listOf<String?>(null, "pm_b"), sentCards())
        }

    @Test
    fun pmrWithoutDefaultUsesFirstCard() =
        runTest {
            api.rescheduleClientBookingHandler = { _, body ->
                if (body.stripePaymentMethodId == null) throw paymentMethodRequired() else ok
            }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_a"), card("pm_b"))) }

            assertEquals(ProposeOutcome.Sent, propose())
            assertEquals(listOf<String?>(null, "pm_a"), sentCards())
        }

    @Test
    fun pmrWithEmptyVaultNeedsPaymentMethod() =
        runTest {
            api.rescheduleClientBookingHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { PaymentMethodsData(emptyList()) }

            assertEquals(ProposeOutcome.NeedsPaymentMethod, propose())
            assertEquals(1, api.rescheduleClientBookingCalls)
        }

    @Test
    fun pmrThenListFailureIsCardsUnavailable() =
        runTest {
            api.rescheduleClientBookingHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { throw IOException("offline") }

            assertEquals(ProposeOutcome.CardsUnavailable, propose())
            assertEquals(1, api.rescheduleClientBookingCalls)
        }

    @Test
    fun secondPmrIsCardUnusable() =
        runTest {
            api.rescheduleClientBookingHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_a"), card("pm_b", isDefault = true))) }

            assertEquals(ProposeOutcome.CardUnusable, propose())
            assertEquals(2, api.rescheduleClientBookingCalls)
            assertEquals(1, api.getPaymentMethodsCalls)
            assertEquals(listOf<String?>(null, "pm_b"), sentCards())
        }

    @Test
    fun otherErrorOnFirstTryFails() =
        runTest {
            api.rescheduleClientBookingHandler = { _, _ -> throw serverError(422, "RESCHEDULE_OUTSIDE_AUTH_WINDOW") }

            val outcome = propose()

            assertTrue(outcome is ProposeOutcome.Failed)
            assertEquals(ErrorCode.RescheduleOutsideAuthWindow, failedCode(outcome))
            assertEquals(0, api.getPaymentMethodsCalls)
            assertEquals(1, api.rescheduleClientBookingCalls)
        }

    @Test
    fun otherErrorOnRetryFails() =
        runTest {
            api.rescheduleClientBookingHandler = { _, body ->
                if (body.stripePaymentMethodId == null) {
                    throw paymentMethodRequired()
                } else {
                    throw serverError(422, "INVALID_PAYMENT_METHOD")
                }
            }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_b", isDefault = true))) }

            val outcome = propose()

            assertEquals(ErrorCode.InvalidPaymentMethod, failedCode(outcome))
            assertEquals(2, api.rescheduleClientBookingCalls)
        }
}
