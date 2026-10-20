@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.login

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
        val viewModel = LoginViewModel(manager)

        init {
            // Background profile refresh after login is best-effort; fail it quietly.
            api.userProfileHandler = { throw IOException("not needed") }
        }

        fun fill(
            email: String = "a@b.co",
            password: String = "pw",
        ) {
            viewModel.onEmailChange(email)
            viewModel.onPasswordChange(password)
        }
    }

    @Test
    fun blankFieldsSetMissingFieldsAndMakeNoApiCall() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.fill(email = "  ", password = "")

            h.viewModel.submit()
            runCurrent()

            assertEquals(LoginError.MissingFields, h.viewModel.uiState.value.error)
            assertEquals(0, h.api.loginCalls)
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
            h.fill(email = "  a@b.co ", password = " pw ")

            h.viewModel.submit()
            runCurrent()

            assertEquals("a@b.co" to " pw ", sent)
        }

    @Test
    fun clientSuccessSetsHomeClientDestination() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { loginData("client") }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LaunchDestination.Home(AppMode.Client), h.viewModel.destination.value)
            assertNull(h.viewModel.uiState.value.error)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun expertWithIncompleteSetupRoutesToSignupSetup() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { loginData("client", "expert") }
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
            h.api.loginHandler = { loginData("client", "expert") }
            h.api.expertProfileHandler = { ExpertProfileData(setupCompleted = true) }
            h.api.topicsHandler = { MyExpertTopicsPayload(listOf(mapOf("id" to "t1"))) }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LaunchDestination.Home(AppMode.Expert), h.viewModel.destination.value)
        }

    @Test
    fun wrongCredentialsShowServerMessageAndNoDestination() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = {
                throw h.api.httpError(401, """{"status":"error","message":"Invalid email or password"}""")
            }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LoginError.Server("Invalid email or password"), h.viewModel.uiState.value.error)
            assertNull(h.viewModel.destination.value)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun fieldErrorFor422ShowsFieldMessage() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","errors":[{"field":"email","message":"Email is invalid"}]}""",
                )
            }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LoginError.Server("Email is invalid"), h.viewModel.uiState.value.error)
        }

    @Test
    fun ioExceptionMapsToNetworkError() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { throw IOException("offline") }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LoginError.Network, h.viewModel.uiState.value.error)
        }

    @Test
    fun decodingFailureMapsToUnknownError() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { throw IllegalStateException("bad json") }
            h.fill()

            h.viewModel.submit()
            runCurrent()

            assertEquals(LoginError.Unknown, h.viewModel.uiState.value.error)
        }

    @Test
    fun suspendedAccountShowsNoInlineErrorAndNoDestination() =
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

            assertNull(h.viewModel.uiState.value.error)
            assertNull(h.viewModel.destination.value)
            assertFalse(h.viewModel.uiState.value.isSubmitting)
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
    fun editingFieldsClearsTheError() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.viewModel.submit()
            runCurrent()
            assertEquals(LoginError.MissingFields, h.viewModel.uiState.value.error)

            h.viewModel.onEmailChange("a")
            assertNull(h.viewModel.uiState.value.error)

            h.viewModel.submit()
            runCurrent()
            assertEquals(LoginError.MissingFields, h.viewModel.uiState.value.error)
            h.viewModel.onPasswordChange("p")
            assertNull(h.viewModel.uiState.value.error)
        }

    @Test
    fun onDestinationConsumedClearsDestination() =
        runTest(dispatcherRule.testDispatcher) {
            val h = Harness(this)
            h.api.loginHandler = { loginData("client") }
            h.fill()
            h.viewModel.submit()
            runCurrent()
            assertEquals(LaunchDestination.Home(AppMode.Client), h.viewModel.destination.value)

            h.viewModel.onDestinationConsumed()

            assertNull(h.viewModel.destination.value)
        }
}
