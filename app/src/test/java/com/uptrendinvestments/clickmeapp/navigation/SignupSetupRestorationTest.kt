@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.SignupBasicInfoScreen
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.SignupBasicInfoViewModel
import com.uptrendinvestments.clickmeapp.screens.signup.verifyemail.SignupVerifyEmailScreen
import com.uptrendinvestments.clickmeapp.screens.signup.verifyemail.SignupVerifyEmailViewModel
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Phase 15 SC-2 (signup), CONTEXT D-09: the setup nav display, its entry decorators and the real Personal Details
 * and Verify Email screens survive Activity recreation and process death. The step and the typed text come back,
 * the verification code does not, and a late server hydrate never overwrites restored text (RESEARCH Pitfall 7).
 *
 * The Hilt `SignupSetupNavHost` is replaced by [SignupSetupNavDisplay] plus explicit `viewModel { ... }` factories
 * (the approach of `EntrySavedStateHandleTest`); the navigation state, the entry decorators and the screens are the
 * production ones. Process death is emulated by saving the registry and swapping in a fresh ViewModel store and a
 * fresh repository, so nothing survives except the saved state.
 */
@RunWith(RobolectricTestRunner::class)
class SignupSetupRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    // Eager, so the repository's app-scope fetch completes inside the call that starts it.
    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val clock = MonotonicClock { 0L }

    private var setupNavigator: SignupSetupNavigator? = null
    private lateinit var harness: SignupTestHarness
    private var viewModelOwner: ViewModelStoreOwner = FreshViewModelStoreOwner()

    @Before
    fun setUp() {
        harness = newHarness()
    }

    private fun newHarness() =
        SignupTestHarness(testScope, tmp, dispatcher).also {
            it.repository.seedFromSignup("ada@b.co", false)
            // A code is already out, with its cooldown over, so the Verify Email step shows its code field.
            it.repository.recordCodeSent(0L)
        }

    private fun nav(): SignupSetupNavigator = checkNotNull(setupNavigator)

    private fun entry(key: NavKey): NavEntry<NavKey> =
        NavEntry(key) {
            // Read at ViewModel creation, i.e. after a restore these are the fresh instances.
            val h = harness
            when (key) {
                is PersonalDetailsKey ->
                    SignupBasicInfoScreen(
                        onSaved = nav()::back,
                        onBack = nav()::back,
                        viewModel =
                            viewModel {
                                SignupBasicInfoViewModel(
                                    h.repository,
                                    h.api,
                                    createSavedStateHandle(),
                                )
                            },
                    )
                is VerifyEmailKey ->
                    SignupVerifyEmailScreen(
                        onDone = nav()::back,
                        onBack = nav()::back,
                        viewModel =
                            viewModel {
                                SignupVerifyEmailViewModel(h.repository, clock, createSavedStateHandle())
                            },
                    )
                else -> Text("stub:hub")
            }
        }

    private fun StateRestorationTester.setSetupContent() {
        setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelOwner) {
                ClickMeTheme {
                    val state = rememberNavigationState(HubKey, setOf(HubKey))
                    val setup = remember(state) { SignupSetupNavigator(Navigator(state)) }
                    setupNavigator = setup
                    SignupSetupNavDisplay(
                        navigationState = state,
                        setupNavigator = setup,
                        entryProvider = ::entry,
                    )
                }
            }
        }
    }

    /** Process death: the registry is saved, then ViewModels and the app-scoped repository are gone. */
    private fun StateRestorationTester.dieAndRestore() {
        viewModelOwner = FreshViewModelStoreOwner()
        harness = newHarness()
        emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
    }

    private fun openStep(step: SetupStep) {
        composeRule.runOnIdle { nav().openStep(step) }
        composeRule.waitForIdle()
    }

    private fun back() {
        composeRule.runOnIdle { nav().back() }
        composeRule.waitForIdle()
    }

    @Test
    fun stepAndTextRestored() {
        val tester = StateRestorationTester(composeRule)
        tester.setSetupContent()

        openStep(SetupStep.PersonalDetails)
        composeRule.onNodeWithText("First Name").performTextInput("Ada")
        openStep(SetupStep.VerifyEmail)
        composeRule.onNodeWithText("Enter 6-digit code").performTextInput("123456")
        composeRule.onNodeWithText("123456").assertExists()

        tester.dieAndRestore()

        // Same step on top, with the secret gone and nothing shown as an error or a stale value.
        composeRule.onNodeWithText("Verify Your Email Address").assertExists()
        composeRule.onNodeWithText("Enter 6-digit code").assertExists()
        composeRule.onNodeWithText("123456").assertDoesNotExist()
        composeRule.onNodeWithText("Please enter the 6-digit code").assertDoesNotExist()
        composeRule.onNodeWithText("Code must be 6 digits").assertDoesNotExist()
        // The code-sent flag restored too, so the code field is still offered instead of the Send button.
        composeRule.onNodeWithText("Send Verification Email").assertDoesNotExist()

        // Back leads to the same previous step, with the typed text.
        back()
        composeRule.onNodeWithText("Ada").assertExists()
        back()
        composeRule.onNodeWithText("stub:hub").assertExists()
    }

    @Test
    fun restoredTextBeatsHydration() {
        val tester = StateRestorationTester(composeRule)
        tester.setSetupContent()

        openStep(SetupStep.PersonalDetails)
        composeRule.onNodeWithText("First Name").performTextInput("Ada")

        tester.dieAndRestore()
        composeRule.onNodeWithText("Ada").assertExists()

        // The host's ensureHydrated() lands after the restore with a different server value.
        val h = harness
        h.api.expertProfileHandler = { h.fullProfile(firstName = "Server") }
        h.api.emailStatusHandler = { EmailStatusData(verified = false) }
        val hydrated = runBlocking { h.repository.hydrate() }
        composeRule.waitForIdle()

        assertTrue(hydrated)
        assertEquals("Server", h.repository.draft.value.firstName)
        composeRule.onNodeWithText("Ada").assertExists()
        composeRule.onNodeWithText("Server").assertDoesNotExist()
    }
}

private class FreshViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
