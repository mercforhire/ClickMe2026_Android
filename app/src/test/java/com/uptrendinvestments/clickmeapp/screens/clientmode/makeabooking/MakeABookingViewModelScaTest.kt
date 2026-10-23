@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.stripe.ChallengeOutcome
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeCompletedBookingStore
import com.uptrendinvestments.clickmeapp.testing.FakeStripePaymentMethodReader
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

/** PAY-05: the 3DS hand-off, the single confirm retry, the one-round guard and the D-15 outcomes. */
class MakeABookingViewModelScaTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse(NOW))
    private val reader = FakeStripePaymentMethodReader().apply { result = "pm_new" }
    private val store = FakeCompletedBookingStore()
    private lateinit var previousZone: TimeZone

    private val slotB = Instant.parse("2026-10-23T01:00:00Z")

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        paidFixture(api)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun requiresAction(detail: Map<String, Any?>? = challengeDetail("acct_1")) =
        ApiException.Http(
            status = 402,
            code = ErrorCode.PaymentRequiresAction,
            rawCode = ErrorCode.PaymentRequiresAction.wire,
            serverMessage = "Authentication required",
            fieldErrors = emptyList(),
            detail = detail,
        )

    private fun challengeDetail(account: String?) =
        buildMap<String, Any?> {
            put("client_secret", "pi_1_secret_x")
            if (account != null) put("stripe_account", account)
        }

    /** The first confirm throws the 402; every later one runs [after] (default: success "b9"). */
    private fun scriptConfirm(
        first: ApiException = requiresAction(),
        after: suspend () -> ConfirmBookingData = { ConfirmBookingData("b9") },
    ) {
        api.confirmBookingHandler = { if (api.confirmBookingCalls == 1) throw first else after() }
    }

    private fun TestScope.paidVm(saved: SavedStateHandle = SavedStateHandle()): MakeABookingViewModel {
        val vm = newViewModel(api, saved, clock, store, reader)
        runCurrent()
        vm.selectTopic("t2")
        vm.selectSlot(slotB)
        return vm
    }

    /** Books, completes the sheet and runs the confirm, which answers with the scripted 402. */
    private fun TestScope.bookToChallenge(vm: MakeABookingViewModel) {
        vm.book()
        runCurrent()
        vm.onStripeRequestLaunched()
        vm.onSheetOutcome(SheetOutcome.Completed)
        runCurrent()
    }

    private fun TestScope.challenge(
        vm: MakeABookingViewModel,
        outcome: ChallengeOutcome,
    ) {
        vm.onStripeRequestLaunched()
        vm.onChallengeOutcome(outcome)
        runCurrent()
    }

    @Test
    fun a402PublishesOneChallengeAndKeepsTheAccountInMemory() =
        runTest(main.testDispatcher) {
            scriptConfirm()
            val vm = paidVm()
            bookToChallenge(vm)
            val s = vm.uiState.value
            assertEquals(StripeRequest.RunChallenge("pi_1_secret_x", "acct_1"), s.stripeRequest)
            assertEquals("acct_1", s.challengeAccount)
            assertEquals(PaidStep.Confirming, s.paidStep)
            assertTrue(s.isSubmitting)
            assertNull(s.dialogError)
        }

    @Test
    fun launchingTheChallengeKeepsTheAccountAndIsIdempotent() =
        runTest(main.testDispatcher) {
            scriptConfirm()
            val vm = paidVm()
            bookToChallenge(vm)
            vm.onStripeRequestLaunched()
            vm.onStripeRequestLaunched()
            assertNull(vm.uiState.value.stripeRequest)
            assertEquals("acct_1", vm.uiState.value.challengeAccount)
            assertEquals(PaidStep.Confirming, vm.uiState.value.paidStep)
        }

    @Test
    fun aCompletedChallengeRetriesConfirmOnceWithTheSamePmAndPi() =
        runTest(main.testDispatcher) {
            scriptConfirm()
            val vm = paidVm()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Completed)
            assertEquals(2, api.confirmBookingCalls)
            assertEquals(api.confirmBookingRequests[0], api.confirmBookingRequests[1])
            assertEquals("pm_new", api.confirmBookingRequests[1].stripePaymentMethodId)
            assertEquals("pi_1", api.confirmBookingRequests[1].paymentIntentId)
            val s = vm.uiState.value
            assertEquals("b9", s.completedBookingId)
            assertNull(s.challengeAccount)
            assertNull(s.dialogError)
            assertEquals(PaidStep.Idle, s.paidStep)
        }

    @Test
    fun aSecond402AfterTheChallengeShowsTheServerMessageInsteadOfLooping() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw requiresAction() }
            val vm = paidVm()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Completed)
            assertEquals(2, api.confirmBookingCalls)
            val s = vm.uiState.value
            assertNull(s.stripeRequest)
            assertNull(s.challengeAccount)
            assertEquals(FieldError.Text("Authentication required"), s.dialogError)
            assertEquals("pi_1", s.dialogPaymentReference)
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
        }

    @Test
    fun aNewBookStartsANewRoundAfterTheGuardTripped() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw requiresAction() }
            val vm = paidVm()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Completed)
            vm.dismissError()
            vm.book()
            runCurrent()
            vm.onStripeRequestLaunched()
            vm.onSheetOutcome(SheetOutcome.Completed)
            runCurrent()
            assertEquals(StripeRequest.RunChallenge("pi_1_secret_x", "acct_1"), vm.uiState.value.stripeRequest)
        }

    @Test
    fun anOutcomeWithoutAnActiveChallengeIsIgnored() =
        runTest(main.testDispatcher) {
            val vm = paidVm()
            vm.onChallengeOutcome(ChallengeOutcome.Completed)
            vm.onChallengeOutcome(ChallengeOutcome.Canceled)
            vm.onChallengeOutcome(ChallengeOutcome.Failed("x"))
            runCurrent()
            assertEquals(0, api.confirmBookingCalls)
            assertNull(vm.uiState.value.dialogError)
            assertEquals(PaidStep.Idle, vm.uiState.value.paidStep)
        }

    @Test
    fun aSecondOutcomeForTheSameChallengeIsIgnored() =
        runTest(main.testDispatcher) {
            scriptConfirm()
            val vm = paidVm()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Completed)
            vm.onChallengeOutcome(ChallengeOutcome.Completed)
            runCurrent()
            assertEquals(2, api.confirmBookingCalls)
        }
}
