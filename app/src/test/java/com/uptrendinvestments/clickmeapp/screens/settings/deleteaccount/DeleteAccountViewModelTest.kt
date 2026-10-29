@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.DeletionRequestData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
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

class DeleteAccountViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()
    private val deletionState = AccountDeletionState()

    private fun viewModel() = DeleteAccountViewModel(DeleteAccountRepository(api), guard, deletionState)

    private fun stubSuccess() {
        var minted = 0
        api.requestAccountDeletionHandler = { DeletionRequestData("token-${++minted}") }
        api.deleteAccountHandler = { SuccessMessageResponse(status = "success") }
    }

    private fun DeleteAccountViewModel.fillReasonForm(
        reason: DeleteAccountReason = DeleteAccountReason.PrivacyConcerns,
        feedback: String = "",
        password: String = "pw1",
    ) {
        continueFromWarning()
        selectReason(reason)
        onFeedbackChange(feedback)
        onPasswordChange(password)
    }

    @Test
    fun happyPathRunsBothCallsThenShowsFarewell() =
        runTest(main.testDispatcher) {
            stubSuccess()
            val vm = viewModel()

            vm.continueFromWarning()
            assertEquals(DeleteStep.Reason, vm.uiState.value.step)
            vm.selectReason(DeleteAccountReason.PrivacyConcerns)
            vm.onFeedbackChange("  moving on  ")
            vm.onPasswordChange("pw1")
            vm.confirmDeletion()
            runCurrent()

            assertEquals(listOf("pw1"), api.accountDeletionRequests.map { it.password })
            val delete = api.deleteAccountRequests.single()
            assertEquals("token-1", delete.deletionToken)
            assertEquals("privacy_concerns", delete.reasonCategory)
            assertEquals("moving on", delete.feedbackDetails)
            assertTrue(delete.confirmPermanentAction)
            assertTrue(deletionState.farewellVisible.value)
            assertEquals(
                listOf(RestrictedAction.DeleteAccount, RestrictedAction.DeleteAccount),
                guard.attempts,
            )
        }

    @Test
    fun blankFeedbackIsOmitted() =
        runTest(main.testDispatcher) {
            stubSuccess()
            val vm = viewModel()
            vm.fillReasonForm(feedback = "   ")

            vm.confirmDeletion()
            runCurrent()

            assertNull(api.deleteAccountRequests.single().feedbackDetails)
        }

    @Test
    fun canConfirmNeedsReasonAndPassword() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.continueFromWarning()
            assertFalse(vm.uiState.value.canConfirm)

            vm.onPasswordChange("pw1")
            assertFalse("a password alone is not enough", vm.uiState.value.canConfirm)

            vm.onPasswordChange("   ")
            vm.selectReason(DeleteAccountReason.Other)
            assertFalse("a blank password does not count", vm.uiState.value.canConfirm)

            vm.onPasswordChange("pw1")
            assertTrue(vm.uiState.value.canConfirm)
        }

    @Test
    fun confirmWithoutAReasonSendsNothing() =
        runTest(main.testDispatcher) {
            stubSuccess()
            val vm = viewModel()
            vm.continueFromWarning()
            vm.onPasswordChange("pw1")

            vm.confirmDeletion()
            runCurrent()

            assertEquals(0, api.requestAccountDeletionCalls)
            assertFalse(deletionState.farewellVisible.value)
        }
}
