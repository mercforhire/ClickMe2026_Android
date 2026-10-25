@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.login

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import com.uptrendinvestments.clickmeapp.testing.topicItem
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

class LoginViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun loginData(vararg roles: String) =
        LoginData(
            token = "access",
            user = AuthUser(id = "u1", firstName = "Ada", lastName = "Lovelace", roles = roles.toList()),
            refreshToken = "refresh",
            expiresAt = 1_900_000_000L,
            expiresIn = 3600L,
        )

    private inner class Harness(
        scope: TestScope,
        handle: SavedStateHandle = SavedStateHandle(),
    ) {
        val api = FakeClickMeApi()
        val tokens = FakeSecureTokenStore(null)
        val events = SessionEvents()
        val manager =
            UserManager(
                api,
                tokens,
                SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp)),
                events,
                PushTokenRegistrar(),
                scope.backgroundScope,
                dispatcherRule.testDispatcher,
            )
        val viewModel = LoginViewModel(manager, handle)

        init {
            // Background profile refresh after login is best-effort; fail it quietly.
            api.userProfileHandler = { throw IOException("not needed") }
        }

        fun fill(
            email: String = "a@b.co",
            password: String = "password1",
        ) {
            viewModel.onEmailChange(email)
            viewModel.onPasswordChange(password)
        }

        fun loginOk(vararg roles: String = arrayOf("client")) {
            api.loginHandler = { loginData(*roles) }
        }

        fun wrongCredentials() {
            api.loginHandler = {
                throw api.httpError(401, """{"status":"error","message":"Invalid email or password"}""")
            }
        }
    }

    @Test
    fun typingInvalidEmailBeforeFirstSubmitShowsNoError() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)

            h.viewModel.onEmailChange("bad")
            h.viewModel.onPasswordChange("x")

            assertNull(h.viewModel.uiState.value.emailError)
            assertNull(h.viewModel.uiState.value.passwordError)
        }

    @Test
    fun emptySubmitSetsRequiredErrorsFocusesEmailAndMakesNoApiCall() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Res(R.string.error_email_required), state.emailError)
            assertEquals(FieldError.Res(R.string.error_password_required), state.passwordError)
            assertTrue(state.didAttemptSubmit)
            assertEquals(LoginField.Email, h.viewModel.focusRequest.value)
            assertEquals(0, h.api.loginCalls)
            h.viewModel.onFocusRequestConsumed()
            assertNull(h.viewModel.focusRequest.value)
        }

    @Test
    fun invalidPasswordOnlyFocusesPassword() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.fill(password = "short")

            h.viewModel.submit()
            runCurrent()

            assertNull(h.viewModel.uiState.value.emailError)
            assertEquals(FieldError.Res(R.string.error_password_too_short), h.viewModel.uiState.value.passwordError)
            assertEquals(LoginField.Password, h.viewModel.focusRequest.value)
        }

    @Test
    fun afterFailedSubmitFieldsRevalidateLiveAsTheUserTypes() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.viewModel.submit()
            runCurrent()

            h.viewModel.onEmailChange("bad")
            assertEquals(FieldError.Res(R.string.error_email_invalid), h.viewModel.uiState.value.emailError)
            h.viewModel.onEmailChange("a@b.co")
            assertNull(h.viewModel.uiState.value.emailError)
            h.viewModel.onPasswordChange("short")
            assertEquals(FieldError.Res(R.string.error_password_too_short), h.viewModel.uiState.value.passwordError)
            h.viewModel.onPasswordChange("password1")
            assertNull(h.viewModel.uiState.value.passwordError)
        }

    @Test
    fun submitSendsTrimmedEmailAndUntouchedPassword() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            var sent: Pair<String, String>? = null
            h.api.loginHandler = {
                sent = it.email to it.password
                loginData("client")
            }
            h.fill(email = "  a@b.co ", password = "password1")

            h.viewModel.submit()
            runCurrent()

            assertEquals("a@b.co" to "password1", sent)
        }

    @Test
    fun clientSuccessSetsHomeClientDestination() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.loginOk()
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LaunchDestination.Home(AppMode.Client), h.viewModel.destination.value)
            assertNull(h.viewModel.uiState.value.passwordError)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun expertWithIncompleteSetupRoutesToSignupSetup() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.loginOk("client", "expert")
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = false) }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LaunchDestination.SignupSetup, h.viewModel.destination.value)
        }

    @Test
    fun expertWithTopicsRoutesToExpertHome() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.loginOk("client", "expert")
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(listOf(topicItem())) }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.viewModel.destination.value)
        }

    @Test
    fun wrongCredentialsShowServerMessageUnderPasswordOnly() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.wrongCredentials()
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Text("Invalid email or password"), state.passwordError)
            assertNull(state.emailError)
            assertNull(state.dialogError)
            assertNull(h.viewModel.destination.value)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun serverPasswordErrorClearsOnNextEmailEdit() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.wrongCredentials()
            h.fill()
            h.viewModel.submit()
            runCurrent()

            h.viewModel.onEmailChange("other@b.co")

            assertNull(h.viewModel.uiState.value.passwordError)
        }

    @Test
    fun serverPasswordErrorClearsOnNextPasswordEdit() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.wrongCredentials()
            h.fill()
            h.viewModel.submit()
            runCurrent()

            h.viewModel.onPasswordChange("password2")

            assertNull(h.viewModel.uiState.value.passwordError)
        }

    @Test
    fun unprocessableEmailFieldErrorShowsUnderEmailWithoutDialog() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","errors":[{"field":"email","message":"The email must be valid."}]}""",
                )
            }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(FieldError.Text("The email must be valid."), state.emailError)
            assertNull(state.dialogError)
        }

    @Test
    fun serverErrorOpensDialogAndDismissClearsIt() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { throw h.api.httpError(500, """{"message":"Server unavailable"}""") }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(FieldError.Text("Server unavailable"), h.viewModel.uiState.value.dialogError)
            assertNull(h.viewModel.uiState.value.passwordError)
            h.viewModel.dismissDialog()
            assertNull(h.viewModel.uiState.value.dialogError)
        }

    @Test
    fun ioExceptionOpensNetworkDialog() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { throw IOException("offline") }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(FieldError.Res(R.string.login_error_network), h.viewModel.uiState.value.dialogError)
        }

    @Test
    fun suspendedAccountShowsNoErrorOfAnyKind() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = {
                throw h.api.httpError(
                    403,
                    """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"Account suspended"}""",
                )
            }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            val state = h.viewModel.uiState.value
            assertNull(state.emailError)
            assertNull(state.passwordError)
            assertNull(state.dialogError)
            assertNull(h.viewModel.destination.value)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun secondSubmitWhileInFlightMakesNoSecondCall() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.loginHandler = {
                gate.await()
                loginData("client")
            }
            h.fill()

            h.viewModel.submit()
            runCurrent()
            assertTrue(h.viewModel.uiState.value.isSubmitting)
            h.viewModel.submit()
            runCurrent()
            gate.complete(Unit)
            runCurrent()

            assertEquals(1, h.api.loginCalls)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun prefillSetsEmailAndClearsPasswordAndErrors() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.viewModel.onPasswordChange("password1")
            h.viewModel.submit()
            runCurrent()

            h.viewModel.prefill("x@y.co")

            val state = h.viewModel.uiState.value
            assertEquals("x@y.co", state.email)
            assertEquals("", state.password)
            assertNull(state.emailError)
            assertNull(state.passwordError)
            assertNull(state.dialogError)
            assertFalse(state.didAttemptSubmit)
        }

    @Test
    fun emailIsRestoredFromSavedStateAndPasswordIsNotStored() =
        runTest(dispatcherRule.testDispatcher) {
            val handle = SavedStateHandle(mapOf("login_email" to "kept@b.co"))
            val h = Harness(this, handle)
            assertEquals("kept@b.co", h.viewModel.uiState.value.email)

            h.viewModel.onEmailChange("new@b.co")
            h.viewModel.onPasswordChange("password1")

            assertEquals("new@b.co", handle.get<String>("login_email"))
            assertEquals(setOf("login_email"), handle.keys())
        }

    @Test
    fun onDestinationConsumedClearsDestination() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.loginOk()
            h.fill()
            h.viewModel.submit()
            runCurrent()
            assertEquals(LaunchDestination.Home(AppMode.Client), h.viewModel.destination.value)

            h.viewModel.onDestinationConsumed()

            assertNull(h.viewModel.destination.value)
        }
}
