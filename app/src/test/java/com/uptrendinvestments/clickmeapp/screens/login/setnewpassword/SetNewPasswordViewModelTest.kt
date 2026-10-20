@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.login.setnewpassword

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrength
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
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

class SetNewPasswordViewModelTest {
    @get:Rule
    val rule = MainDispatcherRule()

    private val ok = SuccessMessageResponse(status = "success", message = "done")

    private fun harness(
        saved: SavedStateHandle = SavedStateHandle(),
    ): Pair<FakeClickMeApi, SetNewPasswordViewModel> {
        val api = FakeClickMeApi()
        return api to SetNewPasswordViewModel(api, saved)
    }

    private fun SetNewPasswordViewModel.fillValid() {
        start("a@b.co")
        onCodeChange("123456")
        onNewPasswordChange("abcdefg1!")
        onConfirmChange("abcdefg1!")
    }

    @Test
    fun codeIsSanitizedMirroredAndRestored_passwordsNeverSaved() =
        runTest(rule.testDispatcher) {
            val saved = SavedStateHandle()
            val (_, vm) = harness(saved)
            vm.start("a@b.co")
            vm.onCodeChange("12a34 5678")
            vm.onNewPasswordChange("abcdefg1!")
            vm.onConfirmChange("abcdefg1!")
            assertEquals("123456", vm.uiState.value.code)
            assertEquals("123456", saved.get<String>("reset_code"))
            assertTrue(saved.keys().none { it.contains("password", ignoreCase = true) })
            assertTrue(saved.keys().all { saved.get<Any>(it) == "123456" })

            val (_, restored) = harness(saved)
            assertEquals("123456", restored.uiState.value.code)
            assertEquals("", restored.uiState.value.newPassword)
            assertEquals("", restored.uiState.value.confirmPassword)
        }

    @Test
    fun typingBeforeFirstSubmit_showsNoErrors_andDerivesStrengthAndMatch() =
        runTest(rule.testDispatcher) {
            val (_, vm) = harness()
            vm.start("a@b.co")
            vm.onCodeChange("1")
            vm.onNewPasswordChange("abc")
            vm.onConfirmChange("x")
            val s = vm.uiState.value
            assertNull(s.codeError)
            assertNull(s.newPasswordError)
            assertNull(s.confirmError)
            assertEquals(PasswordStrength.evaluate("abc"), s.strength)
            assertFalse(s.passwordsMatch)
            vm.onNewPasswordChange("abc")
            vm.onConfirmChange("abc")
            assertTrue(vm.uiState.value.passwordsMatch)
            vm.onConfirmChange("")
            assertFalse(vm.uiState.value.passwordsMatch)
        }

