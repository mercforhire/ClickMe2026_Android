package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.stripe.SETUP_INTENT_REUSE_WINDOW_MS
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private var nowMs = 0L

    private val visa = PaymentMethodItem("pm_1", "visa", "4242", 9, 2028, isDefault = true)
    private val master = PaymentMethodItem("pm_2", "mastercard", "8210", 3, 2027, isDefault = false)
    private val setup = PaymentMethodSetupData("seti_1_secret_x", "cus_1", "ek_1")
    private val ok = SuccessMessageResponse(status = "success")

    init {
        api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(visa, master)) }
        api.setupIntentHandler = { setup }
        api.deletePaymentMethodHandler = { ok }
        api.setDefaultPaymentMethodHandler = { ok }
    }

    private fun vm() = PaymentMethodsViewModel(PaymentMethodsRepository(api), MonotonicClock { nowMs })

    private fun PaymentMethodsViewModel.cards() = (uiState.value.list as PaymentMethodsListState.Loaded).cards

    private fun serverError(message: String) = api.httpError(500, """{"status":"error","message":"$message"}""")

    private fun addDialog(message: FieldError) = PaymentMethodsDialog(R.string.payment_methods_error_add_title, message)

    @Test
    fun loadsServerCardsInServerOrder() =
        runTest(main.testDispatcher) {
            val vm = vm()
            assertEquals(PaymentMethodsListState.Loading, vm.uiState.value.list)
            runCurrent()

            assertEquals(listOf(visa, master), vm.cards())
            assertEquals(1, api.getPaymentMethodsCalls)
        }

    @Test
    fun emptyServerListIsLoaded() =
        runTest(main.testDispatcher) {
            api.getPaymentMethodsHandler = { PaymentMethodsData(emptyList()) }
            val vm = vm()
            runCurrent()

            assertEquals(PaymentMethodsListState.Loaded(emptyList()), vm.uiState.value.list)
        }

    @Test
    fun loadFailureShowsServerMessageOrNetworkCopyAndRetryReloads() =
        runTest(main.testDispatcher) {
            api.getPaymentMethodsHandler = { throw serverError("Server down") }
            val vm = vm()
            runCurrent()
            assertEquals(PaymentMethodsListState.Failed(FieldError.Text("Server down")), vm.uiState.value.list)

            api.getPaymentMethodsHandler = { throw IOException("offline") }
            vm.retry()
            assertEquals(PaymentMethodsListState.Loading, vm.uiState.value.list)
            runCurrent()
            assertEquals(
                PaymentMethodsListState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.list,
            )

            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(visa)) }
            vm.retry()
            runCurrent()
            assertEquals(listOf(visa), vm.cards())
        }

    @Test
    fun setDefaultIsBusyIgnoresSecondRequestAndReloadsServerTruth() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            api.setDefaultPaymentMethodHandler = {
                gate.await()
                ok
            }
            val reloaded = listOf(visa.copy(isDefault = false), master.copy(isDefault = true))
            api.getPaymentMethodsHandler = { PaymentMethodsData(reloaded) }

            vm.setDefault("pm_2")
            runCurrent()
            assertEquals(CardOperation("pm_2", CardOperationKind.SetDefault), vm.uiState.value.busy)

            vm.setDefault("pm_1")
            vm.remove("pm_1")
            runCurrent()
            assertEquals(1, api.setDefaultPaymentMethodCalls)
            assertEquals(0, api.deletePaymentMethodCalls)

            gate.complete(Unit)
            runCurrent()

            assertEquals(2, api.getPaymentMethodsCalls)
            assertNull(vm.uiState.value.busy)
            assertEquals(reloaded, vm.cards())
        }

    @Test
    fun setDefaultOnTheCurrentDefaultMakesNoCall() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()

            vm.setDefault("pm_1")
            runCurrent()

            assertEquals(0, api.setDefaultPaymentMethodCalls)
            assertNull(vm.uiState.value.busy)
        }

    @Test
    fun onlyIdsFromTheLoadedListAreSent() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()

            vm.setDefault("pm_foreign")
            vm.remove("pm_foreign")
            runCurrent()

            assertEquals(0, api.setDefaultPaymentMethodCalls)
            assertEquals(0, api.deletePaymentMethodCalls)
        }

    @Test
    fun removeNeverPromotesAnotherCardLocally() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val remaining = master.copy(isDefault = false)
            api.getPaymentMethodsHandler = { PaymentMethodsData(listOf(remaining)) }

            vm.remove("pm_1")
            runCurrent()

            assertEquals(listOf("pm_1"), api.deletedPaymentMethodIds)
            assertEquals(listOf(remaining), vm.cards())
            assertEquals(false, vm.cards().single().isDefault)
        }

    @Test
    fun setDefaultFailureShowsUpdateDialogWithoutReload() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.setDefaultPaymentMethodHandler = { throw serverError("Nope") }

            vm.setDefault("pm_2")
            runCurrent()

            assertEquals(
                PaymentMethodsDialog(R.string.payment_methods_error_update_title, FieldError.Text("Nope")),
                vm.uiState.value.dialog,
            )
            assertNull(vm.uiState.value.busy)
            assertEquals(1, api.getPaymentMethodsCalls)
        }

    @Test
    fun removeFailureShowsRemoveDialogAndSuspendedShowsNothing() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.deletePaymentMethodHandler = { throw serverError("Cannot remove") }

            vm.remove("pm_2")
            runCurrent()
            assertEquals(
                PaymentMethodsDialog(R.string.payment_methods_error_remove_title, FieldError.Text("Cannot remove")),
                vm.uiState.value.dialog,
            )

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)

            api.deletePaymentMethodHandler = {
                throw api.httpError(403, """{"status":"error","code":"ACCOUNT_SUSPENDED"}""")
            }
            vm.remove("pm_2")
            runCurrent()
            assertNull(vm.uiState.value.dialog)
            assertNull(vm.uiState.value.busy)
        }

    @Test
    fun failedSilentReloadKeepsPreviousListWithoutDialog() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.getPaymentMethodsHandler = { throw IOException("offline") }

            vm.remove("pm_2")
            runCurrent()

            assertEquals(listOf(visa, master), vm.cards())
            assertNull(vm.uiState.value.dialog)
            assertNull(vm.uiState.value.busy)
        }

    @Test
    fun addCardPublishesOneShotRequestAndIgnoresSecondTap() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            api.setupIntentHandler = {
                gate.await()
                setup
            }

            vm.addCard()
            runCurrent()
            assertEquals(true, vm.uiState.value.isAddingCard)
            vm.addCard()
            runCurrent()
            assertEquals(1, api.setupIntentCalls)

            gate.complete(Unit)
            runCurrent()
            assertEquals(AddCardRequest("seti_1_secret_x", "cus_1", "ek_1"), vm.uiState.value.addCardRequest)

            vm.onAddCardLaunched()
            assertNull(vm.uiState.value.addCardRequest)
            assertEquals(true, vm.uiState.value.isAddingCard)
        }

    @Test
    fun addCardOutcomesCompletedReloadsCanceledDoesNotFailedAlerts() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()

            vm.addCard()
            runCurrent()
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Canceled)
            runCurrent()
            assertEquals(false, vm.uiState.value.isAddingCard)
            assertEquals(1, api.getPaymentMethodsCalls)

            vm.addCard()
            runCurrent()
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Completed)
            runCurrent()
            assertEquals(false, vm.uiState.value.isAddingCard)
            assertEquals(2, api.getPaymentMethodsCalls)

            vm.addCard()
            runCurrent()
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Failed("Your card was declined."))
            assertEquals(addDialog(FieldError.Text("Your card was declined.")), vm.uiState.value.dialog)

            vm.addCard()
            runCurrent()
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Failed(null))
            assertEquals(addDialog(FieldError.Res(R.string.login_error_unknown)), vm.uiState.value.dialog)
        }

    @Test
    fun cancelledSetupIsReusedWithinTenMinutesThenReminted() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()

            vm.addCard()
            runCurrent()
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Canceled)

            nowMs += SETUP_INTENT_REUSE_WINDOW_MS - 1
            vm.addCard()
            runCurrent()
            assertEquals(1, api.setupIntentCalls)
            assertEquals(AddCardRequest("seti_1_secret_x", "cus_1", "ek_1"), vm.uiState.value.addCardRequest)
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Canceled)

            nowMs += 11 * 60_000L
            vm.addCard()
            runCurrent()
            assertEquals(2, api.setupIntentCalls)
        }

    @Test
    fun setupIsNeverReusedAfterCompletedOrFailed() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()

            vm.addCard()
            runCurrent()
            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Completed)
            runCurrent()
            vm.addCard()
            runCurrent()
            assertEquals(2, api.setupIntentCalls)

            vm.onAddCardLaunched()
            vm.onAddCardResult(SheetOutcome.Failed("x"))
            vm.addCard()
            runCurrent()
            assertEquals(3, api.setupIntentCalls)
        }

    @Test
    fun setupIntentRateLimitShowsServerMessageAndDoesNotRetry() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val text = "Too many setup-intent requests. Please try again in 15 minutes."
            api.setupIntentHandler = { throw api.httpError(429, """{"status":"error","message":"$text"}""") }

            vm.addCard()
            runCurrent()

            assertEquals(addDialog(FieldError.Text(text)), vm.uiState.value.dialog)
            assertEquals(false, vm.uiState.value.isAddingCard)
            assertEquals(1, api.setupIntentCalls)
        }
}
