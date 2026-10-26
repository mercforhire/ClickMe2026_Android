package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
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

@OptIn(ExperimentalCoroutinesApi::class)
class RescheduleResponseControllerTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val responder = RescheduleResponder(ClientBookingsRepository(api), PaymentMethodsRepository(api))
    private val ok = RescheduleResponseData(bookingId = "b1", status = "confirmed")
    private val events = mutableListOf<String>()
    private lateinit var controller: RescheduleResponseController

    private fun TestScope.controller(): RescheduleResponseController =
        RescheduleResponseController(
            scope = backgroundScope,
            responder = responder,
            bookingId = "b1",
            reload = {
                // The reload must run while the controller is still busy and before any outcome is shown.
                events += "reload:${controller.state.value.submitting}:${controller.state.value.notice}"
            },
        ).also { controller = it }

    private fun paymentMethodRequired() = api.httpError(422, """{"code":"PAYMENT_METHOD_REQUIRED"}""")

    private fun error(
        id: Int,
        pop: Boolean = false,
    ) = RescheduleDialog.Error(FieldError.Res(id), popOnDismiss = pop)

    @Test
    fun accept_marks_submitting_ignores_a_second_tap_then_reloads_before_the_notice() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<RescheduleResponseData>()
            api.acceptClientRescheduleHandler = { _, _ -> gate.await() }
            api.declineClientRescheduleHandler = { _, _ -> ok }
            val c = controller()

            c.accept()
            runCurrent()
            assertEquals(RescheduleSubmitting.Accepting, c.state.value.submitting)
            c.accept()
            c.decline("Busy")
            runCurrent()
            assertEquals(1, api.acceptClientRescheduleRequests.size)
            assertEquals(0, api.declineClientRescheduleRequests.size)

            gate.complete(ok)
            runCurrent()

            assertEquals(listOf("reload:Accepting:null"), events)
            assertEquals(
                RescheduleUiState(RescheduleSubmitting.None, dialog = null, notice = RescheduleNotice.Accepted),
                c.state.value,
            )
            c.consumeNotice()
            assertNull(c.state.value.notice)
        }

    @Test
    fun accept_without_a_saved_card_shows_the_payment_method_dialog_and_does_not_reload() =
        runTest(main.testDispatcher) {
            api.acceptClientRescheduleHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { PaymentMethodsData(emptyList()) }
            val c = controller()

            c.accept()
            runCurrent()

            assertEquals(RescheduleDialog.PaymentMethodNeeded, c.state.value.dialog)
            assertEquals(RescheduleSubmitting.None, c.state.value.submitting)
            assertTrue(events.isEmpty())
            c.dismissDialog()
            assertNull(c.state.value.dialog)
        }

    @Test
    fun an_unusable_card_shows_the_card_unusable_dialog() =
        runTest(main.testDispatcher) {
            api.acceptClientRescheduleHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(PaymentMethodItem("pm_a", isDefault = true))) }
            val c = controller()

            c.accept()
            runCurrent()

            assertEquals(RescheduleDialog.CardUnusable, c.state.value.dialog)
        }

    @Test
    fun a_failed_card_load_shows_the_cards_unavailable_error() =
        runTest(main.testDispatcher) {
            api.acceptClientRescheduleHandler = { _, _ -> throw paymentMethodRequired() }
            api.getPaymentMethodsHandler = { throw IOException("offline") }
            val c = controller()

            c.accept()
            runCurrent()

            assertEquals(error(R.string.reschedule_error_cards_unavailable), c.state.value.dialog)
            assertTrue(events.isEmpty())
        }

    @Test
    fun a_failure_that_asks_for_a_reload_reloads_before_the_error_dialog() =
        runTest(main.testDispatcher) {
            api.acceptClientRescheduleHandler = { _, _ -> throw api.httpError(409, """{"code":"INVALID_STATUS"}""") }
            val c = controller()

            c.accept()
            runCurrent()

            assertEquals(listOf("reload:Accepting:null"), events)
            assertEquals(error(R.string.reschedule_error_not_pending), c.state.value.dialog)
            assertNull(c.state.value.notice)
        }

    @Test
    fun a_missing_booking_pops_on_dismiss_without_reloading() =
        runTest(main.testDispatcher) {
            api.acceptClientRescheduleHandler = { _, _ -> throw api.httpError(404, """{"code":"NOT_FOUND"}""") }
            val c = controller()

            c.accept()
            runCurrent()

            assertEquals(error(R.string.reschedule_error_not_found, pop = true), c.state.value.dialog)
            assertTrue(events.isEmpty())
        }

    @Test
    fun a_suspended_account_is_silent() =
        runTest(main.testDispatcher) {
            api.acceptClientRescheduleHandler = { _, _ -> throw api.httpError(403, """{"code":"ACCOUNT_SUSPENDED"}""") }
            val c = controller()

            c.accept()
            runCurrent()

            assertEquals(RescheduleUiState(), c.state.value)
            assertTrue(events.isEmpty())
        }

    private class FakeResponding(
        var acceptOutcome: AcceptOutcome = AcceptOutcome.Accepted,
        var declineOutcome: DeclineOutcome = DeclineOutcome.Declined,
    ) : RescheduleResponding {
        val accepted = mutableListOf<String>()
        val declined = mutableListOf<Pair<String, String?>>()

        override suspend fun accept(bookingId: String): AcceptOutcome {
            accepted += bookingId
            return acceptOutcome
        }

        override suspend fun decline(
            bookingId: String,
            reason: String?,
        ): DeclineOutcome {
            declined += bookingId to reason
            return declineOutcome
        }
    }

    @Test
    fun controllerWorksWithAnyResponder() =
        runTest(main.testDispatcher) {
            val fake = FakeResponding()
            var reloads = 0
            val c =
                RescheduleResponseController(
                    scope = backgroundScope,
                    responder = fake,
                    bookingId = "b9",
                    reload = { reloads++ },
                )

            c.accept()
            runCurrent()
            assertEquals(listOf("b9"), fake.accepted)
            assertEquals(1, reloads)
            assertEquals(RescheduleNotice.Accepted, c.state.value.notice)
            c.consumeNotice()

            fake.declineOutcome =
                DeclineOutcome.Failed(
                    RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_not_pending), reload = true),
                )
            c.decline("Busy")
            runCurrent()
            assertEquals(listOf("b9" to "Busy"), fake.declined)
            assertEquals(2, reloads)
            assertEquals(error(R.string.reschedule_error_not_pending), c.state.value.dialog)
            assertEquals(RescheduleSubmitting.None, c.state.value.submitting)
        }

    @Test
    fun decline_marks_submitting_then_reloads_and_sets_the_declined_notice() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<RescheduleResponseData>()
            api.declineClientRescheduleHandler = { _, _ -> gate.await() }
            val c = controller()

            c.decline("Busy")
            runCurrent()
            assertEquals(RescheduleSubmitting.Declining, c.state.value.submitting)

            gate.complete(ok)
            runCurrent()

            assertEquals(listOf("reload:Declining:null"), events)
            assertEquals(RescheduleNotice.Declined, c.state.value.notice)
            assertEquals(RescheduleSubmitting.None, c.state.value.submitting)
            assertEquals(
                "Busy",
                api.declineClientRescheduleRequests
                    .single()
                    .second.reason,
            )
        }
}
