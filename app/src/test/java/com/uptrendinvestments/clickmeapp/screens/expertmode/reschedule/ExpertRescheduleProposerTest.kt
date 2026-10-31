package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.ExpertRescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.ProposeOutcome
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ExpertRescheduleProposerTest {
    private val api = FakeClickMeApi()
    private val proposer = ExpertRescheduleProposer(ExpertBookingsRepository(api))
    private val ok = RescheduleResponseData(bookingId = "b1", status = "pending_reschedule")
    private val start = Instant.parse("2026-11-02T15:00:00.250Z")

    private fun failedCode(outcome: ProposeOutcome): ErrorCode? =
        ((outcome as ProposeOutcome.Failed).error as ApiException.Http).code

    @Test
    fun sendsOnePatchWithStartZoneAndNote() =
        runTest {
            api.rescheduleExpertBookingHandler = { _, _ -> ok }

            assertEquals(ProposeOutcome.Sent, proposer.propose("b1", start, ZoneId.of("UTC"), "Conflict"))
            assertEquals(1, api.rescheduleExpertBookingCalls)
            assertEquals(0, api.getPaymentMethodsCalls)
            assertEquals(
                listOf(ExpertRescheduleProposalRequest("2026-11-02T15:00:00Z", "UTC", "Conflict")),
                api.rescheduleExpertBookingRequests,
            )
        }

    @Test
    fun paymentMethodRequiredIsJustFailed() =
        runTest {
            api.rescheduleExpertBookingHandler =
                { _, _ -> throw api.httpError(422, """{"code":"PAYMENT_METHOD_REQUIRED","message":"x"}""") }

            val outcome = proposer.propose("b1", start, ZoneId.of("UTC"), null)

            assertEquals(ErrorCode.PaymentMethodRequired, failedCode(outcome))
            assertEquals(0, api.getPaymentMethodsCalls)
            assertEquals(1, api.rescheduleExpertBookingCalls)
        }

    @Test
    fun anyErrorIsFailed() =
        runTest {
            api.rescheduleExpertBookingHandler =
                { _, _ -> throw api.httpError(422, """{"code":"RESCHEDULE_OUTSIDE_AUTH_WINDOW","message":"x"}""") }

            val outcome = proposer.propose("b1", start, ZoneId.of("UTC"), null)

            assertEquals(ErrorCode.RescheduleOutsideAuthWindow, failedCode(outcome))
            assertEquals(0, api.getPaymentMethodsCalls)
            assertEquals(1, api.rescheduleExpertBookingCalls)
        }
}
