package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.UpdatePasswordRequest
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSecurityViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()

    init {
        api.updatePasswordHandler = { SuccessMessageResponse(status = "success") }
    }

    private fun vm() = AccountSecurityViewModel(AccountSecurityRepository(api), guard)

    private fun AccountSecurityViewModel.fill(
        current: String = "old1pass",
        new: String = "new2pass",
        confirm: String = "new2pass",
    ) {
        onCurrentChange(current)
        onNewChange(new)
        onConfirmChange(confirm)
    }

    @Test
    fun validChangeSendsOnceAndClears() =
        runTest(main.testDispatcher) {
            val vm = vm()
            vm.fill()

            vm.submit()
            vm.submit()
            runCurrent()

            assertEquals(1, api.updatePasswordCalls)
            assertEquals(UpdatePasswordRequest("old1pass", "new2pass"), api.updatePasswordRequests.single())
            val state = vm.uiState.value
            assertEquals("", state.current)
            assertEquals("", state.new)
            assertEquals("", state.confirm)
            assertEquals(SaveState.Idle, state.saveState)
            assertEquals(AccountSecurityDialog.Success, state.dialog)
        }

    @Test
    fun guardRefusalSendsNothing() =
        runTest(main.testDispatcher) {
            guard.blocked = true
            val vm = vm()
            vm.fill()

            vm.submit()
            runCurrent()

            assertEquals(listOf(RestrictedAction.ChangePassword), guard.attempts)
            assertEquals(0, api.updatePasswordCalls)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertEquals("new2pass", vm.uiState.value.new)
        }

    private fun AccountSecurityViewModel.fieldError(field: PasswordField) = uiState.value.fieldErrors[field]

    private fun assertRejectedLocally(
        field: PasswordField,
        @StringRes message: Int,
        current: String = "old1pass",
        new: String = "new2pass",
        confirm: String = "new2pass",
    ) = runTest(main.testDispatcher) {
        val vm = vm()
        vm.fill(current, new, confirm)

        vm.submit()
        runCurrent()

        assertEquals(mapOf(field to FieldError.Res(message)), vm.uiState.value.fieldErrors)
        assertEquals(0, api.updatePasswordCalls)
        assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        assertTrue(guard.attempts.isEmpty())
    }

    @Test
    fun validationOrder() {
        assertRejectedLocally(PasswordField.Current, R.string.account_security_error_current_empty, current = "")
        assertRejectedLocally(PasswordField.New, R.string.account_security_error_new_empty, new = "")
        assertRejectedLocally(PasswordField.New, R.string.account_security_error_new_short, new = "abc1")
        assertRejectedLocally(PasswordField.New, R.string.account_security_error_new_no_number, new = "abcdefgh")
        assertRejectedLocally(
            PasswordField.New,
            R.string.account_security_error_new_same,
            current = "same1234",
            new = "same1234",
            confirm = "same1234",
        )
        assertRejectedLocally(PasswordField.Confirm, R.string.account_security_error_confirm_empty, confirm = "")
        assertRejectedLocally(
            PasswordField.Confirm,
            R.string.account_security_error_confirm_mismatch,
            confirm = "other3pass",
        )
    }

    @Test
    fun editingAFieldClearsOnlyItsOwnError() =
        runTest(main.testDispatcher) {
            val vm = vm()
            vm.fill(current = "", new = "abc1", confirm = "")
            vm.submit()
            assertEquals(setOf(PasswordField.Current), vm.uiState.value.fieldErrors.keys)

            vm.onNewChange("new2pass")
            assertEquals(setOf(PasswordField.Current), vm.uiState.value.fieldErrors.keys)
            vm.onCurrentChange("old1pass")
            assertEquals(emptyMap<PasswordField, FieldError>(), vm.uiState.value.fieldErrors)
        }

    @Test
    fun forbiddenMapsToCurrentField() =
        runTest(main.testDispatcher) {
            api.updatePasswordHandler = {
                throw api.httpError(403, """{"status":"error","message":"Current password is incorrect"}""")
            }
            val vm = vm()
            vm.fill()
            vm.submit()
            runCurrent()

            assertEquals(FieldError.Text("Current password is incorrect"), vm.fieldError(PasswordField.Current))
            assertNull(vm.uiState.value.dialog)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertEquals("new2pass", vm.uiState.value.new)

            api.updatePasswordHandler = { throw api.httpError(403, """{"status":"error"}""") }
            vm.submit()
            runCurrent()

            assertEquals(
                FieldError.Res(R.string.account_security_wrong_current),
                vm.fieldError(PasswordField.Current),
            )
        }

    @Test
    fun validationEnvelopeMapsFields() =
        runTest(main.testDispatcher) {
            api.updatePasswordHandler = {
                throw api.httpError(
                    422,
                    """{"status":"error","code":"VALIDATION_ERROR","errors":[""" +
                        """{"field":"current_password","message":"Wrong"},""" +
                        """{"field":"new_password","message":"Too weak"}]}""",
                )
            }
            val vm = vm()
            vm.fill()
            vm.submit()
            runCurrent()

            assertEquals(FieldError.Text("Wrong"), vm.fieldError(PasswordField.Current))
            assertEquals(FieldError.Text("Too weak"), vm.fieldError(PasswordField.New))
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun validationEnvelopeWithUnknownFieldOpensGenericDialog() =
        runTest(main.testDispatcher) {
            api.updatePasswordHandler = {
                throw api.httpError(
                    422,
                    """{"status":"error","errors":[{"field":"something_else","message":"Nope"}]}""",
                )
            }
            val vm = vm()
            vm.fill()
            vm.submit()
            runCurrent()

            assertEquals(emptyMap<PasswordField, FieldError>(), vm.uiState.value.fieldErrors)
            assertEquals(failureDialog, vm.uiState.value.dialog)
        }

    private val failureDialog =
        AccountSecurityDialog.Error(
            R.string.account_security_failed_title,
            FieldError.Res(R.string.account_security_failed_body),
        )

    @Test
    fun tooManyAttemptsDialog() =
        runTest(main.testDispatcher) {
            api.updatePasswordHandler = { throw api.httpError(429, """{"status":"error","message":"slow down"}""") }
            val vm = vm()
            vm.fill()
            vm.submit()
            runCurrent()

            assertEquals(
                AccountSecurityDialog.Error(
                    R.string.account_security_failed_title,
                    FieldError.Res(R.string.account_security_too_many),
                ),
                vm.uiState.value.dialog,
            )
            assertEquals(1, api.updatePasswordCalls)
        }

    @Test
    fun networkErrorOpensFailureDialog() =
        runTest(main.testDispatcher) {
            api.updatePasswordHandler = { throw IOException("offline") }
            val vm = vm()
            vm.fill()
            vm.submit()
            runCurrent()

            assertEquals(failureDialog, vm.uiState.value.dialog)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun deleteEntryIsGuarded() {
        val vm = vm()

        assertTrue(vm.requestDeleteAccount())
        guard.blocked = true
        assertFalse(vm.requestDeleteAccount())
        assertEquals(listOf(RestrictedAction.DeleteAccount, RestrictedAction.DeleteAccount), guard.attempts)
    }

    @Test
    fun uiStateToStringNeverPrintsPasswords() {
        val state = AccountSecurityUiState(current = "old1pass", new = "new2pass", confirm = "new2pass")

        assertFalse(state.toString().contains("old1pass"))
        assertFalse(state.toString().contains("new2pass"))
    }
}
