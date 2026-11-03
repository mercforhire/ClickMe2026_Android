package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUG-06: when the host activity is recreated while the payment sheet is open, Stripe's `FlowController.Builder.build()`
 * registers its result launcher and, because the activity is already started, delivers the pending option result
 * inside `build()`, before the new controller exists. The confirm that result asks for must still run, once the
 * controller is bound, or Book Now spins forever.
 */
class PaidFlowStripeHostRegressionTest {
    private var confirms = 0
    private val outcomes = mutableListOf<SheetOutcome>()

    private fun deliverOptionResult(
        gate: ConfirmGate,
        didCancel: Boolean,
        hasPaymentOption: Boolean,
    ) = routePaymentOptionResult(
        paidStep = PaidStep.CollectingPaymentMethod,
        didCancel = didCancel,
        hasPaymentOption = hasPaymentOption,
        confirm = { gate.request() },
        reportSheetOutcome = { outcomes += it },
    )

    @Test
    fun aSelectionDeliveredDuringBuildConfirmsOnceTheControllerIsBound() {
        val gate = ConfirmGate()
        deliverOptionResult(gate, didCancel = false, hasPaymentOption = true)
        assertEquals("nothing to confirm on yet", 0, confirms)

        gate.bind { confirms++ }
        gate.runPending()

        assertEquals(1, confirms)
        assertEquals(emptyList<SheetOutcome>(), outcomes)
    }

    @Test
    fun aPendingConfirmRunsExactlyOnce() {
        val gate = ConfirmGate()
        deliverOptionResult(gate, didCancel = false, hasPaymentOption = true)
        gate.bind { confirms++ }
        gate.runPending()
        gate.runPending()
        assertEquals(1, confirms)
    }

    @Test
    fun aSelectionAfterTheControllerIsBoundConfirmsAtOnce() {
        val gate = ConfirmGate()
        gate.bind { confirms++ }
        deliverOptionResult(gate, didCancel = false, hasPaymentOption = true)
        assertEquals(1, confirms)
        gate.runPending()
        assertEquals("no second confirm", 1, confirms)
    }

    /** D-18: a dismissal never authorizes a card, whether or not the controller is bound yet. */
    @Test
    fun aDismissalDuringBuildNeverConfirms() {
        val gate = ConfirmGate()
        deliverOptionResult(gate, didCancel = true, hasPaymentOption = true)
        gate.bind { confirms++ }
        gate.runPending()
        assertEquals(0, confirms)
        assertEquals(listOf<SheetOutcome>(SheetOutcome.Canceled), outcomes)
    }

    @Test
    fun theResetAfterCompletionNeverConfirmsEvenWhenDeliveredEarly() {
        val gate = ConfirmGate()
        deliverOptionResult(gate, didCancel = false, hasPaymentOption = false)
        gate.bind { confirms++ }
        gate.runPending()
        assertEquals(0, confirms)
        assertEquals(emptyList<SheetOutcome>(), outcomes)
    }
}
