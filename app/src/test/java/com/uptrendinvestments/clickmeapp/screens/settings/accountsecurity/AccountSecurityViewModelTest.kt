package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.UpdatePasswordRequest
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

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

    @Test
    fun uiStateToStringNeverPrintsPasswords() {
        val state = AccountSecurityUiState(current = "old1pass", new = "new2pass", confirm = "new2pass")

        assertFalse(state.toString().contains("old1pass"))
        assertFalse(state.toString().contains("new2pass"))
    }
}