    @Test
    fun emptySubmit_setsAllRequiredErrors_focusesCode_andMakesNoCall() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            vm.start("a@b.co")
            vm.submit()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.error_code_required), s.codeError)
            assertEquals(FieldError.Res(R.string.error_new_password_required), s.newPasswordError)
            assertEquals(FieldError.Res(R.string.error_confirm_required), s.confirmError)
            assertTrue(s.didAttemptSubmit)
            assertEquals(ResetField.Code, vm.focusRequest.value)
            assertEquals(0, api.resetPasswordCalls)
            vm.onFocusRequestConsumed()
            assertNull(vm.focusRequest.value)
        }

    @Test
    fun afterFirstSubmit_eachFieldRevalidatesLive() =
        runTest(rule.testDispatcher) {
            val (_, vm) = harness()
            vm.start("a@b.co")
            vm.submit()
            vm.onNewPasswordChange("abcdefgh")
            assertEquals(FieldError.Res(R.string.error_new_password_rules), vm.uiState.value.newPasswordError)
            vm.onConfirmChange("x")
            assertEquals(FieldError.Res(R.string.error_confirm_mismatch), vm.uiState.value.confirmError)
            vm.onCodeChange("12345")
            assertEquals(FieldError.Res(R.string.error_code_invalid), vm.uiState.value.codeError)
            vm.onCodeChange("123456")
            assertNull(vm.uiState.value.codeError)
        }

    @Test
    fun blankEmail_showsMissingEmailDialog_andMakesNoCall() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            vm.onCodeChange("123456")
            vm.onNewPasswordChange("abcdefg1!")
            vm.onConfirmChange("abcdefg1!")
            vm.submit()
            runCurrent()
            assertEquals(FieldError.Res(R.string.error_reset_missing_email), vm.uiState.value.dialogError)
            assertEquals(0, api.resetPasswordCalls)
            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun success_sendsFourFieldBody_holdsSavedFor600ms_thenEmitsEmail() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            val gate = CompletableDeferred<Unit>()
            var sent: ResetPasswordRequest? = null
            api.resetPasswordHandler = {
                sent = it
                gate.await()
                ok
            }
            vm.fillValid()
            vm.submit()
            runCurrent()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            gate.complete(Unit)
            runCurrent()
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertEquals(ResetPasswordRequest("a@b.co", "123456", "abcdefg1!", "abcdefg1!"), sent)
            assertEquals(1, api.resetPasswordCalls)
            advanceTimeBy(599)
            assertNull(vm.passwordChanged.value)
            advanceTimeBy(2)
            runCurrent()
            assertEquals("a@b.co", vm.passwordChanged.value)
            vm.onNavigated()
            assertNull(vm.passwordChanged.value)
        }

    @Test
    fun http400_showsExpiredCodeInline_notServerText() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = {
                throw api.httpError(400, """{"code":"EXPIRED_TOKEN","message":"Invalid or expired code"}""")
            }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(FieldError.Res(R.string.error_code_expired), s.codeError)
            assertEquals(ResetField.Code, vm.focusRequest.value)
            assertEquals(SaveState.Idle, s.saveState)
            assertNull(s.dialogError)
            assertNull(vm.passwordChanged.value)
        }

    @Test
    fun http422_mapsFieldErrorsInline() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = {
                throw api.httpError(
                    422,
                    """{"code":"VALIDATION_ERROR","errors":[
                        {"field":"password","message":"Too weak"},
                        {"field":"password_confirmation","message":"Mismatch"},
                        {"field":"code","message":"Bad code"}]}""",
                )
            }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(FieldError.Text("Too weak"), s.newPasswordError)
            assertEquals(FieldError.Text("Mismatch"), s.confirmError)
            assertEquals(FieldError.Text("Bad code"), s.codeError)
            assertNull(s.dialogError)
            assertEquals(SaveState.Idle, s.saveState)
        }

    @Test
    fun http422_emailError_goesToDialog() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = {
                throw api.httpError(422, """{"errors":[{"field":"email","message":"Email is invalid"}]}""")
            }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            assertEquals(FieldError.Text("Email is invalid"), vm.uiState.value.dialogError)
            assertNull(vm.uiState.value.codeError)
        }

    @Test
    fun http422_unknownField_fallsBackToDialog() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = {
                throw api.httpError(422, """{"errors":[{"field":"surprise","message":"Something odd"}]}""")
            }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            val s = vm.uiState.value
            assertEquals(FieldError.Text("Something odd"), s.dialogError)
            assertNull(s.codeError)
            assertNull(s.newPasswordError)
            assertNull(s.confirmError)
        }

    @Test
    fun http500_showsServerMessageInDialog() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = { throw api.httpError(500, """{"message":"Server down"}""") }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            assertEquals(FieldError.Text("Server down"), vm.uiState.value.dialogError)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun ioException_showsNetworkMessage() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = { throw IOException("offline") }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialogError)
        }

    @Test
    fun secondSubmitWhileSaving_makesNoSecondCall() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            val gate = CompletableDeferred<Unit>()
            api.resetPasswordHandler = {
                gate.await()
                ok
            }
            vm.fillValid()
            vm.submit()
            runCurrent()
            vm.submit()
            runCurrent()
            assertEquals(1, api.resetPasswordCalls)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals("a@b.co", vm.passwordChanged.value)
        }

    @Test
    fun serverTextError_isReplacedOnThatFieldsNextEdit() =
        runTest(rule.testDispatcher) {
            val (api, vm) = harness()
            api.resetPasswordHandler = {
                throw api.httpError(422, """{"errors":[{"field":"password","message":"Too weak"}]}""")
            }
            vm.fillValid()
            vm.submit()
            advanceUntilIdle()
            assertEquals(FieldError.Text("Too weak"), vm.uiState.value.newPasswordError)
            vm.onNewPasswordChange("abcdefg1!x")
            assertNull(vm.uiState.value.newPasswordError)
        }
}
