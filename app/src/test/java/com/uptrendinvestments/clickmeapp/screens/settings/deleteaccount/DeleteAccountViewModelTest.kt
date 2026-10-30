@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.DeletionRequestData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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

class DeleteAccountViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()
    private val deletionState = AccountDeletionState()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        DeleteAccountViewModel(
            DeleteAccountRepository(api),
            guard,
            deletionState,
            CoroutineScope(main.testDispatcher),
            handle,
        )

    /** What the system hands the recreated ViewModel: the same entries in a new handle. */
    private fun copyOf(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })

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

    @Test
    fun requestStepAuthErrorsGoUnderPassword() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.fillReasonForm()
            api.requestAccountDeletionHandler = {
                throw api.httpError(401, """{"status":"error","message":"Invalid password."}""")
            }

            vm.confirmDeletion()
            runCurrent()

            assertEquals(FieldError.Text("Invalid password."), vm.uiState.value.passwordError)
            assertNull(vm.uiState.value.dialog)
            assertFalse(vm.uiState.value.isDeleting)

            api.requestAccountDeletionHandler = { throw api.httpError(403, """{"status":"error"}""") }
            vm.confirmDeletion()
            runCurrent()

            assertEquals(FieldError.Res(R.string.delete_account_password_incorrect), vm.uiState.value.passwordError)
            assertEquals(0, api.deleteAccountCalls)
            assertFalse(deletionState.farewellVisible.value)
        }

    @Test
    fun expiredTokenGoesUnderPassword() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.fillReasonForm()
            api.requestAccountDeletionHandler = { DeletionRequestData("token-1") }
            api.deleteAccountHandler = {
                throw api.httpError(400, """{"status":"error","code":"INVALID_TOKEN","message":"Token expired."}""")
            }

            vm.confirmDeletion()
            runCurrent()

            assertEquals(FieldError.Res(R.string.delete_account_expired), vm.uiState.value.passwordError)
            assertNull(vm.uiState.value.dialog)
            assertFalse(deletionState.farewellVisible.value)
        }

    @Test
    fun conflictShowsServerMessageAndRetryMintsNewToken() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.fillReasonForm(reason = DeleteAccountReason.Other, feedback = "bye")
            var minted = 0
            api.requestAccountDeletionHandler = { DeletionRequestData("token-${++minted}") }
            api.deleteAccountHandler = {
                throw api.httpError(
                    409,
                    """{"status":"error","code":"EXPERT_HAS_UPCOMING_BOOKINGS",""" +
                        """"message":"Finish your 2 upcoming sessions first."}""",
                )
            }

            vm.confirmDeletion()
            runCurrent()

            assertEquals(FieldError.Text("Finish your 2 upcoming sessions first."), vm.uiState.value.dialog)
            val kept = vm.uiState.value
            assertEquals(DeleteAccountReason.Other, kept.reason)
            assertEquals("bye", kept.feedback)
            assertEquals("pw1", kept.password)
            assertEquals(DeleteStep.Reason, kept.step)

            vm.dismissDialog()
            vm.confirmDeletion()
            runCurrent()

            assertEquals(2, api.requestAccountDeletionCalls)
            assertEquals(listOf("token-1", "token-2"), api.deleteAccountRequests.map { it.deletionToken })
        }

    @Test
    fun clientUpcomingBookingsCodeDecodes() {
        assertEquals(ErrorCode.ClientHasUpcomingBookings, ErrorCode.fromWire("CLIENT_HAS_UPCOMING_BOOKINGS"))
    }

    @Test
    fun rateLimitAndNetworkDialogs() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.fillReasonForm()

            api.requestAccountDeletionHandler = {
                throw api.httpError(429, """{"status":"error","code":"TOO_MANY_REQUESTS"}""")
            }
            vm.confirmDeletion()
            runCurrent()
            assertEquals(FieldError.Res(R.string.delete_account_too_many), vm.uiState.value.dialog)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)

            api.requestAccountDeletionHandler = { throw IOException("offline") }
            vm.confirmDeletion()
            runCurrent()
            assertEquals(FieldError.Res(R.string.delete_account_network), vm.uiState.value.dialog)

            vm.dismissDialog()
            api.requestAccountDeletionHandler = { throw api.httpError(500, """{"status":"error","message":"boom"}""") }
            vm.confirmDeletion()
            runCurrent()
            assertEquals(FieldError.Res(R.string.delete_account_generic), vm.uiState.value.dialog)
            assertFalse(vm.uiState.value.isDeleting)
        }

    @Test
    fun doubleTapSendsOnePair() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.fillReasonForm()
            val gate = CompletableDeferred<Unit>()
            api.requestAccountDeletionHandler = {
                gate.await()
                DeletionRequestData("token-1")
            }
            api.deleteAccountHandler = { SuccessMessageResponse(status = "success") }

            vm.confirmDeletion()
            vm.confirmDeletion()
            runCurrent()

            assertTrue(vm.uiState.value.isDeleting)
            assertEquals(1, api.requestAccountDeletionCalls)

            gate.complete(Unit)
            runCurrent()

            assertEquals(1, api.requestAccountDeletionCalls)
            assertEquals(1, api.deleteAccountCalls)
            assertTrue(deletionState.farewellVisible.value)
            assertTrue("the spinner stays up behind the farewell", vm.uiState.value.isDeleting)
        }

    @Test
    fun guardRefusalSendsNothing() =
        runTest(main.testDispatcher) {
            stubSuccess()
            val vm = viewModel()
            guard.blocked = true

            vm.continueFromWarning()
            assertEquals(DeleteStep.Warning, vm.uiState.value.step)

            guard.blocked = false
            vm.fillReasonForm()
            guard.blocked = true
            vm.confirmDeletion()
            runCurrent()

            assertEquals(0, api.requestAccountDeletionCalls)
            assertEquals(0, api.deleteAccountCalls)
            assertFalse(vm.uiState.value.isDeleting)
        }

    @Test
    fun feedbackCapsAt1000() =
        runTest(main.testDispatcher) {
            val vm = viewModel()
            vm.onFeedbackChange("a".repeat(1200))
            assertEquals(FEEDBACK_MAX, vm.uiState.value.feedback.length)
        }

    @Test
    fun uiStateToStringDoesNotLeakThePassword() {
        val state = DeleteAccountUiState(password = "hunter2", feedback = "private words")
        val text = state.toString()
        assertFalse(text.contains("hunter2"))
        assertFalse(text.contains("private words"))
    }

    // ---- process death and recreation (CONTEXT D-12, D-09) ----

    @Test
    fun reasonFeedbackAndStepRestored() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle)
            first.continueFromWarning()
            first.selectReason(DeleteAccountReason.Other)
            first.onFeedbackChange("Too many emails")

            val second = viewModel(copyOf(handle))

            val state = second.uiState.value
            assertEquals(DeleteStep.Reason, state.step)
            assertEquals(DeleteAccountReason.Other, state.reason)
            assertEquals("Too many emails", state.feedback)
        }

    @Test
    fun backToWarningIsRestored() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(handle)
            first.continueFromWarning()
            first.backToWarning()

            assertEquals(DeleteStep.Warning, viewModel(copyOf(handle)).uiState.value.step)
        }

    @Test
    fun restoredFeedbackIsCapped() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle(mapOf("delete_account_feedback" to "x".repeat(FEEDBACK_MAX + 50)))

            assertEquals(
                FEEDBACK_MAX,
                viewModel(handle)
                    .uiState.value.feedback.length,
            )
        }

    @Test
    fun passwordNeverInSavedState() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm = viewModel(handle)
            vm.fillReasonForm(feedback = "bye", password = "s3cret-pw")

            assertTrue(handle.keys().isNotEmpty())
            for (key in handle.keys()) {
                val value = handle.get<Any>(key)
                assertFalse("$key must not hold the password", value.toString().contains("s3cret-pw"))
                assertFalse("no password key: $key", key.contains("password", ignoreCase = true))
                assertTrue("$key holds ${value?.javaClass}", value is String)
            }
        }

    @Test
    fun restoredPasswordEmptyWithoutError() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            viewModel(handle).fillReasonForm(password = "s3cret-pw")

            val state = viewModel(copyOf(handle)).uiState.value

            assertEquals("", state.password)
            assertNull(state.passwordError)
            assertNull(state.dialog)
        }
}
