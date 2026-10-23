package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class RescheduleResponseTest {
    private val api = FakeClickMeApi()
    private val responder = RescheduleResponder(ClientBookingsRepository(api), PaymentMethodsRepository(api))
    private val ok = RescheduleResponseData(bookingId = "b1", status = "confirmed")

    private fun card(
        id: String,
        isDefault: Boolean = false,
    ) = PaymentMethodItem(id = id, brand = "visa", last4 = "4242", isDefault = isDefault)

    private fun paymentMethodRequired() =
        api.httpError(422, """{"code":"PAYMENT_METHOD_REQUIRED","message":"Payment method required"}""")

    private fun sentCards() = api.acceptClientRescheduleRequests.map { it.second.stripePaymentMethodId }

    private fun show(
        id: Int,
        reload: Boolean = false,
    ) = RescheduleFailure.Show(FieldError.Res(id), reload = reload)

    // ---- accept ----

    @Test
    fun accept_succeeds_first_time_without_listing_cards() =
        runTest {
            api.acceptClientRescheduleHandler = { _, _ -> ok }

            assertEquals(AcceptOutcome.Accepted, responder.accept("b1"))
            assertEquals(listOf<String?>(null), sentCards())
            assertEquals(0, api.getPaymentMethodsCalls)
        }

    @Test
    fun accept_recovers_with_the_default_card() =
        runTest {
            api.acceptClientRescheduleHandler = { _, body ->
                if (body.stripePaymentMethodId == null) throw paymentMethodRequired() else ok
            }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_a"), card("pm_b", isDefault = true))) }

            assertEquals(AcceptOutcome.Accepted, responder.accept("b1"))
            assertEquals(listOf<String?>(null, "pm_b"), sentCards())
        }

    @Test
    fun accept_recovers_with_the_first_card_when_none_is_default() =
        runTest {
            api.acceptClientRescheduleHandler = { _, body ->
                if (body.stripePaymentMethodId == null) throw paymentMethodRequired() else ok
            }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_a"), card("pm_c"))) }

            assertEquals(AcceptOutcome.Accepted, responder.accept("b1"))
            assertEquals(listOf<String?>(null, "pm_a"), sentCards())
        }

    @Test
    fun accept_with_no_saved_card_needs_a_payment_method() =
        runTest {
            api.acceptClientRescheduleHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { PaymentMethodsData(emptyList()) }

            assertEquals(AcceptOutcome.NeedsPaymentMethod, responder.accept("b1"))
            assertEquals(1, api.acceptClientRescheduleRequests.size)
        }

    @Test
    fun accept_with_a_failed_card_load_does_not_retry() =
        runTest {
            api.acceptClientRescheduleHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { throw IOException("offline") }

            assertEquals(AcceptOutcome.CardsUnavailable, responder.accept("b1"))
            assertEquals(1, api.acceptClientRescheduleRequests.size)
        }

    @Test
    fun accept_retries_exactly_once_then_reports_an_unusable_card() =
        runTest {
            api.acceptClientRescheduleHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_a"), card("pm_b", isDefault = true))) }

            assertEquals(AcceptOutcome.CardUnusable, responder.accept("b1"))
            assertEquals(listOf<String?>(null, "pm_b"), sentCards())
        }

    @Test
    fun accept_retry_failing_with_another_error_maps_through_the_failure_mapper() =
        runTest {
            api.acceptClientRescheduleHandler = { _, body ->
                if (body.stripePaymentMethodId == null) {
                    throw paymentMethodRequired()
                } else {
                    throw api.httpError(409, """{"code":"INVALID_STATUS"}""")
                }
            }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(card("pm_b", isDefault = true))) }

            assertEquals(
                AcceptOutcome.Failed(show(R.string.reschedule_error_not_pending, reload = true)),
                responder.accept("b1"),
            )
        }

    @Test
    fun accept_first_call_failures_do_not_list_cards() =
        runTest {
            api.acceptClientRescheduleHandler =
                { _, _ -> throw api.httpError(409, """{"code":"EXPERT_UNAVAILABLE"}""") }
            assertEquals(
                AcceptOutcome.Failed(show(R.string.reschedule_error_expert_unavailable, reload = true)),
                responder.accept("b1"),
            )
            assertEquals(0, api.getPaymentMethodsCalls)

            api.acceptClientRescheduleHandler = { _, _ -> throw IOException("offline") }
            assertEquals(AcceptOutcome.Failed(show(R.string.login_error_network)), responder.accept("b1"))
            assertEquals(0, api.getPaymentMethodsCalls)
        }

    // ---- decline ----

    @Test
    fun normalize_decline_reason_trims_blanks_and_caps() {
        assertEquals("Busy", normalizeDeclineReason("  Busy  "))
        assertNull(normalizeDeclineReason("   "))
        assertNull(normalizeDeclineReason(null))
        assertEquals(DECLINE_REASON_MAX_CHARS, normalizeDeclineReason("x".repeat(600))!!.length)
    }

    @Test
    fun decline_sends_the_trimmed_reason() =
        runTest {
            api.declineClientRescheduleHandler = { _, _ -> ok }

            assertEquals(DeclineOutcome.Declined, responder.decline("b1", "  Busy  "))
            assertEquals(DeclineRescheduleRequest("Busy"), api.declineClientRescheduleRequests.single().second)
        }

    @Test
    fun decline_with_a_blank_reason_sends_none() =
        runTest {
            api.declineClientRescheduleHandler = { _, _ -> ok }

            assertEquals(DeclineOutcome.Declined, responder.decline("b1", ""))
            assertEquals(DeclineRescheduleRequest(null), api.declineClientRescheduleRequests.single().second)
        }

    @Test
    fun decline_forbidden_reloads_with_the_forbidden_copy() =
        runTest {
            api.declineClientRescheduleHandler = { _, _ -> throw api.httpError(403, """{"code":"FORBIDDEN"}""") }

            assertEquals(
                DeclineOutcome.Failed(show(R.string.reschedule_error_forbidden, reload = true)),
                responder.decline("b1", "Busy"),
            )
        }
}
