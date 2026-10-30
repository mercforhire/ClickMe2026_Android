@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.initial

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/** Mirrors the private `SignupInitialViewModel.EMAIL_KEY`; the test pins that it is the only key written. */
private const val EMAIL_KEY = "signup_email"

class SignupInitialViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun signupData(emailVerified: Boolean = false) =
        SignupData(
            token = "access-s",
            user = AuthUser(id = "u9", firstName = "", lastName = "", roles = listOf("expert")),
            refreshToken = "refresh-s",
            expiresAt = 1_900_000_000L,
            expiresIn = 3600L,
            emailVerified = emailVerified,
        )

    private inner class Harness(
        scope: TestScope,
        handle: SavedStateHandle = SavedStateHandle(),
    ) {
        val h = SignupTestHarness(scope, tmp, main.testDispatcher)
        val api get() = h.api
        val viewModel = SignupInitialViewModel(h.userManager, h.repository, handle)

        fun fill(
            email: String = "ada@b.co",
            password: String = "abcdefg1",
        ) {
            viewModel.onEmailChange(email)
            viewModel.onPasswordChange(password)
        }

        fun fail(
            status: Int,
            json: String,
        ) {
            api.signupHandler = { throw api.httpError(status, json) }
        }
    }

    /** D-09, T-04-26: only the email is mirrored; the password is a secret and stays in memory. */
    @Test
    fun passwordNeverInSavedState() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val h = Harness(this, handle)

            h.fill(email = "ada@b.co", password = "abcdefg1")

            assertEquals("ada@b.co", handle.get<String>(EMAIL_KEY))
            assertEquals(setOf(EMAIL_KEY), handle.keys())
            assertTrue(handle.keys().none { handle.get<Any>(it) == "abcdefg1" })

            // After process death the password is empty with no error and no focus request.
            val restored = Harness(this, SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
            val state = restored.viewModel.uiState.value
            assertEquals("ada@b.co", state.email)
            assertEquals("", state.password)
            assertNull(state.passwordError)
            assertNull(restored.viewModel.focusRequest.value)
        }

    @Test
    fun typingBeforeFirstSubmitShowsNoError() =
        runTest(main.testDispatcher) {
            val h = Harness(this)

            h.viewModel.onEmailChange("bad")
            h.viewModel.onPasswordChange("x")

            assertNull(h.viewModel.uiState.value.emailError)
            assertNull(h.viewModel.uiState.value.passwordError)
        }

    @Test
    fun emptySubmitSetsRequiredErrorsFocusesEmailAndMakesNoCall() =
        runTest(main.testDispatcher) {
            val h = Harness(this)

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Res(R.string.error_email_required), state.emailError)
            assertEquals(FieldError.Res(R.string.error_password_required), state.passwordError)
            assertTrue(state.didAttemptSubmit)
            assertEquals(SignupField.Email, h.viewModel.focusRequest.value)
            assertEquals(0, h.api.signupCalls)
        }

    @Test
    fun validationIsLiveAfterTheFirstSubmit() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.viewModel.submit()
            runCurrent()

            h.viewModel.onPasswordChange("12345678")
            assertEquals(FieldError.Res(R.string.error_signup_password_letter), h.viewModel.uiState.value.passwordError)
            h.viewModel.onPasswordChange("abcdefgh")
            assertEquals(FieldError.Res(R.string.error_signup_password_number), h.viewModel.uiState.value.passwordError)
            h.viewModel.onEmailChange("nope")
            assertEquals(FieldError.Res(R.string.error_email_invalid), h.viewModel.uiState.value.emailError)
            h.viewModel.onPasswordChange("abcdefg1")
            assertNull(h.viewModel.uiState.value.passwordError)
        }

    @Test
    fun validSubmitTrimsEmailStoresTokensSeedsDraftAndRoutesToSetup() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            var sent: SignupRequest? = null
            h.api.signupHandler = {
                sent = it
                signupData(emailVerified = true)
            }
            h.fill(email = " Ada@B.co ")

            h.viewModel.submit()
            runCurrent()

            assertEquals(1, h.api.signupCalls)
            assertEquals("Ada@B.co", sent?.email)
            assertEquals("abcdefg1", sent?.password)
            assertEquals("access-s", h.h.tokens.accessToken)
            assertEquals("Ada@B.co", h.h.repository.draft.value.email)
            assertTrue(h.h.repository.draft.value.emailVerified)
            assertEquals(LaunchDestination.SignupSetup, h.viewModel.destination.value)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
            h.viewModel.onDestinationConsumed()
            assertNull(h.viewModel.destination.value)
        }

    @Test
    fun conflictShowsServerMessageUnderEmailWithoutDialog() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.fail(
                409,
                """{"status":"error","code":"EMAIL_TAKEN","message":"An account with that email already exists."}""",
            )
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Text("An account with that email already exists."), state.emailError)
            assertNull(state.dialogError)
            assertNull(h.viewModel.destination.value)
        }

    @Test
    fun conflictWithoutMessageUsesTheLocalString() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.fail(409, """{"status":"error"}""")
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Res(R.string.signup_initial_error_email_taken), state.emailError)
            assertNull(state.dialogError)
        }

    @Test
    fun unprocessableMapsEachErrorToItsField() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.fail(
                422,
                """{"status":"error","errors":[{"field":"email","message":"Invalid email format."},""" +
                    """{"field":"password","message":"Password must contain at least one digit."}]}""",
            )
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Text("Invalid email format."), state.emailError)
            assertEquals(FieldError.Text("Password must contain at least one digit."), state.passwordError)
            assertNull(state.dialogError)
        }

    @Test
    fun unprocessableWithOnlyAnUnrelatedFieldOpensTheDialog() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.fail(422, """{"status":"error","errors":[{"field":"role","message":"Role is invalid."}]}""")
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Text("Role is invalid."), state.dialogError)
            assertNull(state.emailError)
            assertNull(state.passwordError)
        }

    @Test
    fun serverErrorOpensTheDialogWithItsMessage() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.fail(500, """{"status":"error","message":"Something broke."}""")
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(FieldError.Text("Something broke."), h.viewModel.uiState.value.dialogError)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
            h.viewModel.dismissDialog()
            assertNull(h.viewModel.uiState.value.dialogError)
        }

    @Test
    fun networkFailureOpensTheNetworkDialog() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.api.signupHandler = { throw IOException("offline") }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(FieldError.Res(R.string.login_error_network), h.viewModel.uiState.value.dialogError)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun secondSubmitWhileInFlightMakesNoSecondCall() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            val gate = CompletableDeferred<SignupData>()
            h.api.signupHandler = { gate.await() }
            h.fill()

            h.viewModel.submit()
            runCurrent()
            assertTrue(h.viewModel.uiState.value.isSubmitting)
            h.viewModel.submit()
            runCurrent()
            assertEquals(1, h.api.signupCalls)

            gate.complete(signupData())
            runCurrent()
            assertFalse(h.viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun serverFieldErrorClearsOnTheNextEdit() =
        runTest(main.testDispatcher) {
            val h = Harness(this)
            h.fail(409, """{"status":"error","message":"Taken."}""")
            h.fill()
            h.viewModel.submit()
            runCurrent()

            h.viewModel.onEmailChange("other@b.co")

            assertNull(h.viewModel.uiState.value.emailError)
        }

    @Test
    fun emailIsMirroredToSavedStateAndRestoredButThePasswordNever() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val first = Harness(this, handle)
            first.fill(email = "ada@b.co", password = "secret-pass1")

            assertEquals("ada@b.co", handle.get<String>("signup_email"))
            assertEquals(setOf("signup_email"), handle.keys())
            assertFalse(handle.keys().any { handle.get<Any>(it) == "secret-pass1" })

            val restored = Harness(this, handle)
            assertEquals("ada@b.co", restored.viewModel.uiState.value.email)
            assertEquals("", restored.viewModel.uiState.value.password)
        }

    @Test
    fun strengthFollowsThePassword() =
        runTest(main.testDispatcher) {
            val h = Harness(this)

            h.viewModel.onPasswordChange("")
            val empty = h.viewModel.uiState.value.strength
            h.viewModel.onPasswordChange("abcdefg1")

            assertTrue(empty != h.viewModel.uiState.value.strength)
        }
}
