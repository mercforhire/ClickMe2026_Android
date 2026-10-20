@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.login.forgetpassword

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ForgotPasswordViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = ForgotPasswordViewModel(api, handle)

    private fun succeed() {
        api.forgotPasswordHandler = { SuccessMessageResponse(status = "success", message = "ok") }
    }

    @Test
    fun startSeedsEmailOnlyOnce() {
        val vm = viewModel()
        vm.start("a@b.co")
        assertEquals("a@b.co", vm.uiState.value.email)
        vm.start("other@b.co")
        assertEquals("a@b.co", vm.uiState.value.email)
    }

    @Test
    fun startPrefersSavedEmail() {
        val vm = viewModel(SavedStateHandle(mapOf("forgot_email" to "kept@b.co")))
        vm.start("a@b.co")
        assertEquals("kept@b.co", vm.uiState.value.email)
    }

    @Test
    fun emptySendShowsRequiredErrorFocusesAndSkipsApi() =
        runTest(dispatcherRule.testDispatcher) {
            val vm = viewModel()
            vm.start("")
            vm.send()
            runCurrent()
            assertEquals(FieldError.Res(R.string.error_forgot_email_required), vm.uiState.value.emailError)
            assertTrue(vm.focusEmail.value)
            assertEquals(0, api.forgotPasswordCalls)
            vm.onFocusConsumed()
            assertFalse(vm.focusEmail.value)
            vm.onEmailChange("x@y")
            assertEquals(FieldError.Res(R.string.error_email_invalid), vm.uiState.value.emailError)
            vm.onEmailChange("x@y.co")
            assertNull(vm.uiState.value.emailError)
        }

    @Test
    fun typingBeforeFirstSendNeverShowsError() {
        val vm = viewModel()
        vm.start("")
        vm.onEmailChange("not-an-email")
        assertNull(vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.didAttemptSend)
    }

    @Test
    fun sendTrimsIdentityAndHoldsCodeSentFor600ms() =
        runTest(dispatcherRule.testDispatcher) {
            succeed()
            val vm = viewModel()
            vm.start("")
            vm.onEmailChange(" a@b.co ")
            vm.send()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            advanceTimeBy(599)
            assertEquals(1, api.forgotPasswordCalls)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertNull(vm.navigateToReset.value)
            advanceTimeBy(2)
            assertEquals("a@b.co", vm.navigateToReset.value)
        }

    @Test
    fun sendPostsTrimmedIdentity() =
        runTest(dispatcherRule.testDispatcher) {
            var seen: String? = null
            api.forgotPasswordHandler = {
                seen = it.identity
                SuccessMessageResponse(status = "success")
            }
            val vm = viewModel()
            vm.start("  a@b.co")
            vm.send()
            advanceUntilIdle()
            assertEquals("a@b.co", seen)
        }

    @Test
    fun onNavigatedClearsOneShotAndResetsToIdle() =
        runTest(dispatcherRule.testDispatcher) {
            succeed()
            val vm = viewModel()
            vm.start("a@b.co")
            vm.send()
            advanceUntilIdle()
            assertEquals("a@b.co", vm.navigateToReset.value)
            vm.onNavigated()
            assertNull(vm.navigateToReset.value)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun validationErrorShowsInlineAndInDialog() =
        runTest(dispatcherRule.testDispatcher) {
            api.forgotPasswordHandler = {
                throw api.httpError(
                    400,
                    """{"status":"error","code":"VALIDATION_FAILED","message":"Validation failed",""" +
                        """"errors":[{"field":"identity","message":"Must be a valid email"}]}""",
                )
            }
            val vm = viewModel()
            vm.start("a@b.co")
            vm.send()
            advanceUntilIdle()
            val state = vm.uiState.value
            assertEquals(FieldError.Text("Must be a valid email"), state.emailError)
            assertEquals(FieldError.Text("Validation failed"), state.dialogError)
            assertEquals(SaveState.Idle, state.saveState)
            assertNull(vm.navigateToReset.value)
        }

    @Test
    fun rateLimitShowsServerMessageInDialog() =
        runTest(dispatcherRule.testDispatcher) {
            api.forgotPasswordHandler = {
                throw api.httpError(
                    429,
                    """{"status":"error","code":"TOO_MANY_REQUESTS",""" +
                        """"message":"Too many attempts. Please try again in 15 minutes."}""",
                )
            }
            val vm = viewModel()
            vm.start("a@b.co")
            vm.send()
            advanceUntilIdle()
            assertEquals(
                FieldError.Text("Too many attempts. Please try again in 15 minutes."),
                vm.uiState.value.dialogError,
            )
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun networkFailureShowsNetworkDialog() =
        runTest(dispatcherRule.testDispatcher) {
            api.forgotPasswordHandler = { throw IOException("offline") }
            val vm = viewModel()
            vm.start("a@b.co")
            vm.send()
            advanceUntilIdle()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialogError)
        }

    @Test
    fun secondSendWhileSavingOrSavedMakesNoSecondCall() =
        runTest(dispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<SuccessMessageResponse>()
            api.forgotPasswordHandler = { gate.await() }
            val vm = viewModel()
            vm.start("a@b.co")
            vm.send()
            runCurrent()
            vm.send()
            runCurrent()
            assertEquals(1, api.forgotPasswordCalls)
            gate.complete(SuccessMessageResponse(status = "success"))
            advanceTimeBy(100)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            vm.send()
            runCurrent()
            assertEquals(1, api.forgotPasswordCalls)
        }

    @Test
    fun dismissDialogClearsDialogError() =
        runTest(dispatcherRule.testDispatcher) {
            api.forgotPasswordHandler = { throw IOException("offline") }
            val vm = viewModel()
            vm.start("a@b.co")
            vm.send()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.dialogError != null)
            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }
}
