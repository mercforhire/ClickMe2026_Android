@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
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
        val before = api.confirmBookingCalls
        api.confirmBookingHandler = { if (api.confirmBookingCalls == before + 1) throw first else after() }
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

    // ---- Task 2: cancel, failure and hard failure (D-15) ----

    @Test
    fun aCancelledChallengeShowsTheNotChargedAlertAndKeepsTheSlotAndAttemptId() =
        runTest(main.testDispatcher) {
            scriptConfirm()
            val vm = paidVm()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Canceled)
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_sca_canceled), s.dialogError)
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
            assertNull(s.challengeAccount)
            assertNull(s.stripeRequest)
            assertEquals(slotB, s.selectedSlotStart)
            assertEquals(1, api.confirmBookingCalls)

            vm.dismissError()
            vm.book()
            runCurrent()
            assertEquals(2, api.paymentIntentRequests.size)
            assertEquals(
                api.paymentIntentRequests[0].bookingAttemptId,
                api.paymentIntentRequests[1].bookingAttemptId,
            )
        }

    @Test
    fun aFailedChallengeShowsStripesMessageOrTheFallbackAndKeepsTheForm() =
        runTest(main.testDispatcher) {
            scriptConfirm()
            val vm = paidVm()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Failed("Your card was declined by the bank."))
            var s = vm.uiState.value
            assertEquals(FieldError.Text("Your card was declined by the bank."), s.dialogError)
            assertEquals(PaidStep.Idle, s.paidStep)
            assertFalse(s.isSubmitting)
            assertNull(s.challengeAccount)
            assertEquals(slotB, s.selectedSlotStart)

            // Failed(null) uses the fallback copy.
            api.confirmBookingHandler = { throw requiresAction() }
            vm.dismissError()
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Failed(null))
            s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_sca_failed), s.dialogError)
            assertEquals(slotB, s.selectedSlotStart)
            assertEquals(
                api.paymentIntentRequests.first().bookingAttemptId,
                api.paymentIntentRequests.last().bookingAttemptId,
            )
        }

    @Test
    fun aBlankOrMissingConnectedAccountNeverStartsAChallenge() =
        runTest(main.testDispatcher) {
            for (detail in listOf(challengeDetail(""), challengeDetail(null), null)) {
                api.confirmBookingHandler = { throw requiresAction(detail) }
                val vm = paidVm()
                bookToChallenge(vm)
                val s = vm.uiState.value
                assertNull(s.stripeRequest)
                assertNull(s.challengeAccount)
                if (detail != null) {
                    assertEquals(FieldError.Res(R.string.make_a_booking_error_sca_unavailable), s.dialogError)
                }
                assertEquals(PaidStep.Idle, s.paidStep)
                assertFalse(s.isSubmitting)
            }
        }

    // ---- WR-04: process death during the challenge ----

    @Test
    fun anOpenChallengeLeavesOnlyABooleanMarkerThatEveryOutcomeClears() =
        runTest(main.testDispatcher) {
            val outcomes = listOf(ChallengeOutcome.Completed, ChallengeOutcome.Canceled, ChallengeOutcome.Failed("x"))
            for (outcome in outcomes) {
                scriptConfirm()
                val saved = SavedStateHandle()
                val vm = paidVm(saved)
                bookToChallenge(vm)
                assertEquals(true, saved.get<Boolean>("booking_challenge_pending"))
                challenge(vm, outcome)
                assertFalse(outcome.toString(), saved.contains("booking_challenge_pending"))
            }
        }

    @Test
    fun aSecond402ClearsTheMarker() =
        runTest(main.testDispatcher) {
            api.confirmBookingHandler = { throw requiresAction() }
            val saved = SavedStateHandle()
            val vm = paidVm(saved)
            bookToChallenge(vm)
            challenge(vm, ChallengeOutcome.Completed)
            assertFalse(saved.contains("booking_challenge_pending"))
        }

    @Test
    fun aRestoreAfterDeathDuringTheChallengeExplainsThePossibleHoldAndIgnoresTheLateResult() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle(mapOf("booking_challenge_pending" to true))
            val vm = newViewModel(api, saved, clock, store, reader)
            runCurrent()
            var s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_challenge_interrupted), s.dialogError)
            assertEquals(R.string.make_a_booking_error_title, s.dialogTitle)
            assertFalse(s.isSubmitting)
            assertTrue(s.availability is AvailabilityState.Loaded)
            assertFalse(saved.contains("booking_challenge_pending"))

            // The PaymentLauncher result Stripe delivers after the restore has no context to act on.
            vm.onChallengeOutcome(ChallengeOutcome.Completed)
            runCurrent()
            assertEquals(0, api.confirmBookingCalls)
            s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.make_a_booking_error_challenge_interrupted), s.dialogError)

            // The notice is shown once: the form is usable after it is dismissed.
            vm.dismissError()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun aRestoreWithoutAnOpenChallengeShowsNoNotice() =
        runTest(main.testDispatcher) {
            val vm = newViewModel(api, SavedStateHandle(), clock, store, reader)
            runCurrent()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun noSecretReachesTheSavedStateHandle() =
        runTest(main.testDispatcher) {
            val outcomes = listOf(ChallengeOutcome.Completed, ChallengeOutcome.Canceled, ChallengeOutcome.Failed("x"))
            for (outcome in outcomes) {
                scriptConfirm()
                val saved = SavedStateHandle()
                val vm = paidVm(saved)
                bookToChallenge(vm)
                assertNoSecrets(saved)
                challenge(vm, outcome)
                assertNoSecrets(saved)
            }
        }

    private fun assertNoSecrets(saved: SavedStateHandle) {
        val secrets = setOf("acct_1", "pi_1_secret_x", "pi_1", "pm_new", "seti_1_secret_x")
        saved.keys().forEach { key -> assertFalse("$key holds a secret", saved.get<Any>(key) in secrets) }
    }
}
