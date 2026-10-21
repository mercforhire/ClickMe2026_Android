@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.verifyemail

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class SignupVerifyEmailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val ok = SuccessMessageResponse(status = "success", message = "ok")
    private val hold = 600L

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.repository.seedFromSignup("ada@b.co", false)
            it.api.resendEmailHandler = { ok }
            it.api.verifyEmailHandler = { ok }
        }

    private fun TestScope.clock() = MonotonicClock { testScheduler.currentTime }

    private fun TestScope.viewModel(
        h: SignupTestHarness,
        saved: SavedStateHandle = SavedStateHandle(),
        repository: SignupSetupRepository = h.repository,
    ) = SignupVerifyEmailViewModel(repository, clock(), saved)

    /** Sends the first code and waits out the 600 ms "Email Sent!" hold. */
    private fun TestScope.sendFirst(vm: SignupVerifyEmailViewModel) {
        vm.send()
        runCurrent()
        advanceTimeBy(hold + 1)
        runCurrent()
    }

    @Test
    fun opening_sendsNothing_andTakesTheEmailFromTheDraft() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(0, h.api.resendEmailCalls)
            assertEquals("ada@b.co", vm.uiState.value.email)
            assertFalse(vm.uiState.value.codeSent)
        }

    @Test
    fun send_callsOnce_holdsEmailSent_thenShowsCodeAndCountsDown() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)

            vm.send()
            runCurrent()
            assertEquals(1, h.api.resendEmailCalls)
            assertEquals(SaveState.Saved, vm.uiState.value.sendState)
            assertFalse(vm.uiState.value.codeSent)
            assertEquals(30, vm.uiState.value.resendSecondsRemaining)

            advanceTimeBy(hold + 1)
            runCurrent()
            assertTrue(vm.uiState.value.codeSent)

            advanceTimeBy(1_000 - hold)
            runCurrent()
            assertEquals(29, vm.uiState.value.resendSecondsRemaining)

            advanceTimeBy(29_000)
            runCurrent()
            assertEquals(0, vm.uiState.value.resendSecondsRemaining)
        }

    @Test
    fun resend_isIgnoredWhileCountingDown_andAtZeroCallsClearsCodeAndRestarts() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            sendFirst(vm)
            vm.onCodeChange("123")

            vm.resend()
            runCurrent()
            assertEquals(1, h.api.resendEmailCalls)

            advanceTimeBy(30_000)
            runCurrent()
            assertEquals(0, vm.uiState.value.resendSecondsRemaining)
            vm.resend()
            runCurrent()

            assertEquals(2, h.api.resendEmailCalls)
            assertEquals("", vm.uiState.value.code)
            assertEquals(30, vm.uiState.value.resendSecondsRemaining)
            assertFalse(vm.uiState.value.isResending)
        }

    @Test
    fun rateLimitedOnSend_countsAsSent_withoutADialog() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.resendEmailHandler = { throw h.api.httpError(429, """{"code":"RATE_LIMITED","message":"Wait"}""") }
            val vm = viewModel(h)

            sendFirst(vm)

            assertTrue(vm.uiState.value.codeSent)
            assertEquals(30, vm.uiState.value.resendSecondsRemaining)
            assertNull(vm.uiState.value.dialogError)
            assertEquals(SaveState.Saved, vm.uiState.value.sendState)
        }

    @Test
    fun otherSendErrors_showTitledDialog_andLeaveCodeNotSent() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)

            h.api.resendEmailHandler = { throw h.api.httpError(500, """{"message":"Server down"}""") }
            vm.send()
            runCurrent()
            assertEquals(FieldError.Text("Server down"), vm.uiState.value.dialogError)
            assertEquals(R.string.signup_verify_error_send_title, vm.uiState.value.dialogTitleRes)
            assertFalse(vm.uiState.value.codeSent)
            assertEquals(SaveState.Idle, vm.uiState.value.sendState)

            vm.dismissDialog()
            h.api.resendEmailHandler = { throw IOException("offline") }
            vm.send()
            runCurrent()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialogError)
            assertEquals(R.string.signup_verify_error_send_title, vm.uiState.value.dialogTitleRes)
            assertFalse(vm.uiState.value.codeSent)
            assertEquals(2, h.api.resendEmailCalls)
        }

    @Test
    fun onCodeChange_sanitizes_mirrors_andClearsTheError() =
        runTest(main.testDispatcher) {
            val saved = SavedStateHandle()
            val h = harness(this)
            val vm = viewModel(h, saved)
            vm.verify()
            runCurrent()
            assertNotNull(vm.uiState.value.codeError)

            vm.onCodeChange("12a3 45678")
            runCurrent()

            assertEquals("123456", vm.uiState.value.code)
            assertEquals("123456", saved.get<String>("verify_code"))
            assertNull(vm.uiState.value.codeError)
        }

    @Test
    fun verify_validatesTheCodeLocally_withoutACall() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)

            vm.verify()
            runCurrent()
            assertEquals(FieldError.Res(R.string.error_code_required), vm.uiState.value.codeError)

            vm.onCodeChange("123")
            vm.verify()
            runCurrent()
            assertEquals(FieldError.Res(R.string.error_code_invalid), vm.uiState.value.codeError)
            assertEquals(0, h.api.verifyEmailCalls)
        }

    @Test
    fun verify_success_marksDraftVerified_thenSignalsDoneAfterTheHold() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            vm.onCodeChange("123456")

            vm.verify()
            runCurrent()
            assertEquals(1, h.api.verifyEmailCalls)
            assertEquals(SaveState.Saved, vm.uiState.value.verifyState)
            assertTrue(h.repository.draft.value.emailVerified)
            assertFalse(vm.done.value)
            assertFalse(vm.uiState.value.verified)

            advanceTimeBy(hold + 1)
            runCurrent()
            assertTrue(vm.done.value)
            vm.onDoneConsumed()
            assertFalse(vm.done.value)
        }

    @Test
    fun invalidCode422_showsExpiredMessageInline_withoutDialogOrRetry() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.verifyEmailHandler = { throw h.api.httpError(422, """{"code":"INVALID_CODE","message":"bad"}""") }
            val vm = viewModel(h)
            vm.onCodeChange("123456")

            vm.verify()
            runCurrent()
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(FieldError.Res(R.string.error_code_expired), vm.uiState.value.codeError)
            assertNull(vm.uiState.value.dialogError)
            assertEquals(SaveState.Idle, vm.uiState.value.verifyState)
            assertEquals(1, h.api.verifyEmailCalls)
        }

    @Test
    fun tooManyRequests429_showsTheServerMessageInline_orTheFallback() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val message = "Too many attempts. Please try again in 15 minutes."
            h.api.verifyEmailHandler =
                { throw h.api.httpError(429, """{"code":"TOO_MANY_REQUESTS","message":"$message"}""") }
            val vm = viewModel(h)
            vm.onCodeChange("123456")

            vm.verify()
            runCurrent()
            assertEquals(FieldError.Text(message), vm.uiState.value.codeError)
            assertNull(vm.uiState.value.dialogError)

            h.api.verifyEmailHandler = { throw h.api.httpError(429, """{"code":"TOO_MANY_REQUESTS"}""") }
            vm.verify()
            runCurrent()
            assertEquals(FieldError.Res(R.string.signup_verify_error_too_many), vm.uiState.value.codeError)
            assertNull(vm.uiState.value.dialogError)
            assertEquals(2, h.api.verifyEmailCalls)
        }

    @Test
    fun alreadyVerifiedDraft_showsVerified_andSendIsANoOp() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.repository.seedFromSignup("ada@b.co", true)
            val vm = viewModel(h)

            vm.send()
            vm.verify()
            runCurrent()

            assertTrue(vm.uiState.value.verified)
            assertEquals(0, h.api.resendEmailCalls)
            assertEquals(0, h.api.verifyEmailCalls)
        }

    @Test
    fun reEntry_within30s_keepsCodeFieldAndCountdown_andResendStaysDisabled() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val first = viewModel(h)
            sendFirst(first)
            advanceTimeBy(10_000 - testScheduler.currentTime)
            runCurrent()

            val second = viewModel(h)
            runCurrent()

            assertTrue(second.uiState.value.codeSent)
            assertEquals(20, second.uiState.value.resendSecondsRemaining)
            second.resend()
            runCurrent()
            assertEquals(1, h.api.resendEmailCalls)
        }

    @Test
    fun processDeath_restoresFromSavedState_andSeedsAFreshRepository() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val saved = SavedStateHandle()
            val first = viewModel(h, saved)
            sendFirst(first)
            first.onCodeChange("987654")
            val endsAt = h.repository.verificationSend.value.cooldownEndsAt
            advanceTimeBy(10_000)
            runCurrent()

            val freshRepository =
                SignupSetupRepository(h.api, h.userManager, h.photo, backgroundScope, main.testDispatcher)
            val second = viewModel(h, saved, freshRepository)
            runCurrent()

            assertTrue(second.uiState.value.codeSent)
            assertEquals("987654", second.uiState.value.code)
            val expected = ((endsAt - testScheduler.currentTime + 999) / 1000).toInt()
            assertEquals(expected, second.uiState.value.resendSecondsRemaining)
            assertEquals(endsAt, freshRepository.verificationSend.value.cooldownEndsAt)
            assertTrue(freshRepository.verificationSend.value.codeSent)
        }

    @Test
    fun afterLogout_aNewViewModelStartsUnsent() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.signIn()
            runCurrent()
            h.repository.seedFromSignup("ada@b.co", false)
            val first = viewModel(h)
            sendFirst(first)

            h.userManager.logout()
            runCurrent()
            val second = viewModel(h, SavedStateHandle())
            runCurrent()

            assertFalse(second.uiState.value.codeSent)
            assertEquals(0, second.uiState.value.resendSecondsRemaining)
        }
}
